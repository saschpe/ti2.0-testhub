#language:de
# Befehl zum ausführen des Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smoke' -Dzeta.env=local
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:ZETA

@smoke
Funktionalität: Smoke Tests mit PoPP und VSDM2

  @TCID:ZETA_SMOKE_CLUSTER_AVAILABILITY_POPP
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  Szenariogrundriss: Availability check ZETA Komponenten (PDP / PEP / Ingress) für PoPP
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Wenn TGR sende eine leere GET Anfrage an "<Ressource>"
    Dann TGR finde die letzte Anfrage mit dem Pfad "<Pfad>"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.httpVersion" überein mit "HTTP/1.1"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und gebe die Antwortzeit vom aktuellen Nachrichtenpaar aus

  Beispiele:
    | Ressource                              | Pfad                                    | #Ressource         |
    | ${smoke.endpoints.poppZetaPdp.url}     | ${smoke.endpoints.poppZetaPdp.path}     | #PoPP ZeTA PDP     |
    | ${smoke.endpoints.poppZetaPep.url}     | ${smoke.endpoints.poppZetaPep.path}     | #PoPP ZeTA PEP     |
    | ${smoke.endpoints.poppZetaIngress.url} | ${smoke.endpoints.poppZetaIngress.path} | #PoPP ZETA Ingress |
    | ${smoke.endpoints.vsdmZetaPdp.url}     | ${smoke.endpoints.vsdmZetaPdp.path}     | #VSDM ZETA PDP     |
    | ${smoke.endpoints.vsdmZetaPep.url}     | ${smoke.endpoints.vsdmZetaPep.path}     | #VSDM ZETA PEP     |
    | ${smoke.endpoints.vsdmZetaIngress.url} | ${smoke.endpoints.vsdmZetaIngress.path} | #VSDM ZeTA Ingress |
