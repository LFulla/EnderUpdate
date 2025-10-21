package net.lyof.phantasm.mixin;

import net.lyof.phantasm.config.ConfigEntries;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AreaEffectCloudEntity.class)
public abstract class DragonBreathEntityMixin {

    @Shadow public abstract float getRadius();

    @Inject(method = "tick", at = @At("TAIL"))
    private void addDragonBreathBurnDamage(CallbackInfo ci) {
        AreaEffectCloudEntity self = (AreaEffectCloudEntity) (Object) this;
        
        // Only apply to dragon breath clouds (they use dragon breath particles)
        if (self.getParticleType() == ParticleTypes.DRAGON_BREATH && ConfigEntries.explosiveDragonFireballs) {
            // Get entities within the cloud
            Box boundingBox = self.getBoundingBox().expand(this.getRadius());
            List<LivingEntity> entities = self.getWorld().getEntitiesByClass(LivingEntity.class, boundingBox, entity -> true);
            
            for (LivingEntity entity : entities) {
                // Skip the dragon itself if it's the owner
                if (entity == self.getOwner()) continue;
                
                // Apply fire damage every 10 ticks (0.5 seconds)
                if (self.age % 10 == 0) {
                    // Create fire damage
                    DamageSource fireSource = self.getWorld().getDamageSources().create(DamageTypes.IN_FIRE);
                    entity.damage(fireSource, 2.0f); // 1 heart of fire damage
                    
                    // Set entity on fire for 3 seconds
                    entity.setOnFireFor(3);
                    
                }
            }
        }
    }
}