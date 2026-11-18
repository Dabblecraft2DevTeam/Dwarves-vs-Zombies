package me.andre111.dvz.volatileCode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

public class DynamicClassFunctions {
	public static String nmsPackage = "net.minecraft.server";
	public static String obcPackage = "org.bukkit.craftbukkit";
	
	public static final HashMap<String, Class<?>> classes = new HashMap<>();
	public static final HashMap<String, Method> methods = new HashMap<>();
	public static final HashMap<String, Field> fields = new HashMap<>();
	
	public static boolean setPackages() {
		return true;
	}
	
	public static boolean setClasses() {
		return true;
	}
	
	public static boolean setMethods() {
		return true;
	}
	
	public static boolean setFields() {
		return true;
	}
	
	public static void bindRegionFiles() {
	}

	public static void unbindRegionFiles() {
	}
	
	public static synchronized boolean clearWorldReference(String worldName) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public static void forceUnloadWorld(World world) {
		if (world == null) return;
		world.setAutoSave(false);
		for (Player player : world.getPlayers()) {
			player.kickPlayer("World is being deleted... and you were in it!");
		}
		Bukkit.unloadWorld(world, false);
	}
	
	protected static Object getMinecraftServer() {
		return null;
	}

	public static void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
		if (block == null) return;
		block.setHurtEntities(true);
		block.setDamagePerBlock(damage);
		block.setMaxDamage(max);
	}
}
