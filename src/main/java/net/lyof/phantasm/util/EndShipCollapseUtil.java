package net.lyof.phantasm.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EndShipCollapseUtil {
    
    public static void triggerCollapse(World world, BlockPos runePos) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;
        
        // Play dramatic sound
        world.playSound(null, runePos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 
            SoundCategory.BLOCKS, 2.0f, 0.5f);
        
        // Send warning message to nearby players
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(runePos.getX(), runePos.getY(), runePos.getZ()) < 50 * 50) {
                player.sendMessage(Text.literal("The ship's power source has been destabilized! The ship is falling apart!")
                    .formatted(Formatting.RED, Formatting.BOLD), false);
            }
        }
        
        // Schedule the actual collapse after exactly 5 seconds
        serverWorld.getServer().execute(() -> {
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Exactly 5 second delay
                    // Execute the collapse on the main server thread
                    serverWorld.getServer().execute(() -> {
                        collapseShipBlocks(world, runePos);
                        
                        // Send final message to nearby players
                        for (PlayerEntity player : world.getPlayers()) {
                            if (player.squaredDistanceTo(runePos.getX(), runePos.getY(), runePos.getZ()) < 50 * 50) {
                                player.sendMessage(Text.literal("The End ship has collapsed!")
                                    .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }
    
    private static void collapseShipBlocks(World world, BlockPos center) {
        int searchRadius = 25; // Adjust based on End ship size
        
        // Collect all ship blocks first
        java.util.List<BlockPos> shipBlocks = new java.util.ArrayList<>();
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -10; y <= 21; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    // Check if this is likely a ship block
                    if (isShipBlock(state)) {
                        shipBlocks.add(pos);
                    }
                }
            }
        }
        
        // Make all blocks fall at once - dramatic crash!
        for (BlockPos pos : shipBlocks) {
            BlockState state = world.getBlockState(pos);
            
            // Remove block and create falling block entity
            world.removeBlock(pos, false);
            FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(world, pos, state);
            
            // Add some random velocity for dramatic effect
            double randomX = (world.random.nextDouble() - 0.5) * 0.4;
            double randomZ = (world.random.nextDouble() - 0.5) * 0.4;
            double randomY = world.random.nextDouble() * 0.2; // Small upward push for some blocks
            fallingBlock.setVelocity(randomX, randomY, randomZ);
        }
        
        // Play additional crash sound
        world.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 
            SoundCategory.BLOCKS, 3.0f, 0.8f);
    }
    
    private static boolean isShipBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.PURPUR_BLOCK ||
               block == Blocks.PURPUR_PILLAR ||
               block == Blocks.PURPUR_STAIRS ||
               block == Blocks.PURPUR_SLAB ||
               block == Blocks.OBSIDIAN ||
               block == Blocks.IRON_BARS ||
               state.isIn(BlockTags.BANNERS) ||
               block == Blocks.BEACON ||
               block == Blocks.END_STONE_BRICKS ||
               block == Blocks.END_STONE_BRICK_STAIRS ||
               block == Blocks.END_STONE_BRICK_SLAB ||
               block == Blocks.CHEST ||
               block == Blocks.DRAGON_HEAD ||
               block == Blocks.MAGENTA_STAINED_GLASS ||
               block == Blocks.END_ROD ||
               block == Blocks.BREWING_STAND;
    }
}