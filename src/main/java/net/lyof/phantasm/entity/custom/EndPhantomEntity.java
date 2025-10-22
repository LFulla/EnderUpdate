package net.lyof.phantasm.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class EndPhantomEntity extends PhantomEntity {
    
    public EndPhantomEntity(EntityType<? extends PhantomEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public static DefaultAttributeContainer.Builder createEndPhantomAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0) 
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0) 
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.2) // Flying speed
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0); // Large follow range like phantoms
    }
    
    @Override
    protected void initGoals() {
        // Use phantom's default flying AI goals
        super.initGoals();
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // End phantoms don't burn in sunlight (since End has no day/night)
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.IN_FIRE) || 
            source.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE)) {
            return false;
        }
        return super.damage(source, amount);
    }
    
    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        
        // Drop phantom membranes when killed
        if (!this.getWorld().isClient && damageSource.getAttacker() instanceof PlayerEntity) {
            // Drop 1-3 phantom membranes (more generous than regular phantoms)
            int dropCount = 1 + this.random.nextInt(2);
            for (int i = 0; i < dropCount; i++) {
                this.dropItem(Items.PHANTOM_MEMBRANE);
            }
        }
    }
}
