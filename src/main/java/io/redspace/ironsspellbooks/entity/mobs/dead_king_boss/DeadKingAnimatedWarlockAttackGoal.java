package io.redspace.ironsspellbooks.entity.mobs.dead_king_boss;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.goals.WarlockAttackGoal;
import io.redspace.ironsspellbooks.network.SyncAnimationPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class DeadKingAnimatedWarlockAttackGoal extends WarlockAttackGoal {
    final DeadKingBoss deadKing;

    public DeadKingAnimatedWarlockAttackGoal(DeadKingBoss abstractSpellCastingMob, double pSpeedModifier, int minAttackInterval, int maxAttackInterval) {
        super(abstractSpellCastingMob, pSpeedModifier, minAttackInterval, maxAttackInterval);
        deadKing = abstractSpellCastingMob;
        nextAttack = randomizeNextAttack(0);
        this.wantsToMelee = true;
    }

    int meleeAnimTimer = -1;
    public DeadKingBoss.AttackType currentAttack;
    public DeadKingBoss.AttackType nextAttack;
    public DeadKingBoss.AttackType queueCombo;

    @Override
    public boolean isActing() {
        return super.isActing() || meleeAnimTimer > 0;
    }

    @Override
    protected void handleAttackLogic(double distanceSquared) {
        var meleeRange = meleeRange();
        if (meleeAnimTimer < 0 && (!wantsToMelee || distanceSquared > meleeRange * meleeRange || spellCastingMob.isCasting())) {
            super.handleAttackLogic(distanceSquared);
            return;
        }
        //Handling Animation hit frames
        mob.getLookControl().setLookAt(target);
        deadKing.isMeleeing = meleeAnimTimer > 0;
        if (meleeAnimTimer > 0) {
            //We are currently attacking and are in a melee animation
            forceFaceTarget();
            meleeAnimTimer--;
            if (currentAttack.data.isHitFrame(meleeAnimTimer - 4)) {
                if (currentAttack == DeadKingBoss.AttackType.SLAM) {
                    mob.playSound(SoundRegistry.DEAD_KING_SLAM.get());
                } else {
                    playSwingSound();
                }
            } else if (currentAttack.data.isHitFrame(meleeAnimTimer)) {
                Vec3 lunge = target.position().subtract(mob.position()).normalize().scale(.35f);
                mob.push(lunge.x, lunge.y, lunge.z);
                if (currentAttack == DeadKingBoss.AttackType.SLAM) {
                    Vec3 slamPos = mob.position().add(mob.getForward().multiply(1, 0, 1).normalize().scale(2.5f));
                    Vec3 bbHalf = new Vec3(meleeRange, meleeRange, meleeRange).scale(.3);
                    float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5f;
                    mob.level.getEntitiesOfClass(LivingEntity.class, new AABB(slamPos.subtract(bbHalf), slamPos.add(bbHalf))).forEach((entity) -> {
                        if (entity.isPickable() && !DamageSources.isFriendlyFireBetween(mob, entity)) {
                            entity.hurt(mob.level().damageSources().mobAttack(mob), damage);
                            var impulse = (slamPos.subtract(entity.position()).add(0, 0.75, 0)).normalize().scale(Mth.lerp(entity.distanceToSqr(mob.position()) / (meleeRange * meleeRange), 2, .5));
                            entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
                            entity.hurtMarked = true;
                            if (entity instanceof Player player && player.isBlocking()) {
                                player.disableShield();
                            }
                        }
                    });
                } else {
                    if (distanceSquared <= meleeRange * meleeRange && Utils.hasLineOfSight(mob.level, mob, target, true)) {
                        boolean flag = this.mob.doHurtTarget(target);
                        target.invulnerableTime = 0;
                        if (flag) {
                            if (currentAttack.data.isSingleHit() && ((mob.getRandom().nextFloat() < .75f) || target.isBlocking())) {
                                //Attack again! combos!
                                queueCombo = randomizeNextAttack(0);
                            }
                        }
                    }
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
                if (--this.attackTime == 0) {
                    doMeleeAction();
                } else if (this.attackTime < 0) {
                    resetAttackTimer(distanceSquared);
                }
            }
        }
    }

    private DeadKingBoss.AttackType randomizeNextAttack(float distanceSquared) {
        return mob.getRandom().nextFloat() < .3f ? DeadKingBoss.AttackType.SLAM : DeadKingBoss.AttackType.DOUBLE_SWING;
        //float chanceToSlam = Math.max(0.8f, .8f + .1f * mob.level.getEntities(mob, mob.getBoundingBox().expandTowards(mob.getForward().scale(2)).inflate(1), (e) -> !DamageSources.isFriendlyFireBetween(mob, e)).size());
        //return chanceToSlam < mob.getRandom().nextFloat() ? DeadKingBoss.AttackType.SLAM : DeadKingBoss.AttackType.DOUBLE_SWING;
    }


    private void forceFaceTarget() {
        double d0 = target.getX() - mob.getX();
        double d1 = target.getZ() - mob.getZ();
        float yRot = (float) (Mth.atan2(d1, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        mob.setYBodyRot(yRot);
        mob.setYHeadRot(yRot);
        mob.setYRot(yRot);
    }

    @Override
    protected void doMeleeAction() {
        //anim duration
        currentAttack = nextAttack;
        if (currentAttack != null) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            meleeAnimTimer = currentAttack.data.lengthInTicks;
            PacketDistributor.sendToPlayersTrackingEntity(deadKing, new SyncAnimationPacket<>(currentAttack.toString(), deadKing));
        }
    }

    @Override
    protected void doMovement(double distanceSquared) {
        var meleeRange = meleeRange();
        if (target.isDeadOrDying()) {
            this.mob.getNavigation().stop();
        } else if (distanceSquared > meleeRange * meleeRange) {
            this.mob.getNavigation().moveTo(this.target, this.speedModifier * 1.3f);
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
        mob.playSound(SoundRegistry.DEAD_KING_SWING.get(), 1, Mth.randomBetweenInclusive(mob.getRandom(), 9, 13) * .1f);
    }

}