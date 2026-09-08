package fr.crewcmoi.other.utils;

import java.util.Locale;

public final class MoneyFormat {

    private MoneyFormat() {
    }

    private static final double THOUSAND = 1_000.0;
    private static final double MILLION = 1_000_000.0;
    private static final double BILLION = 1_000_000_000.0;

    public static String format(double amount) {
        boolean negative = amount < 0;
        double abs = Math.abs(amount);

        String result;
        if (abs >= BILLION) {
            result = trimZeros(abs / BILLION) + "B";
        } else if (abs >= MILLION) {
            result = trimZeros(abs / MILLION) + "M";
        } else if (abs >= THOUSAND) {
            result = trimZeros(abs / THOUSAND) + "K";
        } else {
            result = trimZeros(abs);
        }

        return negative ? "-" + result : result;
    }

    private static String trimZeros(double value) {
        
        String formatted = String.format(Locale.US, "%.2f", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }

    public static double parse(String input) {
        if (input == null) {
            throw new NumberFormatException("Montant vide.");
        }

        String trimmed = input.trim().replace(",", ".");
        if (trimmed.isEmpty()) {
            throw new NumberFormatException("Montant vide.");
        }

        char lastChar = Character.toUpperCase(trimmed.charAt(trimmed.length() - 1));
        double multiplier;
        String numberPart;

        switch (lastChar) {
            case 'K':
                multiplier = THOUSAND;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
                break;
            case 'M':
                multiplier = MILLION;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
                break;
            case 'B':
                multiplier = BILLION;
                numberPart = trimmed.substring(0, trimmed.length() - 1);
                break;
            default:
                multiplier = 1.0;
                numberPart = trimmed;
                break;
        }

        if (numberPart.isEmpty()) {
            throw new NumberFormatException("Montant invalide : " + input);
        }

        double value = Double.parseDouble(numberPart);
        return value * multiplier;
    }
}
