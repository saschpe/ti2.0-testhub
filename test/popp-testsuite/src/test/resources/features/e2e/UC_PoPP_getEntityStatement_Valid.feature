#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: Abfrage des EntityStatements vom PoPP-Service

  @TCID:UC_PoPP_getEntityStatement_Valid
  @STATUS:InBearbeitung
  @MODUS:Automatisch
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenariogrundriss: Erfolgreiche Abfrage des EntityStatements vom PoPP-Service

  Dieser Testfall testet den Anwendungsfall Abfrage des EntityStatements vom PoPP-Service.

    Angenommen der Endpunkt <Identifier-URL>/.well-known/openid-federation ist beim PoPP-Service verfügbar

    Wenn der Fachdienst das EntityStatement vom PoPP-Service über den Endpunkt <Identifier-URL>/.well-known/openid-federation abfragt
    Dann erhält der Fachdienst eine gültige Antwort mit einem spezifikationskonformen EntityStatement vom PoPP-Service


    Beispiele:
      | Identifier-URL           | @onlyThis |
      | popp.test.poppservice.de | @run      |
      | popp.ref.poppservice.de  | @run      |
      | popp.dev.poppservice.de  | @run      |