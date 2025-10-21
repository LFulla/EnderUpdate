package net.lyof.phantasm.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.lyof.phantasm.block.ModBlocks;
import net.lyof.phantasm.config.ConfigEntries;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndSpikeFeature.class)
public abstract class EndSpikeFeatureMixin {
    
    @Inject(method = "generateSpike", at = @At("HEAD"), cancellable = true)
    public void generateMassiveAngledSpike(ServerWorldAccess world, Random random, EndSpikeFeatureConfig config, 
                                          EndSpikeFeature.Spike spike, CallbackInfo ci) {
        if (ConfigEntries.improveEndSpires) {
            // Cancel the original spike generation
            ci.cancel();
            
            // Generate our massive angled spike
            this.createMassiveAngledSpike(world, random, spike);
        }
    }
    
    private void createMassiveAngledSpike(ServerWorldAccess world, Random random, EndSpikeFeature.Spike spike) {
        // Original spike position
        int originalX = spike.getCenterX();
        int originalZ = spike.getCenterZ();
        
        // Calculate distance from origin (dragon nest at 0,0)
        double distanceFromOrigin = Math.sqrt(originalX * originalX + originalZ * originalZ);
        
        // Push spires further out by increasing distance
        double pushDistance = 10;
        double multiplier = (distanceFromOrigin + pushDistance) / distanceFromOrigin;
        
        // New position further from dragon nest
        int newX = (int)(originalX * multiplier);
        int newZ = (int)(originalZ * multiplier);
        
        BlockPos centerPos = new BlockPos(newX, 0, newZ);
        int baseRadius = spike.getRadius() + 5;
        
        int spikeHeight = 130;
        
        // Generate multiple angled spires from the base
        int numSpires = 3; 
        
        // Calculate direction toward dragon nest (0,0)
        double directionToNest = Math.atan2(-newZ, -newX); // Direction from spire position to (0,0)
        
        for (int spireIndex = 0; spireIndex < numSpires; spireIndex++) {
            double angleOffset = (Math.PI * spireIndex / (numSpires - 1)) - (Math.PI / 3); 
            angleOffset += (random.nextDouble() - 0.5) * 0.3; // Add small random variation
            
            double angle = directionToNest + angleOffset;
            
            // Calculate the direction relative to the nest
            double relativeAngle = angle - directionToNest;
            
            // Reduce lean intensity for "backward" angles (away from 0,0)
            double leanIntensity;
            if (Math.abs(relativeAngle) > Math.PI / 2) {
                // This spire leans "backward" - use minimal lean to prevent cutoff
                leanIntensity = 0.02 + random.nextDouble() * 0.03; // 0.02-0.05 (very minimal)
            } else {
                // This spire leans "forward" - can use normal lean
                leanIntensity = 0.15 + random.nextDouble() * 0.2; // 0.15-0.35 (normal)
            }
            
            double leanX = Math.cos(angle) * leanIntensity;
            double leanZ = Math.sin(angle) * leanIntensity;
            
            // Vary height per spire
            int thisSpireHeight = (int)(spikeHeight + random.nextInt(5)); 
            
            this.generateAngledSpire(world, random, centerPos, baseRadius, thisSpireHeight, leanX, leanZ);
        }
        
        BlockPos centralTop = this.generateCentralSpike(world, random, centerPos, baseRadius, spikeHeight - 5);
        
        // Place end crystal on top of central spire
        this.placeEndCrystal(world, centralTop);
    }
    
