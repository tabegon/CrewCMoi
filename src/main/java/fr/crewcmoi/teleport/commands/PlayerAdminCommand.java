package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.teleport.gui.PlayerAdminGuiManager; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class PlayerAdminCommand implements CommandExecutor {private final PlayerAdminGuiManager gui;public PlayerAdminCommand(PlayerAdminGuiManager g){gui=g;}public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p))return true;if(!p.hasPermission("crew.playerinfo.admin")){p.sendMessage("§cVous n'avez pas la permission.");return true;}gui.open(p);return true;}}
