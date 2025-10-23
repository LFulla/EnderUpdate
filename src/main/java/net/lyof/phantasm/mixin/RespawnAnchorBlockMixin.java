package net.lyof.phantasm.mixin;

import net.lyof.phantasm.block.ModBlocks;
import net.lyof.phantasm.config.ConfigEntries;
import net.minecraft.block.Block;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {
    
    @Inject(method = "isNether", at = @At("HEAD"), cancellable = true)
    private static void allowEndSpawn(World world, CallbackInfoReturnable<Boolean> cir) {
        // Allow respawn anchors to work in the End dimension as well as Nether
        if (world.getRegistryKey().getValue().toString().equals("minecraft:the_end")) {
            cir.setReturnValue(true);
        }
    }
    
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void handleFallenStarCharging(net.minecraft.block.BlockState state, World world, BlockPos pos, 
                                        PlayerEntity player, Hand hand, BlockHitResult hit, 
                                        CallbackInfoReturnable<ActionResult> cir) {
        
        ItemStack itemStack = player.getStackInHand(hand);
        
        // Check if player is holding a fallen star item (block item)
        if (itemStack.isOf(((Block) ModBlocks.FALLEN_STAR).asItem())) {
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            
            // Only charge if not at max charges (4)
            if (charges < 4) {
                if (!world.isClient) {
                    // Consume the fallen star
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                    
                    // Add one charge to the respawn anchor
                    world.setBlockState(pos, state.with(RespawnAnchorBlock.CHARGES, charges + 1), 3);
                    
                    // Play the charging sound (same as glowstone)
                    world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 
                                  net.minecraft.sound.SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                
                cir.setReturnValue(ActionResult.SUCCESS);
            } else {
                // Anchor is already fully charged
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }
}