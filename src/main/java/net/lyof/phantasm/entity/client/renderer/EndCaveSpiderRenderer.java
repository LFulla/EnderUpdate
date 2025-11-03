package net.lyof.phantasm.entity.client.renderer;

import net.lyof.phantasm.Phantasm;
import net.lyof.phantasm.entity.custom.EndCaveSpiderEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SpiderEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class EndCaveSpiderRenderer extends SpiderEntityRenderer<EndCaveSpiderEntity> {
    private static final Identifier TEXTURE = Phantasm.makeID("textures/entity/end_cave_spider.png");

    public EndCaveSpiderRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.4f; // Much smaller shadow to match the smaller spider
    }

    @Override
    protected void scale(EndCaveSpiderEntity entity, MatrixStack matrices, float amount) {
        // Scale the spider model to half size to match the hitbox
        matrices.scale(0.5f, 0.5f, 0.5f);
    }

    @Override
    public Identifier getTexture(EndCaveSpiderEntity entity) {
        return TEXTURE;
    }
}