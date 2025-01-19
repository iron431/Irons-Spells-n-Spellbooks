package io.redspace.ironsspellbooks.entity.mobs.goals.melee;

import net.minecraft.world.phys.Vec3;

public class LungeKeyframe {
    private final int timeStamp;
    private final Vec3 lungeVector;

    public LungeKeyframe(int timeStamp, Vec3 lungeVector) {
        this.timeStamp = timeStamp;
        this.lungeVector = lungeVector;
    }

    public int timeStamp() {
        return timeStamp;
    }

    public Vec3 lungeVector() {
        return lungeVector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LungeKeyframe that = (LungeKeyframe) o;

        if (timeStamp != that.timeStamp) return false;
        if (!lungeVector.equals(that.lungeVector)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(timeStamp);
        result = 31 * result + lungeVector.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AttackKeyframe{" +
                "timeStamp=" + timeStamp +
                ", lungeVector=" + lungeVector +
                '}';
    }
}
