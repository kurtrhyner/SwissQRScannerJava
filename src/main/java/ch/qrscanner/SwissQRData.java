package ch.qrscanner;

/**
 * Parst den Rohtext eines Schweizer QR-Rechnungs-QR-Codes
 * und stellt alle Felder strukturiert bereit.
 */
public class SwissQRData {

    public boolean valid = false;
    public String  error = "";
    public String  raw   = "";

    public String qrType = "", version = "", coding = "";
    public String iban = "";
    public String addrTypeCreditor = "", nameCreditor = "";
    public String streetCreditor = "", houseNoCreditor = "";
    public String zipCreditor = "", cityCreditor = "", countryCreditor = "";
    public String addrTypeUltCreditor = "", nameUltCreditor = "";
    public String streetUltCreditor = "", houseNoUltCreditor = "";
    public String zipUltCreditor = "", cityUltCreditor = "", countryUltCreditor = "";
    public String amount = "", currency = "";
    public String addrTypeDebtor = "", nameDebtor = "";
    public String streetDebtor = "", houseNoDebtor = "";
    public String zipDebtor = "", cityDebtor = "", countryDebtor = "";
    public String referenceType = "", reference = "";
    public String additionalInfo = "", trailer = "", billInfo = "";
    public String altProc1 = "", altProc2 = "";

    public static SwissQRData parse(String rawText) {
        SwissQRData d = new SwissQRData();
        d.raw = rawText;

        // Zeilenumbrüche normalisieren
        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        if (lines.length == 0 || (!lines[0].trim().equals("SPC") && !lines[0].trim().equals("EPD"))) {
            d.valid = false;
            d.error = "Kein gültiger Swiss-QR-Code. Erster Wert: '"
                    + (lines.length > 0 ? lines[0] : "(leer)") + "'";
            return d;
        }

        d.qrType              = get(lines, 0);
        d.version             = get(lines, 1);
        d.coding              = get(lines, 2);
        d.iban                = get(lines, 3);
        d.addrTypeCreditor    = get(lines, 4);
        d.nameCreditor        = get(lines, 5);
        d.streetCreditor      = get(lines, 6);
        d.houseNoCreditor     = get(lines, 7);
        d.zipCreditor         = get(lines, 8);
        d.cityCreditor        = get(lines, 9);
        d.countryCreditor     = get(lines, 10);
        d.addrTypeUltCreditor = get(lines, 11);
        d.nameUltCreditor     = get(lines, 12);
        d.streetUltCreditor   = get(lines, 13);
        d.houseNoUltCreditor  = get(lines, 14);
        d.zipUltCreditor      = get(lines, 15);
        d.cityUltCreditor     = get(lines, 16);
        d.countryUltCreditor  = get(lines, 17);
        d.amount              = get(lines, 18);
        d.currency            = get(lines, 19);
        d.addrTypeDebtor      = get(lines, 20);
        d.nameDebtor          = get(lines, 21);
        d.streetDebtor        = get(lines, 22);
        d.houseNoDebtor       = get(lines, 23);
        d.zipDebtor           = get(lines, 24);
        d.cityDebtor          = get(lines, 25);
        d.countryDebtor       = get(lines, 26);
        d.referenceType       = get(lines, 27);
        d.reference           = get(lines, 28);
        d.additionalInfo      = get(lines, 29);
        d.trailer             = get(lines, 30);
        d.billInfo            = get(lines, 31);
        d.altProc1            = get(lines, 32);
        d.altProc2            = get(lines, 33);

        d.valid = true;
        return d;
    }

    private static String get(String[] lines, int i) {
        return (i < lines.length) ? lines[i].trim() : "";
    }

    public String referenceTypeLong() {
        switch (referenceType) {
            case "QRR":  return "QRR (QR-Referenz)";
            case "SCOR": return "SCOR (Creditor Reference)";
            case "NON":  return "NON (Ohne Referenz)";
            default:     return referenceType;
        }
    }

    public String creditorAddress() {
        return joinParts(streetCreditor, houseNoCreditor, zipCreditor, cityCreditor, countryCreditor);
    }

    public String debtorAddress() {
        return joinParts(streetDebtor, houseNoDebtor, zipDebtor, cityDebtor, countryDebtor);
    }

    private String joinParts(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(p);
            }
        }
        return sb.toString();
    }

    /** Zwischenablage-Ausgabe: IBAN;Referenztyp;Referenznummer;Währung;Betrag; */
    public String clipboardLine() {
        return iban + ";" + referenceType + ";" + reference + ";" + currency + ";" + amount + ";";
    }

    /** Vollständige Anzeige aller Felder. */
    public String fullDisplay() {
        if (!valid) {
            return "Fehler: " + error + "\n\nRohdaten:\n" + raw;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("  SCHWEIZER QR-RECHNUNG – ALLE DATEN\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append(row("IBAN",           iban));
        sb.append(row("Referenztyp",    referenceTypeLong()));
        sb.append(row("Referenznummer", reference));
        sb.append(row("Währung",        currency));
        sb.append(row("Betrag",         amount));

        sb.append("\n───────────────────────────────────────────\n");
        sb.append("  ZAHLUNGSEMPFÄNGER\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append(row("Name",    nameCreditor));
        sb.append(row("Adresse", creditorAddress()));

        if (!nameUltCreditor.isEmpty()) {
            sb.append("\n───────────────────────────────────────────\n");
            sb.append("  ENDGÜLTIGER ZAHLUNGSEMPFÄNGER\n");
            sb.append("───────────────────────────────────────────\n");
            sb.append(row("Name", nameUltCreditor));
        }

        if (!nameDebtor.isEmpty()) {
            sb.append("\n───────────────────────────────────────────\n");
            sb.append("  ZAHLUNGSPFLICHTIGER\n");
            sb.append("───────────────────────────────────────────\n");
            sb.append(row("Name",    nameDebtor));
            sb.append(row("Adresse", debtorAddress()));
        }

        if (!additionalInfo.isEmpty()) {
            sb.append("\n───────────────────────────────────────────\n");
            sb.append("  ZUSATZINFORMATIONEN\n");
            sb.append("───────────────────────────────────────────\n");
            sb.append("  ").append(additionalInfo).append("\n");
        }

        if (!billInfo.isEmpty()) {
            sb.append("\n───────────────────────────────────────────\n");
            sb.append("  RECHNUNGSINFORMATIONEN (Swico)\n");
            sb.append("───────────────────────────────────────────\n");
            sb.append("  ").append(billInfo).append("\n");
        }

        sb.append("\n───────────────────────────────────────────\n");
        sb.append("  TECHNISCHE FELDER\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append(row("QR-Typ",  qrType));
        sb.append(row("Version", version));
        sb.append(row("Coding",  coding));
        sb.append(row("Trailer", trailer));

        sb.append("\n═══════════════════════════════════════════\n");
        sb.append("  ZWISCHENABLAGE (Ausgabe)\n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append("  ").append(clipboardLine()).append("\n");

        return sb.toString();
    }

    private String row(String label, String value) {
        if (value == null || value.isEmpty()) return "";
        String labelCol = String.format("%-22s", label + ":");
        return "  " + labelCol + value + "\n";
    }
}
