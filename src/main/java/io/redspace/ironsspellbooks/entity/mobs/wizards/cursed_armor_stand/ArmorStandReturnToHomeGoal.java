package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ArmorStandReturnToHomeGoal extends WaterAvoidingRandomStrollGoal {
    CursedArmorStandEntity mob;

    int stuckTimer;
    private static final int MAX_INTERVAL = 10;
    private static final float CLOSE_DISTANCE = 0.75f;
    boolean closingFinalDistance;

    public ArmorStandReturnToHomeGoal(CursedArmorStandEntity pMob, double pSpeedModifier) {
        super(pMob, pSpeedModifier);
        this.mob = pMob;
        interval = MAX_INTERVAL;
    }

    private static final double ARRIVED_THRESHOLD = .1;
    private static final double ATHRESHOLD_SQR = ARRIVED_THRESHOLD * ARRIVED_THRESHOLD;

    @Override
    public boolean canUse() {
        if (this.mob.hasControllingPassenger()) {
            return false;
        } else if (mob.isArmorStandFrozen()) {
            return false;
        } else {
            Vec3 vec3 = this.getPosition();
            if (vec3 == null) {
                return false;
            } else {
                this.wantedX = vec3.x;
                this.wantedY = vec3.y;
                this.wantedZ = vec3.z;
                this.forceTrigger = false;
                return true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.hasControllingPassenger()) {
            return false; // controlling passenger interruption
        } else if (mob.getNavigation().isDone()) {
            var distance = homeDistanceSqr();
            if (distance <= ATHRESHOLD_SQR) {
                return false; //done
            } else if (distance <= CLOSE_DISTANCE * CLOSE_DISTANCE) {
                closingFinalDistance = true;
                return true; // not quite done, trigger landing sequence
            } else {
                return false; // unknown state, effectively return super logic
            }
        } else {
            return true; // path not done, continue
        }
    }

    @Override
    public void tick() {
        if (closingFinalDistance && mob.spawn != null) {
            Vec3 delta = mob.spawn.subtract(mob.position());
            var currDistance = delta.lengthSqr();
            if (currDistance > CLOSE_DISTANCE * CLOSE_DISTANCE) {
                closingFinalDistance = false;
            } else if (currDistance < ATHRESHOLD_SQR) {
                stop();
            } else {
                mob.setDeltaMovement(mob.getDeltaMovement().add(delta.normalize().scale(0.05)));
            }
        } else {
            super.tick();
            // every 5 seconds, update the interval to more intensely try to find our home
            // if we cannot get to our home in a great matter of time, sethome to our current position and freeze
            if (stuckTimer++ > 20 * 5) {
                interval--;
                if (interval == 0) {
                    mob.spawn = mob.position();
                    stop();
                }
                stuckTimer = 0;
            }
        }
    }

    @Override
    public void start() {
        super.start();
        this.stuckTimer = 0;
        this.interval = MAX_INTERVAL;
    }

    @Override
    public void stop() {
        super.stop();
        closingFinalDistance = false;
        if (homeDistanceSqr() <= ATHRESHOLD_SQR) {
            mob.setArmorStandFrozen(true);
        }
    }

    private double homeDistanceSqr() {
        return mob.spawn == null ? 0 : mob.distanceToSqr(mob.spawn);
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        return mob.spawn;
    }
}
