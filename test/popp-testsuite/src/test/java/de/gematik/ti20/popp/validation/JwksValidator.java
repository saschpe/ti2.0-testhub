/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp.validation;

import static de.gematik.ti20.popp.data.TestConstants.POPP_SERVICE_BASE_URL_RU;
import static de.gematik.ti20.popp.data.TestConstants.VALDID_JWKS_FILE;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;

public class JwksValidator extends BaseValidator {
  public JwksValidator() {
    super();
  }

  public void validateSignedJwks() {
    findRequestForPath("/.well-known/signed-jwks");
    currentResponseAtMatchesAsJsonTheFile("$.body.body", VALDID_JWKS_FILE);
    currentResponseAtMatches("$..x5c.0.content.issuer.CN", "GEM.KOMP-CA.*");
    currentResponseAtMatches("$.body.body.iss", POPP_SERVICE_BASE_URL_RU.getValueOrDefault());
    currentResponseAtMatches("$.body.body.sub", POPP_SERVICE_BASE_URL_RU.getValueOrDefault());
    currentResponseAtMatches("$.body.signature.isValid", "true");
    currentResponseAtMatches(
        "$.body.body.keys.0.kid",
        TigerGlobalConfiguration.resolvePlaceholders("${tiger.kidTokenKey}"));
  }
}
