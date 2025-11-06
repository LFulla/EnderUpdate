package net.lyof.phantasm.world.spawning;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.lyof.phantasm.entity.custom.MeteoriteEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MeteoriteSpawning {
    private static final int SPAWN_CHANCE = 100000; // Same rarity as lightning (1 in 100,000 per tick)
    private static final int SPAWN_HEIGHT = 384; // Y level to spawn meteorites
    private static final int SPAWN_RADIUS = 512; // Radius around players to spawn meteorites

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(MeteoriteSpawning::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        // Only spawn in the End dimension
        if (world.getRegistryKey() != World.END) {
            return;
        }

        // Check spawn chance (equivalent to lightning)
        if (world.random.nextInt(SPAWN_CHANCE) != 0) {
            return;
        }

        // Try to spawn meteorite near a random player
        if (world.getPlayers().isEmpty()) {
            return;
        }

        var player = world.getPlayers().get(world.random.nextInt(world.getPlayers().size()));
        
        // Generate random spawn position around the player
        double spawnX = player.getX() + (world.random.nextDouble() - 0.5) * SPAWN_RADIUS * 2;
        double spawnZ = player.getZ() + (world.random.nextDouble() - 0.5) * SPAWN_RADIUS * 2;
        double spawnY = SPAWN_HEIGHT;

        // Make sure spawn position is in a reasonable area (not too far from End islands)
        BlockPos spawnPos = new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ);
        
        // Check if there's solid ground below within reasonable distance (to ensure it hits something)
        boolean hasTargetBelow = false;
        for (int y = (int) spawnY; y > 0; y -= 10) {
            BlockPos checkPos = new BlockPos((int) spawnX, y, (int) spawnZ);
            if (!world.getBlockState(checkPos).isAir()) {
                hasTargetBelow = true;
                break;
            }
        }

        // Only spawn if there's something to hit
        if (!hasTargetBelow) {
            return;
        }

        // Create and spawn the meteorite
        MeteoriteEntity meteorite = MeteoriteEntity.create(world, spawnX, spawnY, spawnZ);
        world.spawnEntity(meteorite);

        // Log the spawn for debugging
        System.out.println("Meteorite spawned at " + spawnX + ", " + spawnY + ", " + spawnZ + " near player " + player.getName().getString());
    }
}