package net.lyof.phantasm.entity.client.renderer;

import net.lyof.phantasm.Phantasm;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PhantomEntityRenderer;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.Identifier;

public class EndPhantomRenderer extends PhantomEntityRenderer {
    
    private static final Identifier TEXTURE = Phantasm.makeID("textures/entity/end_phantom.png");
    
    public EndPhantomRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public Identifier getTexture(PhantomEntity entity) {
        return TEXTURE;
    }
}