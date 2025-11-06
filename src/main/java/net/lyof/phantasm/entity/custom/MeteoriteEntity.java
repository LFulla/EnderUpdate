package net.lyof.phantasm.entity.custom;

import net.lyof.phantasm.block.ModBlocks;
import net.lyof.phantasm.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MeteoriteEntity extends ArrowEntity {
    private int age = 0;

    public MeteoriteEntity(EntityType<? extends ArrowEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
    }

    public int getAge() {
        return this.age;
    }

    public static MeteoriteEntity create(World world, double x, double y, double z) {
        MeteoriteEntity meteorite = new MeteoriteEntity(ModEntities.METEORITE, world);
        meteorite.setPosition(x, y, z);
        
        // Set random falling angle and velocity - much more dramatic angles
        double speed = 2.0 + world.random.nextDouble() * 1.0; // Speed between 2.0 and 3.0
        double angleX = (world.random.nextDouble() - 0.5) * 2.5; // Much wider angle range
        double angleZ = (world.random.nextDouble() - 0.5) * 2.5; // Much wider angle range
        double velocityY = -speed; // Always falling down
        
        meteorite.setVelocity(angleX, velocityY, angleZ);
        return meteorite;
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        // Add particle trail
        if (this.getWorld().isClient) {
            for (int i = 0; i < 3; i++) {
                this.getWorld().addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        0, 0, 0);
            }
            
            this.getWorld().addParticle(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2,
                    (this.random.nextDouble() - 0.5) * 0.2,
                    (this.random.nextDouble() - 0.5) * 0.2);
        }

        // Apply gravity
        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.x, velocity.y - 0.05, velocity.z);

        // Remove after 5 minutes if not hit anything
        if (this.age > 6000) {
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient) {
            this.explode();
        }
        super.onBlockHit(blockHitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (!this.getWorld().isClient) {
            this.explode();
        }
        super.onEntityHit(entityHitResult);
    }

    private void explode() {
        if (this.getWorld().isClient) return;

        BlockPos pos = this.getBlockPos();
        
        // Create massive explosion that ignores blast resistance
        this.getWorld().createExplosion(this, this.getX(), this.getY() - 1, this.getZ(), 
                100.0f, World.ExplosionSourceType.TNT);
        
        this.getWorld().createExplosion(this, this.getX(), this.getY() - 4, this.getZ(), 
                100.0f, World.ExplosionSourceType.TNT);
        

        // Play explosion sound
        this.getWorld().playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, 
                SoundCategory.HOSTILE, 4.0f, 0.8f + this.random.nextFloat() * 0.4f);

        // Drop meteorite shards closer to ground level
        int shardCount = 3 + this.random.nextInt(6); // 3-8 shards
        for (int i = 0; i < shardCount; i++) {
            BlockPos shardPos = pos.add(
                    this.random.nextInt(7) - 3,
                    this.random.nextInt(2) - 3, // Much lower Y range (-1 to 0)
                    this.random.nextInt(7) - 3
            );
            
            if (this.getWorld().getBlockState(shardPos).isReplaceable()) {
                this.getWorld().setBlockState(shardPos, ModBlocks.METEORITE.getDefaultState());
            }
        }

        this.discard();
    }

    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY; // Meteorites don't become items when picked up
    }

    @Override
    public boolean canHit() {
        return true;
    }
}