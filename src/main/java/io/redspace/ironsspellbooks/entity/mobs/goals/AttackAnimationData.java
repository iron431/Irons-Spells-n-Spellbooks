package io.redspace.ironsspellbooks.entity.mobs.goals;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class AttackAnimationData {
    public record AttackKeyframe(int timeStamp, Vec3 lungeVector,
                                 Vec3 extraKnockback/*, Vec2 knockbackMultipliers, float damageMultiplier*/) {
        public AttackKeyframe(int timeStamp, Vec3 lungeVector) {
            this(timeStamp, lungeVector, Vec3.ZERO);
        }
    }

    //public final int id;
    public final int lengthInTicks;
    public final String animationId;
    public final Int2ObjectOpenHashMap<AttackKeyframe> attacks;
    public final boolean canCancel;
    public final Optional<Float> areaAttackThreshold;

    public AttackAnimationData(int lengthInTicks, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = new Int2ObjectOpenHashMap<>();
        this.canCancel = false;
        for (int i : attackTimestamps) {
            attacks.put(i, new AttackKeyframe(i, new Vec3(0, 0, .45f)/*, Vec2.ONE, 1f*/));
        }
        this.areaAttackThreshold = Optional.empty();
    }

    public AttackAnimationData(int lengthInTicks, String animationId, AttackKeyframe... attacks) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = new Int2ObjectOpenHashMap<>();
        this.canCancel = false;
        for (AttackKeyframe a : attacks) {
            this.attacks.put(a.timeStamp, a);
        }
        this.areaAttackThreshold = Optional.empty();
    }

    public AttackAnimationData(boolean canCancel, int lengthInTicks, String animationId, AttackKeyframe... attacks) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = new Int2ObjectOpenHashMap<>();
        this.canCancel = canCancel;
        for (AttackKeyframe a : attacks) {
            this.attacks.put(a.timeStamp, a);
        }
        this.areaAttackThreshold = Optional.empty();
    }

    public AttackAnimationData(boolean canCancel, float areaAttackThreshold, int lengthInTicks, String animationId, AttackKeyframe... attacks) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = new Int2ObjectOpenHashMap<>();
        this.canCancel = canCancel;
        for (AttackKeyframe a : attacks) {
            this.attacks.put(a.timeStamp, a);
        }
        this.areaAttackThreshold = Optional.of(areaAttackThreshold);
    }

    /**
     * Returns for the tick when the animation should deal damage/hit. It is expected tickCount starts at the animation length and decreases
     */
    public boolean isHitFrame(int tickCount) {
        return attacks.containsKey(lengthInTicks - tickCount);
    }
    /**
     * Returns for the tick when the animation should deal damage/hit. It is expected tickCount starts at the animation length and decreases
     */
    public AttackKeyframe getHitFrame(int tickCount) {
        return attacks.get(lengthInTicks - tickCount);
    }

    public boolean isSingleHit() {
        return attacks.size() == 1;
    }
}
