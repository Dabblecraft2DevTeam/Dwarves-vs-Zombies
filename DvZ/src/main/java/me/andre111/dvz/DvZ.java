package me.andre111.dvz;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.andre111.dvz.commands.DvZCommand;
import me.andre111.dvz.config.ConfigManager;
import me.andre111.dvz.customclass.ClassManager;
import me.andre111.dvz.disguise.DisguiseSystemHandler;
import me.andre111.dvz.dragon.DragonAttackListener;
import me.andre111.dvz.dragon.DragonAttackManager;
import me.andre111.dvz.dragon.DragonDeathListener;
import me.andre111.dvz.event.DVZJoinGameEvent;
import me.andre111.dvz.generator.DvZWorldProvider;
import me.andre111.dvz.listeners.Listener_Block;
import me.andre111.dvz.listeners.Listener_Entity;
import me.andre111.dvz.listeners.Listener_Game;
import me.andre111.dvz.listeners.Listener_Player;
import me.andre111.dvz.manager.BlockManager;
import me.andre111.dvz.manager.BreakManager;
import me.andre111.dvz.manager.HighscoreManager;
import me.andre111.dvz.manager.ItemStandManager;
import me.andre111.dvz.manager.WorldManager;
import me.andre111.dvz.players.SpecialPlayerManager;
import me.andre111.dvz.utils.FileHandler;
import me.andre111.dvz.utils.IconMenuHandler;
import me.andre111.dvz.utils.Invulnerability;
import me.andre111.dvz.utils.Item3DHandler;
import me.andre111.dvz.utils.InventoryHandler;
import me.andre111.dvz.utils.MovementStopper;
import me.andre111.dvz.volatileCode.DvZPackets;
import me.andre111.dvz.volatileCode.DynamicClassFunctions;
import me.andre111.items.SpellItems;
import me.andre111.items.item.spell.DvZSpellLoader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

//TODO - change every playerreference from names to uuids
public class DvZ extends JavaPlugin {
	public static DvZ instance;
	
	private Game[] games = new Game[10];
	public static int startedGames = 0;
	private Lobby lobby;
	
	public static ProtocolManager protocolManager;
	public static SpellItems spellItems;
	
	public static DragonAttackManager dragonAtManager;
	public static DragonDeathListener dragonDeath;
	public static DragonAttackListener attackListener;
	public static MovementStopper moveStop;
	public static Invulnerability inVul;
	public static Item3DHandler item3DHandler;
	public static ItemStandManager itemStandManager;
	//public static EffectManager effectManager;
	public static SpecialPlayerManager playerManager;
	
	public static ClassManager classManager;
	
	public static Logger logger;
	public static final String prefix = "[Dwarves vs Zombies] ";
	
	
	@Override
	public void onLoad() {
		logger = Logger.getLogger("Minecraft");

		// Dynamic package detection
		if (!DynamicClassFunctions.setPackages()) {
			logger.log(Level.WARNING, "NMS/OBC package could not be detected, using " + DynamicClassFunctions.nmsPackage + " and " + DynamicClassFunctions.obcPackage);
		}
		DynamicClassFunctions.setClasses();
		DynamicClassFunctions.setMethods();
		DynamicClassFunctions.setFields();
		
		DvZSpellLoader.addSpells();
	}
	
