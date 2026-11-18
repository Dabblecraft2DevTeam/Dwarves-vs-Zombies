package me.andre111.dvz.disguise;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.event.DvZInvalidInteractEvent;
import me.andre111.dvz.volatileCode.DvZPackets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DisguiseSystemHandler implements Listener {
	private static SupportedDisguises dsystem = SupportedDisguises.NOONE;
	private static DSystem disguisesystem = null;
	
	public static boolean init() {
		//Disguise-Plugin check
		if (Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
			dsystem = SupportedDisguises.LIBSDISGUISES;
		}
			
		if (dsystem == SupportedDisguises.NOONE) {
			DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), DvZ.prefix + ChatColor.RED + "No supported disguising Plugin could be found, disabling...");
			Bukkit.getPluginManager().disablePlugin(DvZ.instance);
			return false;
		}
		
		//init disguisesystem
		DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), DvZ.prefix + "Using " + dsystem.getName() + " to handle Disguising");
		
		disguisesystem = new DSystem_LibsDisguises();
		disguisesystem.initListeners(DvZ.instance);
		
		//setup playerinteract with invalid entity stuffs
		setupInteractListener();
		
		return true;
	}
	
	public static void disguiseP(Player player, String disguise) {
		DvZDisguiseType dtype = DvZDisguiseType.getDisguise(disguise);
		if (dtype != null) {
			disguisesystem.disguiseP(player, dtype);
		} else {
			DvZ.log("Disguise unknown - " + disguise + " - trying to let the Disguiseplugin interpret it!");
			disguisesystem.disguiseP(player, disguise);
		}
	}
	
	public static void undisguiseP(Player player) {
		if (disguisesystem != null) {
			disguisesystem.undisguiseP(player);
		}
	}
	
	public static void redisguiseP(Player player) {
		if (disguisesystem != null) {
			disguisesystem.redisguiseP(player);
		}
	}
	
	public static int newEntityID() {
		return disguisesystem != null ? disguisesystem.newEntityID() : 0;
	}
	
	//Interact with invalid stuff listener
	public static void setupInteractListener() {
		DvZPackets.setupInvalidEntityInteractListener();
		
		Bukkit.getPluginManager().registerEvents(new DisguiseSystemHandler(), DvZ.instance);
	}
	
	//rightclicking invalidstuffs
	@EventHandler
	public void onPlayerInvalidInteractEntity(final DvZInvalidInteractEvent event) {
		Player player = event.getPlayer();
		if (player != null) {
			//clicking on "3D-Items"
			DvZ.item3DHandler.clickOnInvalidEntity(player, event.getTarget());
		}
	}
}
