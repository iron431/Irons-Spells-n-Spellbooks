package io.redspace.ironsspellbooks.entity.mobs.wizards;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.goals.WarlockAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackKeyframe;
import io.redspace.ironsspellbooks.network.SyncAnimationPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GenericAnimatedWarlockAttackGoal<T extends PathfinderMob & IAnimatedAttacker & IMagicEntity> extends WarlockAttackGoal {
    public GenericAnimatedWarlockAttackGoal(T abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        nextAttack = randomizeNextAttack(0);
        this.wantsToMelee = true;
        this.mob = abstractSpellCastingMob; //shadows super.mob
    }

    List<AttackAnimationData> moveList = new ArrayList<>();
    protected final T mob;
    protected int meleeAnimTimer = -1;
    public @Nullable AttackAnimationData currentAttack;
    public @Nullable AttackAnimationData nextAttack;
    public @Nullable AttackAnimationData queueCombo;
    float comboChance = .3f;

    @Override
    public boolean isActing() {
        return super.isActing() || isMeleeing();
    }

    public boolean isMeleeing() {
        return meleeAnimTimer > 0;
    }

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        var meleeRange = meleeRange();
        if (meleeAnimTimer < 0 && (!wantsToMelee || distanceSquared > meleeRange * meleeRange || mob.isCasting())) {
            super.handleAttackLogic(distanceSquared);
            return;
        }
        //Handling Animation hit frames
        mob.getLookControl().setLookAt(target);
        if (meleeAnimTimer > 0 && currentAttack != null) {
            //We are currently attacking and are in a melee animation
            forceFaceTarget();
            meleeAnimTimer--;
            if (currentAttack.isHitFrame(meleeAnimTimer)) {
                AttackKeyframe attackData = currentAttack.getHitFrame(meleeAnimTimer);
                onHitFrame(attackData, meleeRange);
            }
            if (currentAttack.canCancel) {
                Vec3 delta = mob.position().subtract(target.position());
                var modifiedDistanceSquared = delta.x * delta.x + delta.y * delta.y * .5 * .5 + delta.z * delta.z;
                if (modifiedDistanceSquared > meleeRange * meleeRange * 1.8 * 1.8) {
                    stopMeleeAction();
                }
            }
        } else if (queueCombo != null && target != null && !target.isDeadOrDying()) {
            nextAttack = queueCombo;
            queueCombo = null;
            doMeleeAction();
        } else if (meleeAnimTimer == 0) {
            //Reset animations/attack
            nextAttack = randomizeNextAttack((float) distanceSquared);
            resetAttackTimer(distanceSquared);
            meleeAnimTimer = -1;
        } else {
            //Handling attack delay
            if (distanceSquared < meleeRange * meleeRange * 1.2 * 1.2) {
                if (hasLineOfSight && --this.attackTime == 0) {
                    doMeleeAction();
                } else if (this.attackTime < 0) {
                    resetAttackTimer(distanceSquared);
                }
            }
        }
    }

    protected void onHitFrame(AttackKeyframe attackKeyframe, float meleeRange) {
        playSwingSound();
        float f = -Utils.getAngle(mob.getX(), mob.getZ(), target.getX(), target.getZ()) - Mth.HALF_PI;
        Vec3 lunge = attackKeyframe.lungeVector().yRot(f);
        mob.push(lunge.x, lunge.y, lunge.z);

        var forward = mob.getForward();
        // if this is an area attack, collect all nearby like-entities, and evaluate a dot product to determine if our area cone can hit them
        var targets = currentAttack.areaAttackThreshold.isEmpty() ?
                List.of(target) :
                mob.level.getEntitiesOfClass(target.getClass(), mob.getBoundingBox().inflate(attackRadius),
                        (entity -> forward.dot(entity.position().subtract(mob.position()).normalize()) >= currentAttack.areaAttackThreshold.get())
                );
        for (LivingEntity target : targets) {
            if (target.distanceToSqr(mob) <= meleeRange * meleeRange && Utils.hasLineOfSight(mob.level, mob, target, true)) {
                handleDamaging(target, attackKeyframe);
            }
        }
    }

    private void handleDamaging(LivingEntity target, AttackKeyframe attackData) {
        boolean flag = this.mob.doHurtTarget(target);
        target.invulnerableTime = 0;
        float f = -Utils.getAngle(mob.getX(), mob.getZ(), target.getX(), target.getZ()) - Mth.HALF_PI;
        if (flag) {
            if (attackData.extraKnockback() != Vec3.ZERO) {
                target.setDeltaMovement(target.getDeltaMovement().add(attackData.extraKnockback().yRot(f)));
            }
            if (currentAttack.isSingleHit() && ((mob.getRandom().nextFloat() < (comboChance * (target.isBlocking() ? 2 : 1))))) {
                //Attack again! combos!
                queueCombo = randomizeNextAttack(0);
            }
        }
    }

    private AttackAnimationData randomizeNextAttack(float distanceSquared) {
        //TODO: IAttackAnimationProvider?
        if (this.moveList.isEmpty()) {
            return null;
        } else {
            return moveList.get(mob.getRandom().nextInt(moveList.size()));
        }
    }

    private void forceFaceTarget() {
        double d0 = target.getX() - mob.getX();
        double d1 = target.getZ() - mob.getZ();
        float yRot = (float) (Mth.atan2(d1, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        mob.setYBodyRot(yRot);
        mob.setYHeadRot(yRot);
        mob.setYRot(yRot);
    }

    public void stopMeleeAction() {
        if (currentAttack != null) {
            meleeAnimTimer = 0;
            PacketDistributor.sendToPlayersTrackingEntity(mob, new SyncAnimationPacket<>("", mob));
        }
    }

    @Override
    protected void doMeleeAction() {
        //anim duration
        currentAttack = nextAttack;
        if (currentAttack != null) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            meleeAnimTimer = currentAttack.lengthInTicks;
            PacketDistributor.sendToPlayersTrackingEntity(mob, new SyncAnimationPacket<>(currentAttack.animationId, mob));
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() || meleeAnimTimer > 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.meleeAnimTimer = -1;
        this.queueCombo = null;
    }

    public void playSwingSound() {
        mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1, Mth.randomBetweenInclusive(mob.getRandom(), 12, 18) * .1f);
    }

    public GenericAnimatedWarlockAttackGoal<T> setMoveset(List<AttackAnimationData> moveset) {
        this.moveList = moveset;
        nextAttack = randomizeNextAttack(0);
        return this;
    }

    public GenericAnimatedWarlockAttackGoal<T> setComboChance(float comboChance) {
        this.comboChance = comboChance;
        return this;
    }
}