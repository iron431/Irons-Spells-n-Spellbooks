package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireBomb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class BossAbilitiesGoal<T extends Mob & IMagicEntity> extends Goal {
    int abilityTimer;
    int delay;
    boolean isUsing;
    final T mob;

    public BossAbilitiesGoal(T mob) {
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && delay-- <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return isUsing;
    }

    @Override
    public void tick() {
        abilityTimer++;
        if (abilityTimer == 20) {
            for (int i = 0; i < 5; i++) {
                FireBomb fireBomb = new FireBomb(mob.level, mob);
                fireBomb.moveTo(mob.getEyePosition());
                Vec3 motion = Utils.getRandomVec3(1).add(0, 3, 0).scale(0.3);
                fireBomb.setDeltaMovement(motion);
                fireBomb.setDamage(20);
                fireBomb.setAoeDamage(5);
                fireBomb.setExplosionRadius(4);
                mob.level.addFreshEntity(fireBomb);
            }
        }
        if (abilityTimer >= 40) {
            isUsing = false;
        }
    }

    @Override
    public void start() {
        isUsing = true;
        abilityTimer = 0;
        delay = Utils.random.nextIntBetweenInclusive(12, 23) * 20;
    }
}
