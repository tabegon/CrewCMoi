package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.teleport.managers.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public class SetHomeCommand implements CommandExecutor {
    private final Main plugin;
    private final HomeManager manager;
    public SetHomeCommand(Main plugin, HomeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    public boolean onCommand(CommandSender s,Command c,String l,String[] a) {
        if(!(s instanceof Player p)) {
            Messages.send(s,"teleport.sethome.player-only");
            return true;
        }
        String questWorld = plugin.getConfig().getString("quest-world.name", "quest");
        if (p.getWorld().getName().equalsIgnoreCase(questWorld)) {
            p.sendMessage(color("&cTu ne peux pas définir de home dans le monde Quest."));
            return true;
        }
        String name=a.length==0?"home":a[0];
        if(a.length>1||!name.matches("[A-Za-z0-9_-]{1,24}")) {
            p.sendMessage(color("&cUsage : /sethome [nom] &7(1-24 caractères : lettres, chiffres, _ ou -)"));
            return true;
        }
        manager.setHome(p,name,ok->p.sendMessage(color(ok?"&aHome &f"+name+" &adéfini !":"&cTu as atteint ta limite de homes. Achète un slot avec &f/shop home&c.")));
        return true;
    }
    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&',s);
    }
}
