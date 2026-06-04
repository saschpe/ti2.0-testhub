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

import static de.gematik.ti20.popp.data.TestConstants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import de.gematik.rbellogger.data.RbelElement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.assertj.core.api.InstanceOfAssertFactories;

public class PoppTokenValidator extends BaseValidator {

  public PoppTokenValidator() {
    super();
  }

  public void validatePoppTokenforEHealthKT() {

    findRequestForPath(".*/token");
    currentResponseAtMatchesAsJsonTheFile("$.body", VALID_POPP_TOKEN_JSON_RESPONSE_FILE);
    currentResponseAtMatchesAsJsonTheFile(
        "$.body.token.content.body", VALID_POPP_TOKEN_BODY_CLAIMS_FILE);
    currentResponseAtMatchesAsJsonTheFile(
        "$.body.token.content.header", VALID_POPP_TOKEN_HEADER_CLAIMS_FILE);
  }

  public void validatePoppTokenforStandardKt() {
    findRequestForPath(".*/token");
    currentResponseAtMatchesAsJsonTheFile("$.body", VALID_POPP_TOKEN_JSON_RESPONSE_FILE);
    currentResponseAtMatchesAsJsonTheFile(
        "$.body.token.content.body", VALID_POPP_TOKEN_BODY_CLAIMS_FILE);
    currentResponseAtMatchesAsJsonTheFile(
        "$.body.token.content.header", VALID_POPP_TOKEN_HEADER_CLAIMS_FILE);

    int MAX_AGE_POPP_TOKEN_IN_SECONDS = 30;
    assertThat(
            this.rbelMessageRetriever.findElementInCurrentResponse("$.body.token.content.body.iat"))
        .as("iat element should exist")
        .isNotNull()
        .extracting(RbelElement::getRawStringContent)
        .as("raw iat should not be null")
        .isNotNull()
        .extracting(Long::parseLong)
        .extracting(
            epochSeconds ->
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
        .asInstanceOf(InstanceOfAssertFactories.ZONED_DATE_TIME)
        .as("iat must be recent enough")
        .isAfter(ZonedDateTime.now().minusSeconds(MAX_AGE_POPP_TOKEN_IN_SECONDS));

    assertThat(
            this.rbelMessageRetriever.findElementInCurrentResponse(
                "$.body.token.content.body.patientProofTime"))
        .as("patientProofTime element should exist")
        .isNotNull()
        .extracting(RbelElement::getRawStringContent)
        .as("raw patientProofTime should not be null")
        .isNotNull()
        .extracting(Long::parseLong)
        .extracting(
            epochSeconds ->
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
        .asInstanceOf(InstanceOfAssertFactories.ZONED_DATE_TIME)
        .as("patientProofTime must be recent enough")
        .isAfter(ZonedDateTime.now().minusSeconds(MAX_AGE_POPP_TOKEN_IN_SECONDS));
  }

  public void validatePoppTokenforBasicErrorResponse() {
    findRequestForPath(".*/token");
    currentResponseAtMatches("$.body.status", "ERROR");
  }

  public void validatePoppTokenforInvalidCaErrorResponse(String errorMessage) {
    findRequestForPath(".*/token");
    currentResponseAtMatches("$.body.errorMessage", errorMessage);
  }
}
