package me.andre111.dvz.volatileCode;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class DeprecatedMethods {
	public static Player getPlayerByName(String name) {
		return Bukkit.getPlayerExact(name);
	}
	
	public static int getMaterialID(Material mat) {
		return mat != null ? mat.getId() : 0;
	}
	
	public static byte getDatavalue(MaterialData data) {
		return data != null ? data.getData() : 0;
	}
	
	public static int getBlockID(Block block) {
		return block != null ? block.getType().getId() : 0;
	}
	
	public static void setBlockID(Block block, int id) {
		if (block == null) return;
		Material mat = getMaterialByID(id);
		if (mat != null) {
			block.setType(mat);
		}
	}
	
	public static byte getBlockData(Block block) {
		if (block == null) return 0;
		MaterialData md = block.getState().getData();
		return md != null ? md.getData() : 0;
	}
	
	public static void setBlockData(Block block, byte data) {
		if (block == null) return;
		try {
			BlockData bd = Bukkit.getUnsafe().fromLegacy(block.getType(), data);
			if (bd != null) {
				block.setBlockData(bd, false);
				return;
			}
		} catch (Exception ignored) {}
		block.setBlockData(block.getType().createBlockData(), false);
	}
	
	public static void setBlockData(Block block, byte data, boolean applyPhysics) {
		if (block == null) return;
		try {
			BlockData bd = Bukkit.getUnsafe().fromLegacy(block.getType(), data);
			if (bd != null) {
				block.setBlockData(bd, applyPhysics);
				return;
			}
		} catch (Exception ignored) {}
		block.setBlockData(block.getType().createBlockData(), applyPhysics);
	}
	
	public static void setBlockIDandData(Block block, int id, byte data, boolean applyPhysics) {
		if (block == null) return;
		Material mat = getMaterialByID(id);
		if (mat == null) return;
		try {
			BlockData bd = Bukkit.getUnsafe().fromLegacy(mat, data);
			if (bd != null) {
				block.setBlockData(bd, applyPhysics);
				return;
			}
		} catch (Exception ignored) {}
		block.setType(mat, applyPhysics);
	}
	
	public static Material getMaterialByID(int id) {
		for (Material m : Material.values()) {
			if (m.getId() == id) {
				return Bukkit.getUnsafe().fromLegacy(m);
			}
		}
		return null;
	}
	
	public static PotionEffectType getPotionEffectByID(int id) {
		return PotionEffectType.getById(id);
	}
	
	public static FallingBlock spawnFallingBlock(Location loc, Material mat, byte data) {
		if (loc == null || loc.getWorld() == null || mat == null) return null;
		BlockData bd = Bukkit.getUnsafe().fromLegacy(mat, data);
		if (bd == null) bd = mat.createBlockData();
		return loc.getWorld().spawnFallingBlock(loc, bd);
	}
	
	public static void sendBlockChange(Player player, Location loc, Material mat, byte data) {
		if (player == null || loc == null || mat == null) return;
		BlockData bd = Bukkit.getUnsafe().fromLegacy(mat, data);
		if (bd == null) bd = mat.createBlockData();
		player.sendBlockChange(loc, bd);
	}
	
	public static ItemStack createItemStackByID(int id) {
		Material mat = getMaterialByID(id);
		if (mat != null) {
			return new ItemStack(mat);
		}
		return new ItemStack(Material.AIR);
	}
	
	public static int getEntityTypeID(EntityType type) {
		return type != null ? type.getTypeId() : 0;
	}
	
	public static Material getMaterialFromInternalName(String name) {
		if (name == null) return null;
		String clean = name.trim().toUpperCase().replace("MINECRAFT:", "");
		Material mat = Material.matchMaterial(clean);
		if (mat == null) {
			mat = Material.matchMaterial(clean, true);
		}
		if (mat != null && mat.isLegacy()) {
			mat = Bukkit.getUnsafe().fromLegacy(mat);
		}
		return mat;
	}

	public static List<String> tabCompleteInternalName(String name) {
		List<String> list = new ArrayList<>();
		String lower = name != null ? name.toLowerCase() : "";
		for (Material m : Material.values()) {
			if (!m.isLegacy()) {
				String mName = m.name().toLowerCase();
				if (mName.startsWith(lower)) {
					list.add(mName);
				}
			}
		}
		return list;
	}
}
