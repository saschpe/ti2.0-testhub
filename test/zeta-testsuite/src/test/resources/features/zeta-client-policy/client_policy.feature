#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_ablehnungen and not @Ignore'
@PRODUKT:ZETA

Funktionalität: Client-Registrierungs-Policy und OPA-Integration (Policy-Ablehnungen)

  Grundlage:
    Gegeben sei TGR sende eine leere GET Anfrage an "${zeta.paths.client.reset}"
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # ===========================================================================
  # Policy-Ablehnungen (OPA) - diese Tests benötigen spezielle Netzwerk-Topologie
  # ===========================================================================

  @Ignore @client_registrierung @policy_ablehnungen
  Szenariogrundriss: Client-Registrierung wird wegen Client Policy abgelehnt und begründet
    # STATUS: @Ignore – zwei Voraussetzungen fehlen noch:
    # 1) OPA-Requests müssen über den test-TigerProxy geroutet werden (Docker-Topology)
    # 2) Die OPA-Policy muss ablehnen (authz.rego nicht allow=true)

    # OPA Request manipulieren - ungültige Werte setzen
    Und TGR setze lokale Variable "opaCondition" auf "isRequest && request.path =~ '.*${zeta.paths.opa.decisionPath}'"
    Dann Setze im TigerProxy für die Nachricht "${opaCondition}" die Manipulation auf Feld "<OpaInputField>" und Wert "<NeuerWert>" und 1 Ausführungen

    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.client.vsdRequest}"

    # OPA Decision prüfen - sollte allow=false liefern
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.opa.decisionPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.result.allow" überein mit "false"

    # Registrierungs-/Token-Request muss mit 403 abgelehnt werden
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.guard.tokenEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "403"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "body"
    Und validiere "${body}" gegen Schema "schemas/v_1_0/zeta-error.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.error_description" überein mit ".*<ErwarteterHinweis>.*"

    Beispiele: Ungültige Policy-Werte
      | OpaInputField                                         | NeuerWert                    | ErwarteterHinweis                                               |
      | $.body.input.user_info.professionOID                  | 1.2.276.0.76.4.999           | ${testdata.policy_rejection.invalid_profession_oid_error_hint}  |
      | $.body.input.client_assertion.posture.product_id      | unknown_product              | ${testdata.policy_rejection.invalid_product_id_error_hint}      |
      | $.body.input.client_assertion.posture.product_version | 99.99.99                     | ${testdata.policy_rejection.invalid_product_version_error_hint} |
      | $.body.input.authorization_request.scopes.0           | invalid_scope_xyz            | ${testdata.policy_rejection.invalid_scope_error_hint}           |
      | $.body.input.authorization_request.aud.0              | https://evil.example.com/api | ${testdata.policy_rejection.invalid_audience_error_hint}        |

