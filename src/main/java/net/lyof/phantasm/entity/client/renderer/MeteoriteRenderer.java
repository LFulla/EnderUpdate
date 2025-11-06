package net.lyof.phantasm.entity.client.renderer;

import net.lyof.phantasm.entity.custom.MeteoriteEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class MeteoriteRenderer extends ProjectileEntityRenderer<MeteoriteEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/block/magma.png");

    public MeteoriteRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(MeteoriteEntity meteorite, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Add spinning rotation based on entity age
        float rotation = (meteorite.getAge() + tickDelta) * 6.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation * 0.7f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation * 0.5f));
        
        // Scale to make it bigger
        matrices.scale(4.0f, 4.0f, 4.0f);
        
        // Render multiple magma blocks using ItemRenderer
        renderMagmaCluster(meteorite, matrices, vertexConsumers, light);
        
        matrices.pop();
        super.render(meteorite, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderMagmaCluster(MeteoriteEntity meteorite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        ItemStack magmaBlock = new ItemStack(Items.MAGMA_BLOCK);
        
        // Render a 3x3x3 cluster of magma blocks using item rendering
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    matrices.push();
                    matrices.translate(x * 0.8f, y * 0.8f, z * 0.8f);
                    
                    // Make each block much larger
                    float sizeVariation = 5.0f + (Math.abs(x + y + z) % 3) * 0.3f;
                    matrices.scale(sizeVariation, sizeVariation, sizeVariation);
                    
                    // Render the magma block as an item
                    itemRenderer.renderItem(magmaBlock, ModelTransformationMode.GROUND, light, light, matrices, vertexConsumers, meteorite.getWorld(), 0);
                    
                    matrices.pop();
                }
            }
        }
    }

    @Override
    public Identifier getTexture(MeteoriteEntity meteoriteEntity) {
        return TEXTURE;
    }
}