package me.andre111.dvz.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class ChunkGeneratorDvZ extends ChunkGenerator {
	private int waterlevel = 56;

	// This needs to be set to return true to override minecraft's default behaviour
	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}

	@Override
	public ChunkData generateChunkData(World world, Random rand, int chunkx, int chunkz, BiomeGrid biome) {
		ChunkData chunk = createChunkData(world);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int trueX = chunkx * 16 + x;
				int trueZ = chunkz * 16 + z;
				
				double h = DvZGeneratorFunction.get(trueX, trueZ, world.getSeed()) * 127;
				int y;
				int yt;
				// stone and grass
				for (y = 0; y < h; y++) {
					chunk.setBlock(x, y, z, Material.STONE);
					
					// grass
					if (y >= h - 1) {
						chunk.setBlock(x, y, z, Material.GRASS_BLOCK);
					// dirt
					} else if (y >= h - 5) {
						chunk.setBlock(x, y, z, Material.DIRT);
					}
				}
				// water and sand
				yt = y;
				for (y -= 3; y < waterlevel; y++) {
					if (y < yt)
						chunk.setBlock(x, y, z, Material.SAND);
					else
						chunk.setBlock(x, y, z, Material.WATER);
				}
				// This will set the floor of each chunk at bedrock level to bedrock
				chunk.setBlock(x, 0, z, Material.BEDROCK);
			}
		}
		return chunk;
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return Arrays.asList(
				new SpawnPopulator(), 
				new BiomePopulator(),
				new TreeLogPopulator(),
				new DwarfPopulator(),
				new PathPopulator());
	}
	
	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		int x = 0;
		int z = 0;
		int y = world.getHighestBlockYAt(x, z);
		return new Location(world, x, y, z);
	}
}
