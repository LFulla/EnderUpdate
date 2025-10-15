package net.lyof.phantasm.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.lyof.phantasm.config.ConfigEntries;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public class BedBlockMixin {
    
    @ModifyReturnValue(method = "isBedWorking", at = @At("RETURN"))
    private static boolean allowBedsInEnd(boolean original, World world) {
        // If beds should work in the End and we're in the End dimension, return true
        if (ConfigEntries.bedsWorkInEnd && world.getRegistryKey() == World.END) {
            return true;
        }
        return original;
    }
    
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    public void breakBedInEnd(BlockState state, World world, BlockPos pos, PlayerEntity player, 
                             Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        // Only break beds in the End when the config is enabled
        if (ConfigEntries.bedsWorkInEnd && world.getRegistryKey() == World.END) {
            if (!world.isClient) {
                // Break both parts of the bed
                BedPart bedPart = state.get(BedBlock.PART);
                BlockPos otherPos;
                
                if (bedPart == BedPart.HEAD) {
                    // If this is the head, the foot is in the opposite direction
                    otherPos = pos.offset(state.get(BedBlock.FACING).getOpposite());
                } else {
                    // If this is the foot, the head is in the facing direction
                    otherPos = pos.offset(state.get(BedBlock.FACING));
                }
                
                // Drop the bed as an item (only from one part to avoid duplication)
                world.breakBlock(pos, true);
                if (world.getBlockState(otherPos).getBlock() instanceof BedBlock) {
                    world.breakBlock(otherPos, false); // Don't drop twice
                }
            }
            
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}