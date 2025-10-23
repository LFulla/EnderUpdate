package net.lyof.phantasm.mixin;

import net.lyof.phantasm.config.ConfigEntries;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.boss.dragon.phase.StrafePlayerPhase;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonEntity.class)
public class EnderDragonEntityMixin {
    
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        if (!ConfigEntries.improveEndSpires) return;
        
        EnderDragonEntity dragon = (EnderDragonEntity) (Object) this;
        
        // Search for flying players and prioritize them as targets
        PlayerEntity flyingPlayer = findFlyingPlayer(dragon);
        if (flyingPlayer != null) {
            // Set flying player as the dragon's target
            dragon.setTarget(flyingPlayer);
        }
        
        // Only modify behavior when dragon has a target
        LivingEntity livingTarget = dragon.getTarget();
        if (livingTarget == null) return;
        
        // Check if target is a player and above Y level 74
        if (livingTarget instanceof PlayerEntity) {
            PlayerEntity target = (PlayerEntity) livingTarget;
            if (target.getY() > 74.0) {
                // Only switch to aggressive phase if not already in one
                if (dragon.getPhaseManager().getCurrent().getType() != PhaseType.CHARGING_PLAYER &&
                    dragon.getPhaseManager().getCurrent().getType() != PhaseType.STRAFE_PLAYER) {
                    
                    // 50/50 chance to either charge or strafe
                    if (Math.random() < 0.5) {
                        // CHARGE: Set charging phase with predictive targeting
                        Vec3d predictedPos = getPredictedPlayerPosition(dragon, target);
                        dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
                        ChargingPlayerPhase chargingPhase = (ChargingPlayerPhase) dragon.getPhaseManager().getCurrent();
                        chargingPhase.setPathTarget(predictedPos);
                    } else {
                        // STRAFE: Set strafe phase and set target entity  
                        dragon.getPhaseManager().setPhase(PhaseType.STRAFE_PLAYER);
                        StrafePlayerPhase strafePhase = (StrafePlayerPhase) dragon.getPhaseManager().getCurrent();
                        strafePhase.setTargetEntity(target);
                    }
                }
            }
        }
    }
    
    private PlayerEntity findFlyingPlayer(EnderDragonEntity dragon) {
        // Search for players within range who are flying/high up
        return dragon.getWorld().getPlayers().stream()
            .filter(player -> !player.isSpectator() && !player.isCreative())
            .filter(player -> dragon.squaredDistanceTo(player) < 80.0 * 80.0) // Within 80 blocks
            .filter(player -> player.getY() > 74.0) // High altitude
            .filter(player -> player.isFallFlying() || player.getAbilities().flying || player.getVelocity().y > 0.1) // Actually flying
            .min((p1, p2) -> Double.compare(dragon.squaredDistanceTo(p1), dragon.squaredDistanceTo(p2))) // Closest first
            .orElse(null);
    }
    
    private Vec3d getPredictedPlayerPosition(EnderDragonEntity dragon, PlayerEntity target) {
        Vec3d currentPos = target.getPos();
        Vec3d velocity = target.getVelocity();
        
        // Calculate time for dragon to reach target (rough estimation)
        double distance = dragon.getPos().distanceTo(currentPos);
        double dragonSpeed = 1.0; // Approximate dragon charging speed
        double timeToReach = distance / dragonSpeed;
        
        // Predict where the player will be
        Vec3d predictedPos = currentPos.add(velocity.multiply(timeToReach));
        
        // Clamp prediction to reasonable bounds to prevent targeting too far away
        double maxPredictionDistance = 30.0; // Max 30 blocks ahead
        Vec3d predictedOffset = predictedPos.subtract(currentPos);
        if (predictedOffset.length() > maxPredictionDistance) {
            predictedOffset = predictedOffset.normalize().multiply(maxPredictionDistance);
            predictedPos = currentPos.add(predictedOffset);
        }
        
        return predictedPos;
    }
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventFireballSelfDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigEntries.improveEndSpires) return;
        
        EnderDragonEntity dragon = (EnderDragonEntity) (Object) this;
        
        // Cancel damage if it's from the dragon's own fireball (direct hit)
        if (source.getSource() instanceof DragonFireballEntity) {
            DragonFireballEntity fireball = (DragonFireballEntity) source.getSource();
            
            // Check if this fireball was shot by this dragon
            if (fireball.getOwner() == dragon) {
                cir.setReturnValue(false); // Return false to indicate no damage was taken
                return;
            }
        }
        
        // Cancel damage if it's any explosion damage (dragons should be immune to explosions)
        // This catches explosion damage that might not preserve the fireball source properly
        if (source.getName().equals("explosion.player") || 
            source.getName().equals("explosion") || 
            source.getName().contains("explosion")) {
            cir.setReturnValue(false); // Dragons are immune to all explosion damage
            return;
        }
        
        // Also check for fireball-related damage types
        if (source.getName().contains("fireball") || source.getName().contains("dragon")) {
            cir.setReturnValue(false); // Dragons immune to fireball/dragon damage
        }
    }
}
