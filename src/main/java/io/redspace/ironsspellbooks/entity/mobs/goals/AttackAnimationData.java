package io.redspace.ironsspellbooks.entity.mobs.goals;

import net.minecraft.world.phys.Vec3;

public class AttackAnimationData {
    //public final int id;
    public final int lengthInTicks;
    public final String animationId;
    public final int[] attackTimestamps;
    public final Vec3 lungeVector;

    public AttackAnimationData(int lengthInTicks, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attackTimestamps = attackTimestamps;
        this.lungeVector = new Vec3(0,0,.45f);
    }

    public AttackAnimationData(int lengthInTicks, Vec3 lungeVector, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attackTimestamps = attackTimestamps;
        this.lungeVector = lungeVector;
    }

    /**
     * Returns for the tick when the animation should deal damage/hit. It is expected tickCount starts at the animation length and decreases
     */
    public boolean isHitFrame(int tickCount) {
        for (int i : attackTimestamps) {
            if (tickCount == lengthInTicks - i) {
                return true;
            }
        }
        return false;
    }

    public boolean isSingleHit() {
        return attackTimestamps.length == 1;
    }
}
