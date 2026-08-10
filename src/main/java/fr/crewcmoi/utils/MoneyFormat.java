package fr.crewcmoi.utils;

import java.util.Locale;

/**
 * Formatage et parsing des montants d'argent avec des raccourcis K/M/B :
 *  - Affichage : plus de virgules de milliers, remplacées par des suffixes
 *    (1 000 -> "1K", 1 500 000 -> "1.5M", 1 000 000 000 -> "1B", etc.).
 *  - Saisie : les commandes (/pay, /bounty, /money, ...) acceptent ces mêmes
 *    raccourcis en entrée (ex: "1k", "2.5M", "1B"), en plus des nombres classiques.
 */
public final class MoneyFormat {

    private MoneyFormat() {
    }

    private static final double THOUSAND = 1_000.0;
    private static final double MILLION = 1_000_000.0;
    private static final double BILLION = 1_000_000_000.0;

    /**
     * Formate un montant pour l'affichage, sans virgule de milliers, avec les
     * suffixes K/M/B au-delà de 1000. Le nombre de décimales est réduit au strict
     * nécessaire (pas de zéros inutiles), avec un maximum de 2 décimales.
     *
     * Exemples : 0 -> "0", 42.5 -> "42.5", 999.999 -> "1000", 1000 -> "1K",
     * 1500 -> "1.5K", 1000000 -> "1M", 2500000000.0 -> "2.5B".
     */
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
        // Arrondi à 2 décimales puis retire les zéros (et le point) inutiles.
        String formatted = String.format(Locale.US, "%.2f", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }

    /**
     * Parse un montant saisi par un joueur dans une commande, en acceptant les
     * raccourcis K/M/B (insensibles à la casse), les points ET les virgules comme
     * séparateur décimal (ex: "1,5k" ou "1.5k"). Lève NumberFormatException si le
     * texte n'est pas un montant valide.
     *
     * Exemples : "1000" -> 1000.0, "1k" -> 1000.0, "2.5M" -> 2500000.0,
     * "1B" -> 1000000000.0.
     */
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
