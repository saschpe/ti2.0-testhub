#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@asl'
#
# VORAUSSETZUNG: TestHub im Profil "full":
#   docker compose -f doc/docker/compose-local.yaml --profile full up -d
@PRODUKT:ZETA

Funktionalität: ASL-Handshake am ZETA-PEP – Positiv- und Negativfall

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "vsdmClientUrl" auf "http://127.0.0.1:6100"
    Und TGR lösche alle default headers


  @asl_handshake_success @pep @asl @positiv
  Szenariogrundriss: ASL-Handshake gelingt, wenn der VSDM-Client einen VSD-Read durchführt
    # E2E über den VSDM-Client (zeta-sdk-jvm 0.5.1 + asl-jvm). Das Well-Known des
    # VSDM-PEP fordert "zeta_asl_use": "required", das SDK initiiert daraufhin den
    # ASL-Handshake (POST /ASL → POST /ASL/<sessionId>).
    #
    # Inspektion der /ASL-Frames im Tiger-Trace ist nicht möglich, da der Traffic
    # per HTTPS zum vsdm-zeta-ingress läuft. Eine 200-Antwort des VSDM-Clients
    # beweist daher implizit den erfolgreichen ASL-Handshake.

    # VSDM-Client triggern – SDK führt SMC-B-Auth, ASL-Handshake und VSD-Read aus.
    # If-None-Match=0 verhindert ein 428 vom VSDM-Server (Precondition Required).
    Und TGR setze den default header "If-None-Match" auf den Wert "0"
    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=<Egk-Slot>&smcBSlotId=<Smcb-Slot>&isFhirXml=false"

    Dann TGR finde die letzte Anfrage mit dem Pfad "/client/vsdm/vsd"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    Beispiele:
      | Smcb-Slot | Egk-Slot |
      | 1         | 2        |






