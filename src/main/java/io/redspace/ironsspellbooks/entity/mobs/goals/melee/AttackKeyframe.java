package io.redspace.ironsspellbooks.entity.mobs.goals.melee;

import net.minecraft.world.phys.Vec3;

public class AttackKeyframe {
    private final int timeStamp;
    private final Vec3 lungeVector;
    private final Vec3 extraKnockback;

    public AttackKeyframe(int timeStamp, Vec3 lungeVector, Vec3 extraKnockback) {
        this.timeStamp = timeStamp;
        this.lungeVector = lungeVector;
        this.extraKnockback = extraKnockback;
    }

    public AttackKeyframe(int timeStamp, Vec3 lungeVector) {
        this(timeStamp, lungeVector, Vec3.ZERO);
    }

    public int timeStamp() {
        return timeStamp;
    }

    public Vec3 lungeVector() {
        return lungeVector;
    }

    public Vec3 extraKnockback() {
        return extraKnockback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttackKeyframe that = (AttackKeyframe) o;

        if (timeStamp != that.timeStamp) return false;
        if (!lungeVector.equals(that.lungeVector)) return false;
        return extraKnockback.equals(that.extraKnockback);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(timeStamp);
        result = 31 * result + lungeVector.hashCode();
        result = 31 * result + extraKnockback.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AttackKeyframe{" +
                "timeStamp=" + timeStamp +
                ", lungeVector=" + lungeVector +
                ", extraKnockback=" + extraKnockback +
                '}';
    }
}
