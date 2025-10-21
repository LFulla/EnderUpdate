package net.lyof.phantasm.item.custom;


import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;



public class EnderHeartItem extends Item {
    
    public EnderHeartItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.phantasm.ender_heart.tooltip")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
        super.appendTooltip(stack, world, tooltip, context);
    }
    
}
