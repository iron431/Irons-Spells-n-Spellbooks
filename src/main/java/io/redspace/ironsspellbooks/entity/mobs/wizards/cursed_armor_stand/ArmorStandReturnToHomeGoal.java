package io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ArmorStandReturnToHomeGoal extends WaterAvoidingRandomStrollGoal {
    CursedArmorStandEntity mob;

    int stuckTimer;
    private static final int MAX_INTERVAL = 10;
    private static final float CLOSE_DISTANCE = 2;
    boolean closingFinalDistance;

    Vec3 lastStuckPos = Vec3.ZERO;
    int stuckCounter;

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
        if (mob.tickCount % 200 == 0) {
            var currpos = mob.position();
            var distance = lastStuckPos.distanceToSqr(currpos);
            if (distance < 5 * 5) {
                // we seem to be stuck
                stuckCounter++;
            } else {
                // we are not stuck
                lastStuckPos = currpos;
                stuckCounter = 0;
            }
            if (stuckCounter > 2) {
                // if we are stuck for too many iterations, give up
                mob.spawn = mob.position();
                stop();
                return;
            }
        }
        if (closingFinalDistance && mob.getNavigation().isDone() && mob.spawn != null) {
            // the path can only get up to our spawn within a margin of 1 block
            // once we cross the CLOSE_DISTANCE threshold, just nudge ourselves over until we are happy
            Vec3 delta = mob.spawn.subtract(mob.position());
            var currDistance = delta.lengthSqr();

            double d0 = mob.spawn.x - mob.getX();
            double d1 = mob.spawn.z - mob.getZ();
            float f = (float) (Mth.atan2(d1, d0) * 180.0F / (float) Math.PI) - 90.0F;
            mob.setYRot(f);
            mob.setYBodyRot(f);

            mob.getMoveControl().strafe((float) speedModifier, 0);
            if (currDistance > CLOSE_DISTANCE * CLOSE_DISTANCE) {
                closingFinalDistance = false;
            } else if (currDistance < ATHRESHOLD_SQR) {
                stop();
            }
        } else {
            super.tick();
        }
    }

    @Override
    public void start() {
        super.start();
        //force end strafe
        mob.setXxa(0);
        //reset stuck counting
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
