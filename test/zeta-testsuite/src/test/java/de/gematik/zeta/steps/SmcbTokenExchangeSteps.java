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

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.When;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber step that sends a full SMC-B Token-Exchange request through the Tiger proxy.
 *
 * <p>This creates:
 *
 * <ol>
 *   <li>A <b>subject_token</b> signed with the SMC-B brainpoolP256r1 key (smcb_private.p12)
 *   <li>A <b>client_assertion</b> signed with ES256/P-256 (zetakeystore.p12)
 * </ol>
 *
 * and POSTs them as a form-urlencoded token-exchange to the given URL through the Tiger proxy.
 */
@Slf4j
public class SmcbTokenExchangeSteps {

  // SMC-B keystore (brainpoolP256r1)
  private static final String SMCB_KEYSTORE_PATH =
      "doc/docker/backend/zeta/smcb-private/smcb_private.p12";
  private static final String SMCB_KEYSTORE_PASSWORD = "00";
  private static final String SMCB_KEY_ALIAS = "alias";

  // Client assertion keystore (P-256)
  private static final String CLIENT_KEYSTORE_CLASSPATH = "zetakeystore.p12";
  private static final String CLIENT_KEYSTORE_PASSWORD = "testpassword";
  private static final String CLIENT_KEY_ALIAS = "zetamock";
  private static final String CLIENT_KEY_PASSWORD = "testpassword";

  static final String CLIENT_ID = "f00561bc-a975-47aa-b87e-5d0f97265e10";

  @Wenn("sende SMC-B Token-Exchange-Request an {string} über Tiger-Proxy {string}")
  @When("send SMC-B token exchange request to {string} via Tiger proxy {string}")
  public void sendSmcbTokenExchangeRequest(String targetUrl, String proxyUrl) {
    String resolvedTarget = TigerGlobalConfiguration.resolvePlaceholders(targetUrl);
    String resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUrl);

    URI proxyUri = URI.create(resolvedProxy);

    try {
      // 0. Fetch a nonce from the ZETA Guard Keycloak
      String nonceUrl =
          resolvedTarget.replaceAll("/protocol/openid-connect/token$", "/zeta-guard-nonce");
      RestTemplate nonceRt = new RestTemplate();
      String nonceBody = nonceRt.getForEntity(URI.create(nonceUrl), String.class).getBody();
      if (nonceBody == null) {
        throw new IllegalStateException("Nonce endpoint returned null body: " + nonceUrl);
      }
      String nonce = nonceBody.trim();
      log.info("Fetched ZETA nonce: {}", nonce);

      // 1. Create subject_token (signed with SMC-B brainpoolP256r1)
      String subjectToken = createSmcbSubjectToken(nonce);

      // 2. Create client_assertion (signed with P-256)
      String clientAssertion = createClientAssertion(resolvedTarget, nonce);

      // 2b. Create DPoP proof JWT (required by Keycloak when dpop.bound.access.tokens=true)
      String dpopProof = createDpopProof(resolvedTarget);

      // 3. Build form body
      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
      form.add("subject_token", subjectToken);
      form.add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");
      form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
      form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
      form.add("client_assertion", clientAssertion);
      form.add("client_id", CLIENT_ID);

      // 4. Send through Tiger proxy
      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setProxy(
          new Proxy(
              Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));

      RestTemplate rt = new RestTemplate(factory);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.set("DPoP", dpopProof);
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

      log.info("Sending SMC-B Token-Exchange to {} via proxy {}", resolvedTarget, resolvedProxy);
      rt.postForEntity(resolvedTarget, request, String.class);

    } catch (Exception e) {
      throw new AssertionError("Failed to send SMC-B Token-Exchange request: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a subject_token JWT signed with the SMC-B brainpoolP256r1 key. The x5c header contains
   * the SMC-B certificate chain.
   */
  private String createSmcbSubjectToken(String nonce) throws Exception {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }

    // Keycloak expects the issuer URL as audience, not the local HTTP URL
    String smcbAudience = "https://popp-zeta-ingress/auth/";

    // Load SMC-B keystore from project root
    String rootFolder = TigerGlobalConfiguration.resolvePlaceholders("${tiger.rootFolder|.}");
    String keystorePath = rootFolder + "/" + SMCB_KEYSTORE_PATH;

    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (InputStream is = new FileInputStream(keystorePath)) {
      ks.load(is, SMCB_KEYSTORE_PASSWORD.toCharArray());
    }

    PrivateKey privateKey =
        (PrivateKey) ks.getKey(SMCB_KEY_ALIAS, SMCB_KEYSTORE_PASSWORD.toCharArray());
    Certificate[] certChain = ks.getCertificateChain(SMCB_KEY_ALIAS);
    X509Certificate smcbCert = (X509Certificate) certChain[0];

    // Extract TelematikID from certificate for sub claim
    String telematikId = extractTelematikIdFromCert(smcbCert);
    String professionOid = extractProfessionOidFromCert(smcbCert);

    Instant now = Instant.now();

    // Build header JSON manually (Nimbus doesn't support brainpool natively)
    String headerJson =
        String.format(
            "{\"alg\":\"ES256\",\"typ\":\"JWT\",\"x5c\":[\"%s\"]}",
            java.util.Base64.getEncoder().encodeToString(smcbCert.getEncoded()));
    String headerB64 =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

    // Build payload (nonce and professionOid are required by schema and PDP-Mock)
    String payloadJson =
        String.format(
            "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"%s\",\"typ\":\"Bearer\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\",\"nonce\":\"%s\",\"professionOid\":\"%s\"}",
            CLIENT_ID,
            telematikId,
            smcbAudience,
            now.getEpochSecond(),
            now.plusSeconds(300).getEpochSecond(),
            UUID.randomUUID(),
            nonce,
            professionOid);
    String payloadB64 =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

    // Sign with brainpoolP256r1 using BouncyCastle
    String signingInput = headerB64 + "." + payloadB64;
    Signature sig = Signature.getInstance("SHA256withECDSA", "BC");
    sig.initSign(privateKey);
    sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
    byte[] derSignature = sig.sign();

    // Convert DER signature to raw R||S format (as required by JWS)
    byte[] rawSignature = convertDerToRaw(derSignature);
    String signatureB64 =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature);

    String jwt = signingInput + "." + signatureB64;
    log.info("Created SMC-B subject_token (brainpoolP256r1), sub={}", telematikId);
    return jwt;
  }

