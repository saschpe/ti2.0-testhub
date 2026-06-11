#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:VSDM_2_FD
@AFO-ID:A_27012-03
@TYPE:ERROR
Funktionalität: Fehlerbehandlung VSDM 2.0

  @TCID:UC_VSDM2_RVSD_ERROR_CODE_79015
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTFALL:Negativ
  @TESTSTUFE:3
  @PRIO:2
  @DESCRIPTION
  Szenariogrundriss: Fehlercode 79015 - Unbekannte FHIR Profile Version

  Dieser Testfall beschreibt ein Fehlerszenario, das durch die Verwendung einer unbekannten FHIR-Profilversion bei der
  Abfrage der VSD verursacht wird. Aktuell unterstützt der Fachdienst VSDM 2.0 nur die Version 1.0.0.

    Angenommen das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal
    Angenommen das Primärsystem in der LEI verwendet eine SMC-B <Smcb-Card> im Slot <Smcb-Slot>
    Angenommen der Versicherte in der LEI verwendet eine eGK <Egk-Card> im Slot <Egk-Slot>
    Wenn das Primärsystem die VSD mit einer unbekannten FHIR Profile Version <Profile-Version> abfragt
    Dann antwortet der VSDM Ressource Server mit dem Fehlercode <Http-Code> und dem Text <Error-Code>

    Beispiele:
      | Smcb-Card           | Smcb-Slot | Egk-Card           | Egk-Slot | Http-Code | Error-Code                           | Profile-Version |
      | "smcbCardImage.xml" | 1         | "egkCardData.json" | 2        | 400       | "VSDSERVICE_INVALID_PROFILE_VERSION" | "1.2.3"         |
