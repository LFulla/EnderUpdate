package net.lyof.phantasm.mixin;

import net.lyof.phantasm.Phantasm;
import net.lyof.phantasm.block.ModBlocks;
import net.lyof.phantasm.block.entity.ChallengeRuneBlockEntity;
import net.lyof.phantasm.config.ConfigEntries;
import net.lyof.phantasm.entity.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EndCityGenerator.Piece.class)
public abstract class EndCityGeneratorPieceMixin extends SimpleStructurePiece {
    @Unique private static final Identifier CHALLENGE_ID = Phantasm.makeID("elytra");
    @Unique private static final Identifier STRUCTURE_ID = Identifier.of("minecraft", "end_city/ship");

    @Shadow protected abstract Identifier getId();

    public EndCityGeneratorPieceMixin(StructurePieceType type, int length, StructureTemplateManager structureTemplateManager, Identifier id, String template, StructurePlacementData placementData, BlockPos pos) {
        super(type, length, structureTemplateManager, id, template, placementData, pos);
    }

    @Inject(method = "handleMetadata", at = @At("HEAD"), cancellable = true)
    public void placeElytraChallenge(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox, CallbackInfo ci) {
        if (ConfigEntries.elytraChallenge && metadata.startsWith("Elytra") && this.getId().equals(STRUCTURE_ID)) {
            if (ConfigEntries.elytraChallengeOffset.size() < 3) ConfigEntries.elytraChallengeOffset = List.of(0d, 2d, 8d);

            Direction axis = this.placementData.getRotation().rotate(Direction.SOUTH);
            pos = pos.offset(axis.rotateYClockwise(), ConfigEntries.elytraChallengeOffset.get(0).intValue())
                    .down(ConfigEntries.elytraChallengeOffset.get(1).intValue())
                    .offset(axis, ConfigEntries.elytraChallengeOffset.get(2).intValue());
            world.setBlockState(pos, ModBlocks.CHALLENGE_RUNE.getDefaultState(), Block.NOTIFY_ALL);

            if (world.getBlockEntity(pos) instanceof ChallengeRuneBlockEntity rune) {
                rune.setChallenge(CHALLENGE_ID);
            }

            ci.cancel();
        }
    }

    @Inject(method = "handleMetadata", at = @At("HEAD"))
    public void placeEndCaveSpiderSpawners(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox, CallbackInfo ci) {
        // Place spider spawners in the center of specific End City structures
        String structurePath = this.getId().getPath();
        
        // Check if this is one of the target structures
        if (structurePath.contains("tower_top") || 
            structurePath.contains("third_floor_2") || 
            structurePath.contains("fat_tower_middle") || 
            structurePath.contains("second_floor_2")) {
            
            // Place spawner in the center of the structure when processing the first metadata
                // Calculate center position of the structure
            BlockBox box = this.getBoundingBox();
            BlockPos centerPos = new BlockPos(
                (box.getMinX() + box.getMaxX()) / 2,
                box.getMinY() + 1, // One block above the floor
                (box.getMinZ() + box.getMaxZ()) / 2
            );
            
            // Make sure the position is within bounds and suitable for a spawner
            if (boundingBox.contains(centerPos)) {
                // Clear the space for the spawner
                world.setBlockState(centerPos, Blocks.SPAWNER.getDefaultState(), Block.NOTIFY_ALL);
                
                if (world.getBlockEntity(centerPos) instanceof MobSpawnerBlockEntity spawner) {
                    spawner.setEntityType(ModEntities.END_CAVE_SPIDER, random);
                }
            }
        }
    }
}
