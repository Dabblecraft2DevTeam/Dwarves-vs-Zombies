package me.andre111.dvz.disguise;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.Game;
import me.andre111.dvz.customclass.CustomClass;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher;
import me.libraryaddict.disguise.events.UndisguiseEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DSystem_LibsDisguises implements DSystem, Listener {
	private int nextID = Integer.MIN_VALUE;
	
	@Override
	public void initListeners(DvZ plugin) {
		DisguiseConfig.setViewSelfDisguisesDefault(true);
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void disguiseP(Player player, DvZDisguiseType disguise) {
		MobDisguise mdis;
		switch(disguise) {
		case VILLAGER_ZOMBIE:
			mdis = new MobDisguise(DisguiseType.ZOMBIE_VILLAGER, !disguise.isBaby());
			break;
		case WITHER_SKELETON:
			mdis = new MobDisguise(DisguiseType.WITHER_SKELETON, !disguise.isBaby());
			break;
		//----------
		//SPECIAL
		//----------
		//HORSES
		case UNDEAD_HORSE:
			mdis = new MobDisguise(DisguiseType.ZOMBIE_HORSE, !disguise.isBaby());
			break;
		case SKELETON_HORSE:
			mdis = new MobDisguise(DisguiseType.SKELETON_HORSE, !disguise.isBaby());
			break;
		//CHARGED
		case CHARGED_CREEPER:
			mdis = new MobDisguise(DisguiseType.CREEPER, !disguise.isBaby());
			if (mdis.getWatcher() instanceof CreeperWatcher) {
				((CreeperWatcher) mdis.getWatcher()).setPowered(true);
			}
			break;
		default:
			DisguiseType dt = DisguiseType.getType(disguise.getEntityType());
			if (dt == null) {
				try {
					dt = DisguiseType.valueOf(disguise.name());
				} catch (Exception ignored) {}
			}
			if (dt == null) {
				dt = DisguiseType.ZOMBIE;
			}
			mdis = new MobDisguise(dt, !disguise.isBaby());
			break;
		}
		mdis.setViewSelfDisguise(true);
		mdis.setHearSelfDisguise(true);
		DisguiseAPI.disguiseToAll(player, mdis);
	}

	@Override
	public void disguiseP(Player player, String disguise) {
		try {
			EntityType et = EntityType.valueOf(disguise.toUpperCase());
			DisguiseType dt = DisguiseType.getType(et);
			MobDisguise mdis = new MobDisguise(dt, true);
			mdis.setViewSelfDisguise(true);
			mdis.setHearSelfDisguise(true);
			DisguiseAPI.disguiseToAll(player, mdis);
		} catch (Exception e) {
			try {
				DisguiseType dt = DisguiseType.valueOf(disguise.toUpperCase());
				MobDisguise mdis = new MobDisguise(dt, true);
				mdis.setViewSelfDisguise(true);
				mdis.setHearSelfDisguise(true);
				DisguiseAPI.disguiseToAll(player, mdis);
			} catch (Exception ex) {
				DvZ.log("Unknown Entity-Disguise: " + disguise);
			}
		}
	}

	@Override
	public void undisguiseP(Player player) {
		DisguiseAPI.undisguiseToAll(player);
	}

	@Override
	public void redisguiseP(Player player) {
		Disguise dis = DisguiseAPI.getDisguise(player);
		if (dis != null) {
			DisguiseAPI.undisguiseToAll(player);
			DisguiseAPI.disguiseToAll(player, dis);
		}
	}

	@Override
	public int newEntityID() {
		return nextID++;
	}

	@EventHandler
	public void onPlayerUndisguise(UndisguiseEvent event) {
		Entity e = event.getEntity();
		if (!(e instanceof Player)) return;
		
		Player p = (Player) e;
		Game game = DvZ.instance.getPlayerGame(p.getUniqueId());
		if (game != null) {
			if (game.isRunning()) {
				CustomClass cc = game.getClass(p.getUniqueId());
				if (cc != null && cc.getDisguise() != null && !cc.getDisguise().isEmpty()) {
					event.setCancelled(true);
				}
			}
		}
	}
}
