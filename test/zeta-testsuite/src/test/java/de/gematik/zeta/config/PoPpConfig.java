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
package de.gematik.zeta.config;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 * Zentraler, typisierter Zugriff auf alle PoPP-/PDP-Endpunkte der Testsuite.
 *
 * <p>Die Klasse hält selbst KEINE URLs vor. Sie liest ausschließlich die in {@code
 * tiger/defaults.yaml} definierten Schlüssel ({@code zeta.server.*}), die dort wiederum aus dem
 * zentralen Umgebungs-Schalter {@code zeta.env} abgeleitet werden. Welche Umgebung aktiv ist (z.B.
 * {@code local} oder {@code rudev}), wird damit an genau einer Stelle gesteuert und gilt für alle
 * Konsumenten gleichermaßen.
 *
 * <p>Umschalten der Umgebung:
 *
 * <pre>
 *   -Dzeta.env=local        (System-Property)
 *   export ZETA_ENV=local   (Umgebungsvariable)
 * </pre>
 *
 * <p>Alle Getter lösen die Werte <b>lazy</b> auf, damit der Schalter auch dann greift, wenn die
 * Tiger-Konfiguration erst nach dem Laden dieser Klasse initialisiert wird.
 */
@Slf4j
public final class PoPpConfig {

  private PoPpConfig() {
    throw new UnsupportedOperationException("PoPpConfig ist eine Utility-Klasse");
  }

  // ======= PDP / Keycloak =======

  /** Token-Endpoint für den Token Exchange. */
  public static String tokenUrl() {
    return resolve("zeta.server.pdp.tokenUrl");
  }

  /** Erwarteter Issuer des PDP (Realm-URL). */
  public static String issuer() {
    return resolve("zeta.server.pdp.issuer");
  }

  /** Realm-Basis-URL des PDP (Grundlage für DCR-, JWKS- und Nonce-Endpunkt). */
  public static String realmUrl() {
    return resolve("zeta.server.pdp.realmUrl");
  }

  /** Dynamic-Client-Registration-Endpoint. */
  public static String dcrUrl() {
    return resolve("zeta.server.pdp.dcrUrl");
  }

  /** JWKS-Endpoint des PDP. */
  public static String jwksUrl() {
    return resolve("zeta.server.pdp.jwksUrl");
  }

  /** ZETA-Guard-Nonce-Endpoint. */
  public static String nonceUrl() {
    return resolve("zeta.server.pdp.nonceUrl");
  }

  /** Audience für das SMC-B-Subject-Token. */
  public static String smcbAudience() {
    return resolve("zeta.server.pdp.smcbAudience");
  }

  /**
   * Audience für das {@code client_assertion}-JWT (Client-Authentifizierung). Muss den von Keycloak
   * veröffentlichten Frontend-Token-Endpoint (abgeleitet aus {@code KC_HOSTNAME}) enthalten, nicht
   * die tatsächliche Request-URL — sonst lehnt Keycloak mit {@code invalid_client} / "Invalid token
   * audience" ab.
   */
  public static String clientAssertionAudience() {
    return resolve("zeta.server.pdp.clientAssertionAud");
  }

  // ======= PoPP-Client / PEP =======

  /** Basis-URL des lokalen PoPP-Clients. */
  public static String poppClientUrl() {
    return resolve("zeta.server.poppClient.url");
  }

  /** Token-Endpoint des lokalen PoPP-Clients. */
  public static String poppClientTokenEndpoint() {
    return poppClientUrl() + "/token";
  }

  /** Basis-URL des PEP. */
  public static String pepUrl() {
    return resolve("zeta.server.pep.url");
  }

  // ======= Helper =======

  /**
   * Liest einen Tiger-Konfigurationswert und löst dabei verschachtelte Platzhalter auf. Tiger
   * berücksichtigt automatisch System-Properties und Umgebungsvariablen, sodass hier kein eigener
   * Fallback nötig ist.
   */
  private static String resolve(String key) {
    String value = TigerGlobalConfiguration.readString(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "PoPpConfig: Konfigurationsschlüssel '"
              + key
              + "' ist nicht gesetzt. Prüfe tiger/defaults.yaml und zeta.env.");
    }
    return value;
  }

  /** Loggt die aktuell aufgelöste Konfiguration (Debug-Hilfe). */
  public static void logConfiguration() {
    log.info("========== PoPpConfig (zeta.env={}) ==========", resolve("zeta.env"));
    log.info("PDP Realm URL : {}", realmUrl());
    log.info("Token Endpoint: {}", tokenUrl());
    log.info("Issuer        : {}", issuer());
    log.info("DCR Endpoint  : {}", dcrUrl());
    log.info("JWKS Endpoint : {}", jwksUrl());
    log.info("Nonce Endpoint: {}", nonceUrl());
    log.info("SMC-B Audience: {}", smcbAudience());
    log.info("PoPP Client   : {}", poppClientUrl());
    log.info("PEP URL       : {}", pepUrl());
    log.info("==============================================");
  }
}