    private void generateAngledSpire(ServerWorldAccess world, Random random, BlockPos centerPos, 
        int baseRadius, int height, double leanX, double leanZ) {
        for (int y = 0; y < height; y++) {
            // Calculate current position with lean
            double currentX = centerPos.getX() + (leanX * y * 0.8);
            double currentZ = centerPos.getZ() + (leanZ * y * 0.8);
            
            // Calculate radius that tapers toward the top
            double heightFactor = (double) y / height;
            double radiusFactor = Math.max(0.1, 1.0 - heightFactor * heightFactor); // Quadratic taper
            int currentRadius = Math.max(1, (int)(baseRadius * radiusFactor));
            
            // Generate circular cross-section at this height
            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance <= currentRadius) {
                        BlockPos blockPos = new BlockPos(
                            (int)Math.round(currentX) + dx,
                            centerPos.getY() + y,
                            (int)Math.round(currentZ) + dz
                        );
                        
                        BlockState state = this.getSpikeBlockState(random, y, height);
                        this.setBlockState(world, blockPos, state);
                    }
                }
            }
        }
    }
    
    private BlockPos generateCentralSpike(ServerWorldAccess world, Random random, BlockPos centerPos, 
                                    int baseRadius, int height) {
        for (int y = 0; y < height; y++) {
            double heightFactor = (double) y / height;
            double radiusFactor = Math.max(0.2, 1.0 - heightFactor);
            int currentRadius = Math.max(1, (int)(baseRadius * radiusFactor));
            
            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance <= currentRadius) {
                        BlockPos blockPos = centerPos.add(dx, y, dz);
                        BlockState state = this.getSpikeBlockState(random, y, height);
                        this.setBlockState(world, blockPos, state);
                    }
                }
            }
        }
        
        // Return the top center position for end crystal placement
        return centerPos.add(0, height, 0);
    }
    
    private BlockState getSpikeBlockState(Random random, int currentY, int totalHeight) {
        double heightRatio = (double) currentY / totalHeight;
        
        // More crying obsidian toward the top
        if (random.nextDouble() < heightRatio * heightRatio * 0.8) {
            return Blocks.CRYING_OBSIDIAN.getDefaultState();
        }
        // Polished variants in middle sections
        else if (random.nextDouble() < 0.4) {
            return random.nextDouble() < 0.5 
                ? ModBlocks.POLISHED_OBSIDIAN.getDefaultState()
                : ModBlocks.POLISHED_OBSIDIAN_BRICKS.getDefaultState();
        }
        // Regular obsidian for the base
        else {
            return Blocks.OBSIDIAN.getDefaultState();
        }
    }
    
    private void setBlockState(ServerWorldAccess world, BlockPos pos, BlockState state) {
        // Only place blocks within world height bounds
        if (pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY()) {
            world.setBlockState(pos, state, 2);
        }
    }
    
    private void placeEndCrystal(ServerWorldAccess world, BlockPos topPos) {
        // Create and spawn the end crystal entity
        EndCrystalEntity crystal = new EndCrystalEntity(world.toServerWorld(), topPos.getX() + 0.5, topPos.getY() + 1.0, topPos.getZ() + 0.5);
        
        // Set the crystal to be beam-generating (like the ones that heal the dragon)
        crystal.setBeamTarget(null); // No specific beam target initially
        crystal.setShowBottom(false); // Hide the base for a cleaner look
        
        // Spawn the crystal in the world
        world.spawnEntity(crystal);
    }

    @WrapOperation(method = "generateSpike", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/gen/feature/EndSpikeFeature;setBlockState(Lnet/minecraft/world/ModifiableWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    public void randomizeObsidian(EndSpikeFeature instance, ModifiableWorld modifiableWorld, BlockPos pos,
                                  BlockState state, Operation<Void> original,
                                  ServerWorldAccess world, Random random, EndSpikeFeatureConfig config,
                                  EndSpikeFeature.Spike spike) {

        // This method will only run if improveEndSpires is false (fallback behavior)
        double crying = (pos.getY() - 70) / (spike.getHeight() - 70d);
        if (state.isOf(Blocks.OBSIDIAN) && ConfigEntries.improveEndSpires) {
            if (Math.random() < crying * crying * crying)
                state = Blocks.CRYING_OBSIDIAN.getDefaultState();
            else if (Math.random() < 0.35)
                state = Math.random() < 0.4 ? ModBlocks.POLISHED_OBSIDIAN.getDefaultState()
                        : ModBlocks.POLISHED_OBSIDIAN_BRICKS.getDefaultState();
        }

        original.call(instance, modifiableWorld, pos, state);
    }
}
