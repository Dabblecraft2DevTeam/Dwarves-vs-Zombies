package me.andre111.dvz.volatileCode;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.andre111.dvz.DvZ;
import me.andre111.dvz.disguise.DisguiseSystemHandler;
import me.andre111.dvz.event.DvZInvalidInteractEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;

public abstract class DvZPackets {
	private static int D_ENTITY_ID;
	private static WrappedDataWatcher zombieWatcher;
	private static final Map<UUID, BossBar> playerBars = new ConcurrentHashMap<>();

	public static void setEntityID(int eid) {
		D_ENTITY_ID = eid;
	}

	public static void sendInfoBar(Player player, double percentage, String name) {
		if (player == null || !player.isOnline()) return;
		BossBar bar = playerBars.computeIfAbsent(player.getUniqueId(), uuid -> {
			BossBar b = Bukkit.createBossBar(name, BarColor.PURPLE, BarStyle.SOLID);
			b.addPlayer(player);
			return b;
		});
		bar.setTitle(name);
		bar.setProgress(Math.max(0.0, Math.min(1.0, percentage)));
		bar.setVisible(true);
	}

	public static void sendParticles(Location loc, String name, float spreadHor, float spreadVert, float speed, int count, int radius) {
		if (loc == null || loc.getWorld() == null) return;
		Particle particle = null;
		try {
			particle = Particle.valueOf(name.toUpperCase());
		} catch (Exception ignored) {
			particle = mapLegacyParticle(name);
		}
		if (particle != null) {
			loc.getWorld().spawnParticle(particle, loc, count, spreadHor, spreadVert, spreadHor, speed);
		}
	}

	private static Particle mapLegacyParticle(String name) {
		if (name == null) return null;
		String clean = name.trim().toUpperCase().replace(" ", "_");
		switch (clean) {
		case "SMOKE":
		case "LARGESMOKE":
			return Particle.SMOKE;
		case "FLAME":
			return Particle.FLAME;
		case "HEART":
			return Particle.HEART;
		case "SPELL":
		case "INSTANTSPELL":
			return Particle.ENTITY_EFFECT;
		case "WITCHMAGIC":
			return Particle.WITCH;
		case "MAGICCRIT":
		case "CRIT":
			return Particle.CRIT;
		case "EXPLOSION":
		case "EXPLODE":
			return Particle.EXPLOSION;
		case "HUGEEXPLOSION":
			return Particle.EXPLOSION_EMITTER;
		case "PORTAL":
			return Particle.PORTAL;
		case "ENCHANTMENTTABLE":
			return Particle.ENCHANT;
		case "LAVA":
			return Particle.LAVA;
		case "SPLASH":
			return Particle.SPLASH;
		case "BUBBLE":
			return Particle.BUBBLE;
		case "NOTE":
			return Particle.NOTE;
		case "REDDUST":
			return Particle.DUST;
		case "SNOWBALLPOOF":
			return Particle.ITEM_SNOWBALL;
		case "SLIME":
			return Particle.ITEM_SLIME;
		default:
			try {
				return Particle.valueOf(clean);
			} catch (Exception e) {
				return null;
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void sendBlockBreakAnimToPlayer(Player player, Location loc, byte data) {
		if (player == null || !player.isOnline() || loc == null) return;
		float progress = Math.max(0.0f, Math.min(1.0f, (float) data / 9.0f));
		player.sendBlockDamage(loc, progress, DisguiseSystemHandler.newEntityID());
	}
	
	@SuppressWarnings("deprecation")
	public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		if (player == null || !player.isOnline()) return;
		player.sendTitle(title != null ? title : "", subtitle != null ? subtitle : "", fadeIn, stay, fadeOut);
	}

	// Spawn a fake floating zombie representation
	public static void sendFakeFloatingZombieSpawn(Player p, String world, int x, int y, int z, byte rotation, int entityID, int entityID2, int itemID) {
		if (p == null || !p.getWorld().getName().equals(world)) {
			return;
		}

		double dX = x / 32.0;
		double dY = y / 32.0;
		double dZ = z / 32.0;

		try {
			PacketContainer skullPacket = DvZ.protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
			skullPacket.getIntegers().write(0, entityID2);
			skullPacket.getUUIDs().write(0, UUID.randomUUID());
			skullPacket.getEntityTypeModifier().write(0, EntityType.WITHER_SKULL);
			skullPacket.getDoubles().write(0, dX).write(1, dY).write(2, dZ);
			skullPacket.getBytes().write(0, (byte) 0).write(1, rotation).write(2, (byte) 0);

			PacketContainer zombiePacket = DvZ.protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
			zombiePacket.getIntegers().write(0, entityID);
			zombiePacket.getUUIDs().write(0, UUID.randomUUID());
			zombiePacket.getEntityTypeModifier().write(0, EntityType.ZOMBIE);
			zombiePacket.getDoubles().write(0, dX).write(1, dY).write(2, dZ);
			zombiePacket.getBytes().write(0, (byte) 0).write(1, rotation).write(2, (byte) 0);

			DvZ.protocolManager.sendServerPacket(p, skullPacket);
			DvZ.protocolManager.sendServerPacket(p, zombiePacket);
		} catch (Exception ignored) {
		}
	}

	public static void sendFakeZombieRemove(Player p, int eid) {
		if (p == null || !p.isOnline()) return;
		try {
			PacketContainer destroyPacket = DvZ.protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
			destroyPacket.getIntLists().write(0, Collections.singletonList(eid));
			DvZ.protocolManager.sendServerPacket(p, destroyPacket);
		} catch (Exception ignored) {
		}
	}
	
	public static WrappedDataWatcher getDefaultWatcher(World world, EntityType type) {
		Entity entity = world.spawnEntity(new Location(world, 0, 256, 0), type);
		if (entity instanceof Ageable) {
			((Ageable) entity).setAdult();
		}
		WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
		entity.remove();
		return watcher;
	}
	
	public static void setupInvalidEntityInteractListener() {
		DvZ.protocolManager.addPacketListener(new PacketAdapter(DvZ.instance,
		ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				final Player player = event.getPlayer();
				try {
					PacketContainer packet = event.getPacket();
					final int target = packet.getIntegers().read(0);
					
					EntityUseAction eaction = null;
					try {
						eaction = packet.getEntityUseActions().read(0);
					} catch (Exception e) {
						try {
							WrappedEnumEntityUseAction wrapped = packet.getEnumEntityUseActions().read(0);
							if (wrapped != null) {
								eaction = wrapped.getAction();
							}
						} catch (Exception ignored) {}
					}
					
					int act = 0;
					if (eaction != null) {
						switch (eaction) {
						case ATTACK:
							act = 0;
							break;
						case INTERACT:
						case INTERACT_AT:
							act = 1;
							break;
						default:
							break;
						}
					}
					final int action = act;
					
					Bukkit.getScheduler().runTask(DvZ.instance, new Runnable() {
						public void run() {
							boolean found = false;
							for (Entity e : player.getWorld().getEntities()) {
								if (e.getEntityId() == target) {
									found = true;
									break;
								}
							}
							if (!found) {
								DvZInvalidInteractEvent newEvent = new DvZInvalidInteractEvent(player, target, action);
								plugin.getServer().getPluginManager().callEvent(newEvent);
							}
						}
					});
				} catch (FieldAccessException e) {
					DvZ.log("Couldn't access a field in an 0x07-UseEntity packet!");
				}
			}
		});
	}
}
