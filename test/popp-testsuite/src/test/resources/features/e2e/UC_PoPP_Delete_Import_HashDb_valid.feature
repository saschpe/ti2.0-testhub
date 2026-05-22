#language: de
#noinspection NonAsciiCharacters,SpellCheckingInspection

@PRODUKT:PoPP_Service
@TYPE:E2E
Funktionalität: Import und löschen von Daten in eGK-Hash-Datenbank

  @TCID:UC_PoPP_Import_Delete_hashDb_Valid
  @STATUS:InBearbeitung
  @MODUS:Manuell
  @TESTFALL:Positiv
  @TESTSTUFE:3
  @PRIO:1
  @DESCRIPTION

  Szenario: Erfolgreicher Import löschen von Daten in eGK-Hash-Datenbank

  Dieser Testfall testet den Anwendungsfall Import und anschließendes löschen von Daten in eGK-Hash-Datenbank.
  - Ausgangszustand: eGK ist nicht in der hashDB bekannt
  - Ein TSP möchte Daten in die eGK-Hash-Datenbank importieren.
  - Verifizieren durch erfolgreiches anfordern eines PoPP-Token
  - Ein TSP möchte die Daten aus der hashDB löschen
  - Daten sind erfolgreich gelöscht wenn mit der Karte kontaktlos kein PoPP-Token angefordert werden kann

  Der TSP etabliert einen TLS Kanal mit dem PoPP-Service. Der TSP ruft die Schnittstelle I_PoPP_EHC_CertHash_Import
  auf um die Daten zu importieren.

    Angenommen TGR lösche aufgezeichnete Nachrichten
    Und der TSP verwendet die Client Identität "tspEgkTlsValid" für die mTLS-Verbindung zum PoPP-Service
    Und der TSP löscht alle Einträge zur der genutzen eGK

    Und der Versicherte in der LEI präsentiert seine eGK "Standardleser" am Lesegerät "kontaktlos"
    Und TGR pausiere Testausführung mit Nachricht "Lege eGK auf den kontaktlosen Kartenleser"
    Und das Primärsystem den PoPP-Token vom PoPP-Service abfragt und erwarte einen Fehler


    Wenn der TSP sendet den signierten eContent "80276883110000144098.eContent-signed" an den PoPP Service
    Dann der TSP erhält eine positive Rückmeldung mit einer jobID
    Und der TSP fragt den Status seines Imports ab
    Und der TSP erhält Informationen über den Status seines Imports

    Und der TSP wartet auf Abschluss des Imports

    Und der Versicherte in der LEI präsentiert seine eGK "Standardleser" am Lesegerät "kontaktlos"
    Und das Primärsystem den PoPP-Token vom PoPP-Service abfragt
    Dann das PoPP-Token ist vollständig und spezifikationskonform





