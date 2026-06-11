/*-
 * #%L
 * ZeTA Testsuite
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Einfache lokale Performance-Tests für ZeTA-Komponenten. Kein JMeter, kein Load Driver, kein
 * Prometheus – nur parallele HTTP-Anfragen mit Durchsatz- und Latenz-Auswertung.
 */
@Slf4j
public class LocalPerformanceSteps {

  private final List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
  private final AtomicInteger successCount = new AtomicInteger();
  private final AtomicInteger errorCount = new AtomicInteger();
  private double actualRps;

  @Wenn("parallel {int} GET-Anfragen mit {int} Threads an {string} gesendet werden")
  public void sendParallelGetRequests(int totalRequests, int threads, String urlTemplate) {
    var resolvedUrl = resolve(urlTemplate);
    latenciesMs.clear();
    successCount.set(0);
    errorCount.set(0);

    log.info(
        "Starte lokalen Performance-Test: {} Anfragen, {} Threads, URL: {}",
        totalRequests,
        threads,
        resolvedUrl);

    try (var executor = Executors.newFixedThreadPool(threads);
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {

      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(resolvedUrl))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();

      CountDownLatch latch = new CountDownLatch(totalRequests);
      long startNanos = System.nanoTime();

      for (int i = 0; i < totalRequests; i++) {
        executor.submit(
            () -> {
              try {
                long reqStart = System.nanoTime();
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                long durationMs = (System.nanoTime() - reqStart) / 1_000_000;
                latenciesMs.add(durationMs);

                // 2xx/3xx/401 zählen als Erfolg – der Server hat geantwortet.
                // 401 ist erwartetes Verhalten bei PEP-Endpunkten ohne Auth-Token.
                int status = response.statusCode();
                if (status < 500 && status != 404) {
                  successCount.incrementAndGet();
                } else {
                  errorCount.incrementAndGet();
                  log.warn("HTTP {} für {}", status, resolvedUrl);
                }
              } catch (Exception e) {
                errorCount.incrementAndGet();
                log.warn("Fehler bei Anfrage an {}: {}", resolvedUrl, e.getMessage());
              } finally {
                latch.countDown();
              }
            });
      }

      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Performance-Test unterbrochen", e);
      }

      long totalDurationMs = (System.nanoTime() - startNanos) / 1_000_000;
      actualRps = totalRequests * 1000.0 / totalDurationMs;

      logResults(resolvedUrl, totalRequests, threads, totalDurationMs);
    }
  }

  @Dann("war die Fehlerrate unter {double} Prozent")
  public void assertErrorRate(double maxErrorPercent) {
    int total = successCount.get() + errorCount.get();
    double errorRate = total > 0 ? errorCount.get() * 100.0 / total : 0;
    log.info(
        "Fehlerrate: {}/{} = {}% (max erlaubt: {}%)",
        errorCount.get(), total, String.format("%.2f", errorRate), maxErrorPercent);
    assertThat(errorRate)
        .as("Fehlerrate muss unter %s%% liegen", maxErrorPercent)
        .isLessThanOrEqualTo(maxErrorPercent);
  }

  @Und("war der Durchsatz mindestens {double} Anfragen pro Sekunde")
  public void assertThroughput(double minRps) {
    log.info("Durchsatz: {} req/s (min erwartet: {})", String.format("%.1f", actualRps), minRps);
    assertThat(actualRps)
        .as("Durchsatz muss mindestens %s req/s betragen", minRps)
        .isGreaterThanOrEqualTo(minRps);
  }

  @Und("war die p95-Latenz unter {long} ms")
  public void assertP95Latency(long maxMs) {
    var sorted = latenciesMs.stream().sorted().toList();
    if (sorted.isEmpty()) {
      throw new AssertionError("Keine Messwerte vorhanden");
    }
    int p95Index = (int) Math.ceil(sorted.size() * 0.95) - 1;
    long p95 = sorted.get(Math.max(0, p95Index));
    log.info("p95-Latenz: {} ms (max erlaubt: {} ms)", p95, maxMs);
    assertThat(p95).as("p95-Latenz muss unter %s ms liegen", maxMs).isLessThanOrEqualTo(maxMs);
  }

  private void logResults(String url, int totalRequests, int threads, long totalDurationMs) {
    var sorted = latenciesMs.stream().sorted().toList();
    if (sorted.isEmpty()) {
      log.warn("Keine Messwerte – alle Anfragen fehlgeschlagen");
      return;
    }
    LongSummaryStatistics stats = sorted.stream().mapToLong(Long::longValue).summaryStatistics();

    int p50Idx = (int) Math.ceil(sorted.size() * 0.50) - 1;
    int p90Idx = (int) Math.ceil(sorted.size() * 0.90) - 1;
    int p95Idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
    int p99Idx = (int) Math.ceil(sorted.size() * 0.99) - 1;

    log.info("═══════════════════════════════════════════════════════════");
    log.info("  Performance-Ergebnis für: {}", url);
    log.info("───────────────────────────────────────────────────────────");
    log.info(
        "  Anfragen:   {} ({} OK, {} Fehler)", totalRequests, successCount.get(), errorCount.get());
    log.info("  Threads:    {}", threads);
    log.info("  Dauer:      {} ms", totalDurationMs);
    log.info("  Durchsatz:  {} req/s", String.format("%.1f", actualRps));
    log.info("───────────────────────────────────────────────────────────");
    log.info("  Latenz avg: {} ms", String.format("%.1f", stats.getAverage()));
    log.info("  Latenz min: {} ms", stats.getMin());
    log.info("  Latenz p50: {} ms", sorted.get(Math.max(0, p50Idx)));
    log.info("  Latenz p90: {} ms", sorted.get(Math.max(0, p90Idx)));
    log.info("  Latenz p95: {} ms", sorted.get(Math.max(0, p95Idx)));
    log.info("  Latenz p99: {} ms", sorted.get(Math.max(0, p99Idx)));
    log.info("  Latenz max: {} ms", stats.getMax());
    log.info("═══════════════════════════════════════════════════════════");
  }

  private String resolve(String value) {
    return TigerGlobalConfiguration.resolvePlaceholders(value);
  }
}