	@Override
	public void onEnable() {
		if(DvZ.instance==null)
			DvZ.instance = this;
		
		ConfigManager.initConfig(this);
		
		//Dependency check
		if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
		{
			DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), prefix+" "+ChatColor.RED+"ProtocolLib could not be found, disabling...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		if (!Bukkit.getPluginManager().isPluginEnabled("SpellItems"))
		{
			DvZ.sendPlayerMessageFormated(Bukkit.getServer().getConsoleSender(), prefix+" "+ChatColor.RED+"SpellItems could not be found, disabling...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		DvZ.protocolManager = ProtocolLibrary.getProtocolManager();
		DvZ.spellItems = SpellItems.instance;
		
		//Disguises
		if(!DisguiseSystemHandler.init()) {
			return;
		}
		DvZPackets.setEntityID(DisguiseSystemHandler.newEntityID());
		
		IconMenuHandler.instance = new IconMenuHandler(this);
		
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
		SpellItems.luacontroller.loadScript(new File(DvZ.instance.getDataFolder(), "spells.lua").getAbsolutePath());
		
		classManager = new ClassManager();
		classManager.loadClasses();
		
		dragonAtManager = new DragonAttackManager();
		dragonAtManager.loadAttacks();
		dragonDeath = new DragonDeathListener(this);
		attackListener = new DragonAttackListener(this);
		moveStop = new MovementStopper(this);
		inVul = new Invulnerability(this);
		item3DHandler = new Item3DHandler(this);
		itemStandManager = new ItemStandManager();
		//effectManager = new EffectManager();
		//effectManager.loadEffects();
		playerManager = new SpecialPlayerManager();
		playerManager.loadPlayers();
		
		HighscoreManager.loadHighscore();
		BlockManager.loadConfig();
		WorldManager.reload();
		
		if(getConfig().getBoolean("use_lobby", true))
			Bukkit.getServer().createWorld(new WorldCreator(this.getConfig().getString("world_prefix", "DvZ_")+"Lobby"));
		File f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/");
		if(!f.exists()) f.mkdirs();
		f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/Type1/");
		if(!f.exists()) f.mkdirs();
		f = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Worlds/Type2/");
		if(!f.exists()) f.mkdirs();
		for (int i=0; i<10; i++) {
			File f2 = new File(Bukkit.getServer().getWorldContainer().getPath()+"/"+this.getConfig().getString("world_prefix", "DvZ_")+"Main"+i+"/");
			if(f2.exists()) FileHandler.deleteFolder(f2);
		}
		
		lobby = new Lobby(this);
		
		new Listener_Player(this);
		new Listener_Block(this);
		new Listener_Entity(this);
		new Listener_Game(this);
		new DvZWorldProvider(this);
		
		//init and reset games
		for(int i=0; i<games.length; i++) {
			int type = getConfig().getInt("game"+i, 1);
			
			if(type!=0) {
				games[i] = new Game(this, GameType.fromID(type).getFirstType(), i);
				games[i].reset(false);
			}
		}
		//
		
		CommandExecutorDvZ command = new CommandExecutorDvZ();
		for(String st : getDescription().getCommands().keySet()) {
			getCommand(st).setExecutor(command);
		}
		
		//Run Main Game Managment
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for(int i=0; i<games.length; i++) {
					if (games[i]!=null) {
						games[i].tick();
					}
				}
		    }
		}, 20, 20);
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for(int i=0; i<games.length; i++) {
					if (games[i]!=null) {
						games[i].fastTick();
					}
				}
		    }
		}, 1, 1);
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				BreakManager.tick();
				//experimental resend all Itemstands every 10 seconds
				for(Player p : Bukkit.getServer().getOnlinePlayers()) {
					item3DHandler.respawnAll(p);
				}
		    }
		}, 20*10, 20*10);
		//
	}
	
	@Override
	public void onDisable() {
		//Done in game reset
		//HighscoreManager.saveHighscore();
		
		//remove all zombies
		if(item3DHandler!=null)
			item3DHandler.removeAll();
				
		DvZCommand.unregisterCommands();
				
		//remove all release inventories
		for(int i=0; i<games.length; i++) {
			if (games[i]!=null) {
				games[i].reset(false);
			}
		}
		
		if(DvZ.instance!=null)
			DvZ.instance = null;
	}
	
	public static void reloadItems() {
		SpellItems.reload();
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
	}
	
	public static void reloadRewards() {
		SpellItems.reload();
		SpellItems.loadFromConfiguration(ConfigManager.getItemFile());
		SpellItems.addRewardsFromConfiguration(ConfigManager.getRewardFile());
	}
	
	public static void log(String s) {
		logger.info(prefix+s);
	}
	
	//#######################################
	//Bekomme Spiel in dem das der Spieler ist
	//#######################################
	public Game getPlayerGame(UUID player) {
		for(int i=0; i<games.length; i++) {
			if (games[i]!=null) {
				if (games[i].isPlayer(player))
				{
					return games[i];
				}
			}
		}
		
		return null;
	}
	
	public int getGameID(Game game) {
		for(int i=0; i<games.length; i++) {
			if (games[i]==game) {
				return i;
			}
		}
		
		return -1;
	}
	
	public Game getGame(int id) {
		return games[id];
	}
	
	public Lobby getLobby() {
		return lobby;
	}
	
	public int getRunningGameCount() {
		int count = 0;
		
		for(int i=0; i<games.length; i++) {
			if(games[i]!=null)
			if(games[i].isRunning()) {
				count++;
			}
		}
		
		return count;
	}
	
    public void joinGame(Player player, Game game) {
    	joinGame(player, game, false);
	}
    
    public void joinGame(Player player, boolean autojoin) {
    	for(int i=0; i<games.length; i++) {
			if(games[i]!=null) {
				joinGame(player, games[i], autojoin);
				break;
			}
    	}
    }
	
	public void joinGame(Player player, Game game, boolean autojoin) {
		if(!(getConfig().getBoolean("autoadd_players", false) || autojoin) && !game.getPlayerState(player.getUniqueId()).equals(Game.STATE_PREGAME)) return;
		
		game.setPlayerState(player.getUniqueId(), Game.STATE_PREGAME);
		if(ConfigManager.getStaticConfig().getBoolean("use_lobby", true))
			player.teleport(Bukkit.getServer().getWorld(getConfig().getString("world_prefix", "DvZ_")+"Lobby").getSpawnLocation());
		InventoryHandler.clearInv(player, false);

		DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_self_added","You have been added to the game!"));
		
		//event
		DVZJoinGameEvent event = new DVZJoinGameEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(event);

		//autoadd player
		if(game.isRunning()) {
			if (getConfig().getBoolean("autoadd_players", false) || autojoin) {
				game.setPlayerState(player.getUniqueId(), Game.STATE_CHOOSECLASS);
				game.setPlayerTeam(player.getUniqueId(), game.teamSetup.getStartTeam());
				DvZ.sendPlayerMessageFormated(player, ConfigManager.getLanguage().getString("string_choose","Choose your class!"));
				game.addClassItems(player);

				game.broadcastMessage(ConfigManager.getLanguage().getString("string_autoadd","Autoadded -0- to -1- to the Game!").replace("-0-", player.getDisplayName()).replace("-1-", game.getTeam(player.getUniqueId()).getDisplayName()));
			}
		} else {
			if(ConfigManager.getStaticConfig().getBoolean("hscore_in_lobby", true)) {
				if(player.isValid()) {
					player.setScoreboard(HighscoreManager.createOrRefreshPlayerScore(player.getUniqueId()));
				}
			}
		}
	}
	
	public static void sendPlayerMessageFormated(Player player, String message) {
		player.sendMessage(message.split("%n"));
	}
	public static void sendPlayerMessageFormated(CommandSender sender, String message) {
		sender.sendMessage(message.split("%n"));
	}
	
	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}
	public static boolean isPathable(Material material) {
		if (material == null) return false;
		if (material.isAir()) return true;
		if (Tag.SAPLINGS.isTagged(material) ||
			Tag.FLOWERS.isTagged(material) ||
			Tag.CROPS.isTagged(material) ||
			Tag.ALL_SIGNS.isTagged(material) ||
			Tag.PRESSURE_PLATES.isTagged(material) ||
			Tag.BUTTONS.isTagged(material) ||
			Tag.RAILS.isTagged(material)) {
			return true;
		}
		return
				material == Material.WATER ||
				material == Material.DEAD_BUSH ||
				material == Material.BROWN_MUSHROOM ||
				material == Material.RED_MUSHROOM ||
				material == Material.TORCH ||
				material == Material.WALL_TORCH ||
				material == Material.FIRE ||
				material == Material.REDSTONE_WIRE ||
				material == Material.LADDER ||
				material == Material.LEVER ||
				material == Material.REDSTONE_TORCH ||
				material == Material.REDSTONE_WALL_TORCH ||
				material == Material.SNOW ||
				material == Material.SUGAR_CANE ||
				material == Material.VINE ||
				material == Material.LILY_PAD ||
				material == Material.NETHER_WART ||
				material == Material.TRIPWIRE_HOOK ||
				material == Material.TRIPWIRE ||
				material == Material.POTATOES ||
				material == Material.CARROTS ||
				material == Material.SHORT_GRASS ||
				material == Material.TALL_GRASS;
	}
	public final static HashSet<Material> transparent = new HashSet<Material>();
	static {
		transparent.add(Material.AIR);
		transparent.add(Material.WATER);
		transparent.add(Material.DEAD_BUSH);
		transparent.add(Material.BROWN_MUSHROOM);
		transparent.add(Material.RED_MUSHROOM);
		transparent.add(Material.TORCH);
		transparent.add(Material.WALL_TORCH);
		transparent.add(Material.FIRE);
		transparent.add(Material.LADDER);
		transparent.add(Material.LEVER);
		transparent.add(Material.REDSTONE_TORCH);
		transparent.add(Material.REDSTONE_WALL_TORCH);
		transparent.add(Material.REDSTONE_WIRE);
		transparent.add(Material.SNOW);
		transparent.add(Material.SUGAR_CANE);
		transparent.add(Material.VINE);
		transparent.add(Material.LILY_PAD);
		transparent.add(Material.NETHER_WART);
		transparent.add(Material.TRIPWIRE_HOOK);
		transparent.add(Material.TRIPWIRE);
		transparent.add(Material.POTATOES);
		transparent.add(Material.CARROTS);
		transparent.add(Material.SHORT_GRASS);
		transparent.add(Material.TALL_GRASS);
		transparent.addAll(Tag.SAPLINGS.getValues());
		transparent.addAll(Tag.FLOWERS.getValues());
		transparent.addAll(Tag.CROPS.getValues());
		transparent.addAll(Tag.ALL_SIGNS.getValues());
		transparent.addAll(Tag.PRESSURE_PLATES.getValues());
		transparent.addAll(Tag.BUTTONS.getValues());
		transparent.addAll(Tag.RAILS.getValues());
		transparent.addAll(Tag.CARPETS.getValues());
	}
	
	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(DvZ.instance, task, delay, interval);
	}

	public static void cancelTask(int taskId) {
		Bukkit.getScheduler().cancelTask(taskId);
	}
}