  /**
   * Creates a client_assertion JWT signed with ES256 (P-256) from zetakeystore.p12. Includes JWK in
   * header and client_statement in payload.
   */
  private String createClientAssertion(String tokenEndpoint, String nonce) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream(CLIENT_KEYSTORE_CLASSPATH)) {
      if (is == null) {
        throw new IllegalStateException(
            "Keystore not found on classpath: " + CLIENT_KEYSTORE_CLASSPATH);
      }
      ks.load(is, CLIENT_KEYSTORE_PASSWORD.toCharArray());
    }

    ECPrivateKey privateKey =
        (ECPrivateKey) ks.getKey(CLIENT_KEY_ALIAS, CLIENT_KEY_PASSWORD.toCharArray());
    ECPublicKey publicKey = (ECPublicKey) ks.getCertificate(CLIENT_KEY_ALIAS).getPublicKey();

    // Build JWK for header
    ECKey jwk =
        new ECKey.Builder(Curve.P_256, publicKey)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .build();

    Instant now = Instant.now();

    // Client statement (attestation data)
    // attestation_challenge = Base64Url(SHA-256(jwkThumbprint_bytes || nonce_bytes))
    byte[] jwkThumbprintBytes = jwk.computeThumbprint().decode();
    byte[] nonceBytes = java.util.Base64.getUrlDecoder().decode(nonce);
    java.security.MessageDigest challengeDigest =
        java.security.MessageDigest.getInstance("SHA-256");
    challengeDigest.update(jwkThumbprintBytes);
    challengeDigest.update(nonceBytes);
    String attestationChallenge =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(challengeDigest.digest());

    Map<String, Object> posture = new java.util.LinkedHashMap<>();
    posture.put("attestation_challenge", attestationChallenge);
    posture.put(
        "public_key",
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getEncoded()));

    // product_id, product_version and platform_product_id are required by SoftwarePosture
    posture.put("product_id", "de.gematik.test.zeta");
    posture.put("product_version", "1.0.0");
    posture.put("os", "linux");
    posture.put("os_version", "6.1");
    posture.put("arch", "x86_64");
    Map<String, Object> postureProductId = new java.util.LinkedHashMap<>();
    postureProductId.put("platform", "linux");
    postureProductId.put("packaging_type", "jar");
    postureProductId.put("application_id", "de.gematik.test.zeta");
    posture.put("platform_product_id", postureProductId);

    // posture_type is an external type id (Jackson EXTERNAL_PROPERTY), must be sibling of posture
    Map<String, Object> clientStatement = new java.util.LinkedHashMap<>();
    clientStatement.put("platform", "linux");
    clientStatement.put("sub", CLIENT_ID);
    clientStatement.put("attestation_timestamp", now.minusSeconds(10).getEpochSecond());
    clientStatement.put("posture_type", "software");
    clientStatement.put("posture", posture);

    // Keycloak expects the issuer URL as audience, not the local HTTP URL
    String keycloakAudience =
        "https://popp-zeta-ingress/auth/realms/zeta-guard/protocol/openid-connect/token";

    // Build urn:telematik:client-self-assessment (ClientInstanceData structure with name etc.)
    Map<String, Object> platformProductId = new java.util.LinkedHashMap<>();
    platformProductId.put("platform", "linux");
    platformProductId.put("packaging_type", "jar");
    platformProductId.put("application_id", "de.gematik.test.zeta");

    Map<String, Object> selfAssessment = new java.util.LinkedHashMap<>();
    selfAssessment.put("name", "ZeTA Test Client");
    selfAssessment.put("client_id", CLIENT_ID);
    selfAssessment.put("manufacturer_id", "gematik");
    selfAssessment.put("manufacturer_name", "gematik GmbH");
    selfAssessment.put("owner_mail", "test@gematik.de");
    selfAssessment.put("registration_timestamp", System.currentTimeMillis() / 1000);
    selfAssessment.put("platform_product_id", platformProductId);

    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(CLIENT_ID)
            .subject(CLIENT_ID)
            .audience(keycloakAudience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .jwtID(UUID.randomUUID().toString())
            .claim("client_statement", clientStatement)
            .claim("urn:telematik:client-self-assessment", selfAssessment)
            .build();

    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType.JWT)
            .jwk(jwk.toPublicJWK())
            .build();

    SignedJWT signedJwt = new SignedJWT(header, claims);
    signedJwt.sign(new ECDSASigner(privateKey));

    log.info("Created client_assertion (ES256/P-256), iss={}, aud={}", CLIENT_ID, tokenEndpoint);
    return signedJwt.serialize();
  }

  /**
   * Creates a DPoP proof JWT (RFC 9449) signed with ES256. Uses an ephemeral P-256 key pair loaded
   * from the same keystore as the client assertion.
   */
  private String createDpopProof(String tokenEndpoint) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream(CLIENT_KEYSTORE_CLASSPATH)) {
      ks.load(is, CLIENT_KEYSTORE_PASSWORD.toCharArray());
    }
    ECPrivateKey privateKey =
        (ECPrivateKey) ks.getKey(CLIENT_KEY_ALIAS, CLIENT_KEY_PASSWORD.toCharArray());
    ECPublicKey publicKey = (ECPublicKey) ks.getCertificate(CLIENT_KEY_ALIAS).getPublicKey();

    ECKey dpopJwk = new ECKey.Builder(Curve.P_256, publicKey).build();

    JWTClaimsSet dpopClaims =
        new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .claim("htm", "POST")
            .claim("htu", tokenEndpoint)
            .issueTime(Date.from(Instant.now()))
            .build();

    JWSHeader dpopHeader =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(new JOSEObjectType("dpop+jwt"))
            .jwk(dpopJwk.toPublicJWK())
            .build();

    SignedJWT dpopJwt = new SignedJWT(dpopHeader, dpopClaims);
    dpopJwt.sign(new ECDSASigner(privateKey));
    return dpopJwt.serialize();
  }

  /** Extracts the TelematikID from the Admission extension of an SMC-B certificate. */
  private String extractTelematikIdFromCert(X509Certificate cert) {
    try {
      byte[] extValue = cert.getExtensionValue("1.3.36.8.3.3"); // Admission OID
      if (extValue == null) {
        throw new IllegalStateException("No Admission extension in SMC-B certificate");
      }
      try (var asn1In = new org.bouncycastle.asn1.ASN1InputStream(extValue)) {
        var octetString = (org.bouncycastle.asn1.ASN1OctetString) asn1In.readObject();
        try (var asn1In2 = new org.bouncycastle.asn1.ASN1InputStream(octetString.getOctets())) {
          var seq = (org.bouncycastle.asn1.ASN1Sequence) asn1In2.readObject();
          // Navigate to the registration number (TelematikID)
          while (seq.size() == 1) {
            seq = (org.bouncycastle.asn1.ASN1Sequence) seq.getObjectAt(0);
          }
          if (seq.size() >= 3) {
            var regNum = seq.getObjectAt(2);
            while (regNum instanceof org.bouncycastle.asn1.DLSequence dlseq) {
              regNum = dlseq.getObjectAt(0);
            }
            return regNum.toASN1Primitive().toString();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Could not extract TelematikID from certificate: {}", e.getMessage());
    }
    // Fallback
    return "3-2-TestTelematikId";
  }

  /** Extracts the profession OID from the Admission extension of an SMC-B certificate. */
  private String extractProfessionOidFromCert(X509Certificate cert) {
    try {
      byte[] extValue = cert.getExtensionValue("1.3.36.8.3.3"); // Admission OID
      if (extValue == null) {
        throw new IllegalStateException("No Admission extension in SMC-B certificate");
      }
      try (var asn1In = new org.bouncycastle.asn1.ASN1InputStream(extValue)) {
        var octetString = (org.bouncycastle.asn1.ASN1OctetString) asn1In.readObject();
        try (var asn1In2 = new org.bouncycastle.asn1.ASN1InputStream(octetString.getOctets())) {
          var seq = (org.bouncycastle.asn1.ASN1Sequence) asn1In2.readObject();
          // Navigate admission data (unwrap single-element sequences)
          while (seq.size() == 1) {
            seq = (org.bouncycastle.asn1.ASN1Sequence) seq.getObjectAt(0);
          }
          // Index 1 = professionOid, Index 2 = telematikId (ProfessionInfo structure)
          if (seq.size() >= 3) {
            var profOid = seq.getObjectAt(1);
            while (profOid instanceof org.bouncycastle.asn1.DLSequence dlseq) {
              profOid = dlseq.getObjectAt(0);
            }
            return profOid.toASN1Primitive().toString();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Could not extract profession OID from certificate: {}", e.getMessage());
    }
    return "1.2.276.0.76.4.50"; // Fallback: Betriebsstätte Arzt
  }

  private static final int EC_COMPONENT_LENGTH = 32;

  /**
   * Converts a DER-encoded ECDSA signature to raw R||S format. Each component is padded/trimmed to
   * {@value #EC_COMPONENT_LENGTH} bytes for P-256/brainpoolP256r1.
   */
  private byte[] convertDerToRaw(byte[] derSignature) throws Exception {
    try (var asn1In = new org.bouncycastle.asn1.ASN1InputStream(derSignature)) {
      var seq = (org.bouncycastle.asn1.ASN1Sequence) asn1In.readObject();
      var r = ((org.bouncycastle.asn1.ASN1Integer) seq.getObjectAt(0)).getValue();
      var s = ((org.bouncycastle.asn1.ASN1Integer) seq.getObjectAt(1)).getValue();

      byte[] rBytes = toFixedLength(r.toByteArray());
      byte[] sBytes = toFixedLength(s.toByteArray());

      byte[] raw = new byte[EC_COMPONENT_LENGTH * 2];
      System.arraycopy(rBytes, 0, raw, 0, EC_COMPONENT_LENGTH);
      System.arraycopy(sBytes, 0, raw, EC_COMPONENT_LENGTH, EC_COMPONENT_LENGTH);
      return raw;
    }
  }

  private byte[] toFixedLength(byte[] bytes) {
    if (bytes.length == EC_COMPONENT_LENGTH) {
      return bytes;
    } else if (bytes.length > EC_COMPONENT_LENGTH) {
      // Strip leading zero padding
      byte[] trimmed = new byte[EC_COMPONENT_LENGTH];
      System.arraycopy(bytes, bytes.length - EC_COMPONENT_LENGTH, trimmed, 0, EC_COMPONENT_LENGTH);
      return trimmed;
    } else {
      // Pad with leading zeros
      byte[] padded = new byte[EC_COMPONENT_LENGTH];
      System.arraycopy(bytes, 0, padded, EC_COMPONENT_LENGTH - bytes.length, bytes.length);
      return padded;
    }
  }

  @Dann("schreibe Daten aus dem SMC-B Zertifikat {string} in die Variable {string}")
  public void extractSmcbCertificateData(String certBase64Var, String variableName) {
    String resolvedCert = TigerGlobalConfiguration.resolvePlaceholders(certBase64Var);
    try {
      byte[] certBytes = java.util.Base64.getDecoder().decode(resolvedCert);
      java.security.cert.CertificateFactory cf =
          java.security.cert.CertificateFactory.getInstance("X.509");
      java.security.cert.X509Certificate cert =
          (java.security.cert.X509Certificate)
              cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));

      // Extract TelematikID and professionOID from Admission extension (OID 1.3.36.8.3.3)
      String telematikId = extractTelematikIdFromCert(cert);
      String professionId = extractProfessionOidFromCert(cert);

      TigerGlobalConfiguration.putValue(variableName + ".telematikId", telematikId);
      TigerGlobalConfiguration.putValue(variableName + ".professionId", professionId);
      log.info("Extracted SMC-B data: telematikId={}, professionId={}", telematikId, professionId);
    } catch (Exception e) {
      throw new AssertionError("Failed to extract SMC-B certificate data: " + e.getMessage(), e);
    }
  }
}
