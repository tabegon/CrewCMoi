package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.pvp.database.TeamData;
import fr.crewcmoi.pvp.managers.TeamManager;
import fr.crewcmoi.pvp.teamrequest.TeamRequest;
import fr.crewcmoi.pvp.teamrequest.TeamRequestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class TeamRequestGuiManager {
    private final Main plugin;
    private final TeamManager teams;
    private final TeamRequestManager requests;
    private final EconomyManager economy;

    public TeamRequestGuiManager(Main plugin, TeamManager teams, TeamRequestManager requests, EconomyManager economy) {
        this.plugin=plugin; this.teams=teams; this.requests=requests; this.economy=economy;
    }

    public void open(Player player) {
        teams.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) { player.sendMessage(ChatColor.RED+"Tu dois être dans une équipe."); return; }
            open(player,team);
        });
    }

    public void open(Player player, TeamData team) { openRequests(player,team); }

    private void openRequests(Player player, TeamData team) {
        TeamRequestHolder holder=new TeamRequestHolder(team.getId(),false);
        Inventory inv=Bukkit.createInventory(holder,27,color("&8&lDemandes de " + team.getName())); holder.setInventory(inv);
        for(int i=0;i<27;i++) inv.setItem(i,GuiItems.nothing(" "));
        List<TeamRequest> active=requests.active(team.getId());
        int slot=0;
        for(TeamRequest request:active) {
            if(slot>=18) break;
            ItemStack requestedItem=TeamRequestManager.deserializeItem(request.itemData());
            Material icon=request.type().equals("MONEY")?Material.EMERALD:(requestedItem==null?material(request.material()):requestedItem.getType());
            String requestedName=requestedItem!=null && requestedItem.hasItemMeta() && requestedItem.getItemMeta().hasDisplayName()?requestedItem.getItemMeta().getDisplayName():request.material();
            String title=request.type().equals("MONEY")?"&aDemande d'argent":"&eDemande d'objets : " + requestedName;
            List<String> lore=new ArrayList<>();
            lore.add(color("&7Objectif : &f"+format(request.target(),request.type())));
            lore.add(color("&7Reçu : &a"+format(request.collected(),request.type())));
            lore.add(color("&7Manque : &e"+format(request.remaining(),request.type())));
            lore.add(color("&eClique pour contribuer"));
            ItemStack display=item(icon,title,lore); if(requestedItem!=null && !request.type().equals("MONEY")) { ItemMeta dm=display.getItemMeta(); if(dm!=null) { dm.setDisplayName(color(title)); display.setItemMeta(dm); } } inv.setItem(slot,display); holder.mapRequest(slot,request.id()); slot++;
        }
        if(team.isOwner(player.getUniqueId())) {
            inv.setItem(20,item(Material.CHEST,"&eCréer une demande d'objets",List.of(color("&7Tiens l'objet souhaité en main."),color("&7Tu préciseras la quantité dans le chat."))));
            inv.setItem(21,item(Material.EMERALD,"&aCréer une demande d'argent",List.of(color("&7Tu préciseras le montant dans le chat."))));
        }
        inv.setItem(22,item(Material.GOLD_INGOT,"&6Classement des contributions",List.of(color("&7Voir les contributions de l'équipe"))));
        inv.setItem(26,item(Material.BARRIER,"&cRetour",List.of()));
        player.openInventory(inv);
    }

    private void openLeaderboard(Player player, TeamData team) {
        TeamRequestHolder holder=new TeamRequestHolder(team.getId(),true);
        Inventory inv=Bukkit.createInventory(holder,27,color("&8&lContributions de l'équipe")); holder.setInventory(inv);
        for(int i=0;i<27;i++) inv.setItem(i,GuiItems.nothing(" "));
        List<TeamRequestManager.ContributionTotal> totals=requests.leaderboard(team.getId());
        int slot=0,rank=1;
        for(var total:totals) {
            if(slot>=18) break;
            inv.setItem(slot++,item(Material.PLAYER_HEAD,"&e#"+rank++ + " " + total.name(),List.of(color("&7Argent contribué : &a"+MoneyFormat.format(total.money())),color("&7Objets contribué : &b"+total.items()))));
        }
        inv.setItem(26,item(Material.ARROW,"&cRetour aux demandes",List.of())); player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event, Player player, TeamRequestHolder holder, int slot) {
        if(slot<0 || slot>=event.getInventory().getSize()) return;
        teams.getTeamAsync(player.getUniqueId(), team -> {
            if(team==null || team.getId()!=holder.getTeamId()) { player.closeInventory(); return; }
            if(holder.isLeaderboard()) { if(slot==26) openRequests(player,team); return; }
            if(slot==26) { player.closeInventory(); return; }
            if(slot==22) { openLeaderboard(player,team); return; }
            if(slot==20 || slot==21) {
                if(!team.isOwner(player.getUniqueId())) return;
                if(!requests.active(team.getId()).isEmpty()) { player.sendMessage(color("&cVotre équipe a déjà une demande active. Terminez-la avant d'en créer une autre.")); openRequests(player,team); return; }
                player.closeInventory();
                if(slot==20) {
                    ItemStack hand=player.getInventory().getItemInMainHand();
                    if(hand==null || hand.getType().isAir()) { player.sendMessage(color("&cTiens l'objet à demander dans ta main avant de cliquer.")); openRequests(player,team); return; }
                    String encoded=TeamRequestManager.serializeItem(hand);
                    if(encoded==null) { player.sendMessage(color("&cImpossible d'enregistrer cet objet.")); openRequests(player,team); return; }
                    requests.prompt(player,"CREATE_ITEM",0,encoded);
                    player.sendMessage(color("&eEntre la quantité totale à demander dans le chat, ou 'annuler'."));
                } else {
                    requests.prompt(player,"CREATE_MONEY",0);
                    player.sendMessage(color("&eEntre le montant d'argent à demander dans le chat, ou 'annuler'."));
                }
                return;
            }
            Integer requestId=holder.getRequest(slot);
            if(requestId==null) return;
            TeamRequest request=requests.get(requestId,team.getId());
            if(request==null) { openRequests(player,team); return; }
            if(request.type().equals("MONEY")) {
                player.closeInventory(); requests.prompt(player,"CONTRIBUTE_MONEY",requestId);
                player.sendMessage(color("&eCombien veux-tu contribuer ? Solde : &a"+MoneyFormat.format(economy.getBalance(player.getUniqueId()))+" &7(ou 'annuler')."));
            } else {
                contributeItems(player,team,request);
            }
        });
    }

    public void handleChat(Player player, String message) {
        TeamRequestManager.Prompt prompt=requests.takePrompt(player.getUniqueId());
        if(prompt==null) return;
        if(message.equalsIgnoreCase("annuler")) { player.sendMessage(color("&cAction annulée.")); return; }
        teams.getTeamAsync(player.getUniqueId(), team -> {
            if(team==null) { player.sendMessage(color("&cTu n'es plus dans une équipe.")); return; }
            if(prompt.type().equals("CREATE_ITEM") || prompt.type().equals("CREATE_MONEY")) {
                if(!team.isOwner(player.getUniqueId())) { player.sendMessage(color("&cSeul le fondateur peut créer une demande.")); return; }
                if(prompt.type().equals("CREATE_MONEY")) {
                    double amount=parseAmount(message);
                    if(amount<=0) { player.sendMessage(color("&cMontant invalide.")); return; }
                    if(requests.create(team.getId(),player.getUniqueId(),"MONEY",null,null,amount)) player.sendMessage(color("&aDemande d'argent créée.")); else player.sendMessage(color("&cImpossible : votre équipe a déjà une demande active, ou les données sont invalides."));
                } else {
                    int amount;
                    try { amount=Integer.parseInt(message.trim()); } catch(NumberFormatException ex) { amount=0; }
                    ItemStack template=TeamRequestManager.deserializeItem(prompt.itemData());
                    if(template==null || amount<1) { player.sendMessage(color("&cQuantité invalide. Réessaie avec un entier positif.")); return; }
                    String materialName=template.hasItemMeta() && template.getItemMeta().hasDisplayName()?template.getItemMeta().getDisplayName():template.getType().name();
                    if(requests.create(team.getId(),player.getUniqueId(),"ITEM",materialName,prompt.itemData(),amount)) player.sendMessage(color("&aDemande de "+amount+" "+materialName+" créée.")); else player.sendMessage(color("&cImpossible : votre équipe a déjà une demande active, ou les données sont invalides."));
                }
                openRequests(player,team); return;
            }
            if(prompt.type().equals("CONTRIBUTE_MONEY")) {
                TeamRequest request=requests.get(prompt.requestId(),team.getId());
                double amount=parseAmount(message);
                if(request==null || amount<=0 || amount>request.remaining()+0.000001) { player.sendMessage(color("&cMontant invalide ou supérieur au besoin restant.")); openRequests(player,team); return; }
                if(!economy.withdraw(player.getUniqueId(),amount)) { player.sendMessage(color("&cTu n'as pas assez d'argent.")); openRequests(player,team); return; }
                if(!requests.addContribution(request,player,amount)) { economy.deposit(player.getUniqueId(),amount); player.sendMessage(color("&cLa contribution n'a pas pu être enregistrée, ton argent a été rendu.")); }
                else player.sendMessage(color("&aContribution enregistrée : "+MoneyFormat.format(amount)+"."));
                openRequests(player,team);
            }
        });
    }

    private void contributeItems(Player player, TeamData team, TeamRequest request) {
        ItemStack template=TeamRequestManager.deserializeItem(request.itemData());
        if(template==null) { player.sendMessage(color("&cL'objet demandé est introuvable.")); return; }
        int remaining=(int)Math.ceil(request.remaining());
        int available=0;
        for(ItemStack item:player.getInventory().getStorageContents()) if(item!=null && item.isSimilar(template)) available+=item.getAmount();
        int amount=Math.min(remaining,available);
        if(amount<=0) { player.sendMessage(color("&cTu ne possèdes aucun objet correspondant à cette demande.")); return; }
        int left=amount;
        for(int i=0;i<player.getInventory().getStorageContents().length && left>0;i++) {
            ItemStack item=player.getInventory().getItem(i);
            if(item==null || !item.isSimilar(template)) continue;
            int take=Math.min(left,item.getAmount()); item.setAmount(item.getAmount()-take); if(item.getAmount()<=0) player.getInventory().setItem(i,null); left-=take;
        }
        if(!requests.addContribution(request,player,amount)) {
            ItemStack refund=template.clone(); refund.setAmount(amount); player.getInventory().addItem(refund).values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(),it)); player.sendMessage(color("&cLa contribution a échoué, les objets ont été rendus."));
        } else player.sendMessage(color("&aTu as contribué "+amount+" objets."));
        openRequests(player,team);
    }

    private static double parseAmount(String s) { try { return Double.parseDouble(s.replace(",",".").trim()); } catch(Exception e) { return -1; } }
    private static Material material(String s) { try { return Material.valueOf(s.trim().toUpperCase().replace(' ','_')); } catch(Exception e) { return null; } }
    private static String format(double amount,String type) { return type.equals("MONEY")?MoneyFormat.format(amount):Long.toString((long)amount); }
    private ItemStack item(Material mat,String name,List<String> lore) { ItemStack it=new ItemStack(mat); ItemMeta meta=it.getItemMeta(); if(meta!=null){meta.setDisplayName(color(name)); meta.setLore(lore); it.setItemMeta(meta);} return it; }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&',s==null?"":s); }
}
