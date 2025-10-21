package net.lyof.phantasm.item.custom;


import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import java.util.List;



public class ElytraCoreItem extends Item {
    
    public ElytraCoreItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.phantasm.elytra_core.tooltip")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
        super.appendTooltip(stack, world, tooltip, context);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Makes the item have an enchanted glint
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // Apply slow falling effect when held in main hand or offhand
        if (!world.isClient && entity instanceof PlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            
            // Check if player is holding elytra core in either hand
            if (mainHand.getItem() == this || offHand.getItem() == this) {
                // Apply slow falling effect (duration: 2 seconds, amplifier: 0, ambient: false, show particles: false)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 5, 0, false, false));
            }
        }
    }
}
