package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.teleport.gui.HomeAdminGuiManager; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class HomeAdminCommand implements CommandExecutor {private final HomeAdminGuiManager gui;public HomeAdminCommand(HomeAdminGuiManager g){gui=g;}public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p))return true;if(!p.hasPermission("crew.home.admin")){p.sendMessage("§cVous n'avez pas la permission.");return true;}gui.open(p);return true;}}
