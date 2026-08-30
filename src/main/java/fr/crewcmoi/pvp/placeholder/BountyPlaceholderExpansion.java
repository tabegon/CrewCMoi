package fr.crewcmoi.pvp.placeholder;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.utils.MoneyFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Expansion PlaceholderAPI exposant la prime active d'un joueur, pour être intégrée dans
 * n'importe quel plugin de HUD/nametag qui lit PlaceholderAPI (ex: RPGhuds).
 *
 * RPGhuds gère l'affichage au-dessus de la tête des joueurs par ses propres moyens et
 * n'affiche donc pas le suffixe d'équipe scoreboard vanilla posé par CrewCMoi
 * (voir BountyManager#applyBountyDisplay) : la prime doit être injectée dans SA config de
 * nametag/HUD via ces placeholders pour être visible en jeu.
 *
 * Placeholders exposés :
 *  - %crewcmoi_bounty%           : montant brut formaté (ex: "1,5K"), ou "" si aucune prime.
 *  - %crewcmoi_bounty_suffix%    : "<montant> §f" prêt à coller directement après un pseudo
 *                                  dans un nametag (ex: dans la config RPGhuds), ou "" si
 *                                  aucune prime active.
 *  - %crewcmoi_has_bounty%       : "true"/"false", pratique pour un affichage conditionnel.
 */
public class BountyPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final BountyManager bountyManager;

    public BountyPlaceholderExpansion(Main plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "crewcmoi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MonsieurTh30";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // L'expansion reste enregistrée même si le joueur qui déclenche le rendu (ex: un autre
    // joueur qui regarde le nametag) change : on ne veut pas qu'elle soit "unload" entre deux
    // recharges de PlaceholderAPI.
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        double total = bountyManager.getCachedBountyTotal(player.getUniqueId());

        if (params.equalsIgnoreCase("bounty")) {
            return total > 0 ? MoneyFormat.format(total) : "";
        }

        if (params.equalsIgnoreCase("bounty_suffix")) {
            return total > 0 ? ("§6 " + MoneyFormat.format(total) + " §f") : "";
        }

        if (params.equalsIgnoreCase("has_bounty")) {
            return String.valueOf(total > 0);
        }

        return null;
    }
}
