package io.redspace.ironsspellbooks.entity.mobs.goals.melee;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class AttackAnimationData {

    //public final int id;
    public final int lengthInTicks;
    public final String animationId;
    public final Int2ObjectOpenHashMap<AttackKeyframe> attacks;
    public final Int2ObjectOpenHashMap<LungeKeyframe> lunges;
    public final boolean canCancel;
    public final Optional<Float> areaAttackThreshold;
    public final float rangeMultiplier;

    public AttackAnimationData(int lengthInTicks, String animationId, int... attackTimestamps) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = new Int2ObjectOpenHashMap<>();
        this.lunges = new Int2ObjectOpenHashMap<>();
        this.canCancel = false;
        for (int i : attackTimestamps) {
            attacks.put(i, new AttackKeyframe(i, new Vec3(0, 0, .45f)/*, Vec2.ONE, 1f*/));
        }
        this.areaAttackThreshold = Optional.empty();
        this.rangeMultiplier = 1f;
    }

    public AttackAnimationData(String animationId, int lengthInTicks, boolean canCancel, Optional<Float> areaAttackThreshold, Int2ObjectOpenHashMap<AttackKeyframe> attacks) {
        this(animationId, lengthInTicks, canCancel, areaAttackThreshold, attacks, new Int2ObjectOpenHashMap<>(), 1f);
    }

    public AttackAnimationData(String animationId, int lengthInTicks, boolean canCancel, Optional<Float> areaAttackThreshold, Int2ObjectOpenHashMap<AttackKeyframe> attacks, Int2ObjectOpenHashMap<LungeKeyframe> lunges, float rangeMultiplier) {
        this.animationId = animationId;
        this.lengthInTicks = lengthInTicks;
        this.attacks = attacks;
        this.lunges = lunges;
        this.canCancel = canCancel;
        this.areaAttackThreshold = areaAttackThreshold;
        this.rangeMultiplier = rangeMultiplier;
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

    /**
     * Returns for the tick when the animation should do additional lunge. It is expected tickCount starts at the animation length and decreases
     */
    public boolean isLungeFrame(int tickCount) {
        return lunges.containsKey(lengthInTicks - tickCount);
    }

    /**
     * Returns for the tick when the animation should do additional lunge. It is expected tickCount starts at the animation length and decreases
     */
    public LungeKeyframe getLungeFrame(int tickCount) {
        return lunges.get(lengthInTicks - tickCount);
    }

    public boolean isSingleHit() {
        return attacks.size() == 1;
    }

    public static Builder builder(String animationId) {
        return new Builder(animationId);
    }

    public static class Builder {
        public int lengthInTicks;
        public String animationId;
        public Int2ObjectOpenHashMap<AttackKeyframe> attacks;
        public Int2ObjectOpenHashMap<LungeKeyframe> lunges;
        public boolean canCancel = false;
        public Optional<Float> areaAttackThreshold = Optional.empty();
        public float rangeMultiplier = 1f;

        public Builder(String animationId) {
            this.animationId = animationId;
        }

        public Builder length(int lengthInTicks) {
            this.lengthInTicks = lengthInTicks;
            return this;
        }

        public Builder cancellable() {
            this.canCancel = true;
            return this;
        }

        public Builder rangeMultiplier(float rangeMultiplier) {
            this.rangeMultiplier = rangeMultiplier;
            return this;
        }

        public Builder area(float threshold) {
            this.areaAttackThreshold = Optional.of(threshold);
            return this;
        }

        public Builder attacks(AttackKeyframe... attacks) {
            this.attacks = new Int2ObjectOpenHashMap<>();
            for (AttackKeyframe a : attacks) {
                this.attacks.put(a.timeStamp(), a);
            }
            return this;
        }

        public Builder lunges(LungeKeyframe... lunges) {
            this.lunges = new Int2ObjectOpenHashMap<>();
            for (LungeKeyframe a : lunges) {
                this.lunges.put(a.timeStamp(), a);
            }
            return this;
        }

        public Builder attacks(int... attackTimestamps) {
            this.attacks = new Int2ObjectOpenHashMap<>();
            for (int i : attackTimestamps) {
                attacks.put(i, new AttackKeyframe(i, new Vec3(0, 0, .45f)));
            }
            return this;
        }

        public AttackAnimationData build() {
            return new AttackAnimationData(animationId, lengthInTicks, canCancel, areaAttackThreshold, attacks, lunges, rangeMultiplier);
        }
    }
}
