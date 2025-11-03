package net.lyof.phantasm.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

public class EndCaveSpiderEntity extends CaveSpiderEntity {
    
    public EndCaveSpiderEntity(EntityType<? extends CaveSpiderEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createEndCaveSpiderAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0) // More health than regular cave spider
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35) // Slightly faster
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0); // More damage
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        // Make the spider half the size of a regular cave spider
        EntityDimensions originalDimensions = super.getDimensions(pose);
        return EntityDimensions.changing(originalDimensions.width * 0.5f, originalDimensions.height * 0.5f);
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        // Call the base spider attack (not cave spider) to avoid inherited poison
        float damage = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float knockback = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
        
        if (target.damage(this.getDamageSources().mobAttack(this), damage)) {
            if (knockback > 0.0F && target instanceof LivingEntity) {
                ((LivingEntity)target).takeKnockback(knockback * 0.5F, 
                    Math.sin(this.getYaw() * 0.017453292F), 
                    -Math.cos(this.getYaw() * 0.017453292F));
                this.setVelocity(this.getVelocity().multiply(0.6, 1.0, 0.6));
            }
            
            if (target instanceof PlayerEntity player) {
                // Apply only levitation effects (no poison)
                if (this.getWorld().getDifficulty() != Difficulty.PEACEFUL) {
                    // Levitation effect to simulate End theme (short duration)
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 0));
                }
                
                // 25% chance to teleport away after attacking
                if (this.random.nextFloat() < 0.25f) {
                    this.teleportRandomly();
                }
            }
            
            this.applyDamageEffects(this, target);
            return true;
        }
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Immune to fall damage
        if (source.isOf(DamageTypes.FALL)) {
            return false;
        }
        
        // 40% chance to teleport when taking damage (like Endermen)
        if (this.random.nextFloat() < 0.4f && !this.getWorld().isClient) {
            this.teleportRandomly();
        }
        return super.damage(source, amount);
    }

    /**
     * Teleports the spider to a random location within 16 blocks
     */
    private boolean teleportRandomly() {
        if (!this.getWorld().isClient && this.isAlive()) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 32.0;
            double y = this.getY() + (this.random.nextInt(16) - 8);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 32.0;
            
            return this.teleportTo(x, y, z);
        }
        return false;
    }

    /**
     * Teleports the spider to a specific location
     */
    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable((int)x, (int)y, (int)z);
        
        // Find a suitable landing spot
        while (mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(0, -1, 0);
        }
        
        BlockPos targetPos = mutable.up();
        if (!this.canTeleportTo(targetPos)) {
            return false;
        }
        
        // Create teleport particles at old position
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 32; i++) {
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * this.getWidth() * 2.0,
                    this.getY() + this.random.nextDouble() * this.getHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5) * this.getWidth() * 2.0,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        
        // Play teleport sound
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
        
        // Teleport
        this.requestTeleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        
        // Create teleport particles at new position
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 32; i++) {
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * this.getWidth() * 2.0,
                    this.getY() + this.random.nextDouble() * this.getHeight(),
                    this.getZ() + (this.random.nextDouble() - 0.5) * this.getWidth() * 2.0,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        
        // Play teleport sound at new position
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
        
        return true;
    }

    /**
     * Checks if the spider can teleport to the given position
     */
    private boolean canTeleportTo(BlockPos pos) {
        // Check if there's enough space (2 blocks high for spider)
        if (!this.getWorld().isSpaceEmpty(this, new Box(pos.getX(), pos.getY(), pos.getZ(), 
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) {
            return false;
        }
        
        // Check if the spider can pathfind to this location
        return this.getWorld().getBlockState(pos.down()).hasSolidTopSurface(this.getWorld(), pos.down(), this);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SPIDER_DEATH;
    }
}