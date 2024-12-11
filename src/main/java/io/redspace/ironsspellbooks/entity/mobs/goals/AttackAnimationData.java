package io.redspace.ironsspellbooks.entity.mobs.goals;

public class AttackAnimationData {
    //public final int id;
    public final int lengthInTicks;
    public final String animationId;
    public final int[] attackTimestamps;
    public final float lungeStrength;

    public AttackAnimationData(int lengthInTicks, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attackTimestamps = attackTimestamps;
        this.lungeStrength = .45f;
    }

    public AttackAnimationData(int lengthInTicks, float lungeStrength, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attackTimestamps = attackTimestamps;
        this.lungeStrength = lungeStrength;
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
