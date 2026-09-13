package fr.crewcmoi.teleport.gui;
import fr.crewcmoi.teleport.database.HomeData;
import org.bukkit.inventory.Inventory; import org.bukkit.inventory.InventoryHolder;
import java.util.HashMap; import java.util.Map;
public class HomeHolder implements InventoryHolder { public static final int SIZE=54, ITEMS=45, PREV=45, NEXT=53; private Inventory inv; private int page; private final Map<Integer,HomeData> map=new HashMap<>(); public Inventory getInventory(){return inv;} public int getPage(){return page;} public void setPage(int page){this.page=Math.max(0,page);} public void setInventory(Inventory i){inv=i;} public void map(int s,HomeData h){map.put(s,h);} public HomeData get(int s){return map.get(s);} public void clear(){map.clear();} }
