#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
@PRODUKT:ZETA

Funktionalität: Client-Registrierung und ZETA Service Discovery (DB-Integrationsprüfung)

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "pepBaseUrl" auf "${zeta.paths.vsdm.pep.baseUrl}"
    Und TGR setze lokale Variable "pdpBaseUrl" auf "${zeta.paths.vsdm.pdp.baseUrl}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # ===========================================================================
  # Service Discovery: Protected Resource Metadata (RFC 9728) via echten PEP
  # ===========================================================================

  @client_registrierung @service_discovery
  Szenario: Service Discovery - Protected Resource Metadata vom echten PEP abrufen (RFC 9728)
    # Testet den ersten Schritt der initialen Client-Registrierung:
    # Der ZETA Client ruft den well-known Endpunkt des echten PEP (ngx_pep) auf
    # und erhält ein RFC-9728 Protected Resource Metadata Dokument.

    Wenn TGR sende eine leere GET Anfrage an "${pepBaseUrl}/.well-known/oauth-protected-resource"

    # Response muss 200 OK sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/.well-known/oauth-protected-resource"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # Response-Body muss ein gültiges RFC-9728 Dokument sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body"

    # Felder der aktuellen RFC 9728 Response prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.resource"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_servers"

  # ===========================================================================
  # Service Discovery: OAuth Authorization Server Metadata via PEP → Keycloak
  # ===========================================================================

  @client_registrierung @service_discovery
  Szenario: well-known OAuth Authorization Server liefert valides Dokument (200)
    # PEP proxiert diesen Endpunkt zu Keycloak: /auth/realms/zeta-guard/.well-known/zeta-guard-well-known
    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.vsdm.ingress.baseUrl}${zeta.paths.vsdm.wellKnownOAuthServerPath}"
    Dann TGR finde die letzte Anfrage mit dem Pfad ".*${zeta.paths.vsdm.wellKnownOAuthServerPath}$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "AS_WELL_KNOWN"
    # Schema-Validierung: issuer, token_endpoint, jwks_uri, ... müssen vorhanden sein
    Und validiere "${AS_WELL_KNOWN}" gegen Schema "schemas/v_1_0/as-well-known.yaml"
    # jwks_uri muss ein gültiger HTTP(S)-URI sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks_uri"
    Und TGR speichere Wert des Knotens "$.body.jwks_uri" der aktuellen Antwort in der Variable "jwksUri"
    Und TGR prüfe Variable "jwksUri" stimmt überein mit "^https?://[^/]+/.*$"

  # ===========================================================================
  # Dynamic Client Registration gegen echten Keycloak (DB-Integrationsprüfung)
  # ===========================================================================

  @client_registrierung @dcr
  Szenario: Dynamic Client Registration - Client erfolgreich registrieren (POST /register)
    # Testet die Dynamic Client Registration (RFC 7591):
    # Der ZETA Client sendet einen DCR-Request an den PDP Authorization Server.
    # Der Server erzeugt eine client_id und legt einen Client Placeholder an.

    # DCR-Request an den PDP (Keycloak) senden
    Wenn TGR sende eine POST Anfrage an "${pdpBaseUrl}/auth/realms/zeta-guard/clients-registrations/openid-connect" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      !{file('test/zeta-testsuite/src/test/resources/mocks/register-request.json')}
      """

    # Response muss 201 Created sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/clients-registrations/openid-connect"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "201"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id_issued_at"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.token_endpoint_auth_method" überein mit "private_key_jwt"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.redirect_uris"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_client_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_access_token"



    # --- Anfrage-Validierung ---
    Und TGR prüfe aktueller Request stimmt im Knoten "$.method" überein mit "POST"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.Content-Type" überein mit "application/json"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_name" überein mit "sdk-client"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.token_endpoint_auth_method"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.jwks"

  # Hinweis: Token-Exchange- (Auth/DPoP/PoP) und Policy-Ablehnungs-Szenarien
  # wurden in separate Feature-Dateien ausgelagert, um Registrierung (DCR)
  # von Laufzeit-Authentifizierungs-Tests zu trennen.
  #
  # Neue Dateien:
  # - test/zeta-testsuite/src/test/resources/features/zeta-client-authentifizierung/client_authentifizierung.feature
  # - test/zeta-testsuite/src/test/resources/features/zeta-client-policy/client_policy.feature
  #
  # Die originalen Szenarien wurden dort eins zu eins übernommen (ggf. ohne @Ignore),
  # damit CI/Jobs verschiedene Gruppen (mocked vs. integration) gezielt ausführen können.

