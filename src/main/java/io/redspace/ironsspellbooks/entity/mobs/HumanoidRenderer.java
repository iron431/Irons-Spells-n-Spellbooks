package io.redspace.ironsspellbooks.entity.mobs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import io.redspace.ironsspellbooks.render.ArmorCapeLayer;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;
import software.bernie.geckolib.util.ClientUtil;
import software.bernie.geckolib.util.RenderUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HumanoidRenderer<T extends Mob & GeoAnimatable> extends GeoEntityRenderer<T> {
    private static final String LEFT_HAND = DefaultBipedBoneIdents.LEFT_HAND_BONE_IDENT;
    private static final String RIGHT_HAND = DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT;
    private static final String LEFT_BOOT = DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_IDENT;
    private static final String RIGHT_BOOT = DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_IDENT;
    private static final String LEFT_BOOT_2 = DefaultBipedBoneIdents.LEFT_FOOT_ARMOR_BONE_2_IDENT;
    private static final String RIGHT_BOOT_2 = DefaultBipedBoneIdents.RIGHT_FOOT_ARMOR_BONE_2_IDENT;
    private static final String LEFT_ARMOR_LEG = DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT;
    private static final String RIGHT_ARMOR_LEG = DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT;
    private static final String LEFT_ARMOR_LEG_2 = DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_2_IDENT;
    private static final String RIGHT_ARMOR_LEG_2 = DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_2_IDENT;
    private static final String CHESTPLATE = DefaultBipedBoneIdents.BODY_ARMOR_BONE_IDENT;
    private static final String RIGHT_SLEEVE = DefaultBipedBoneIdents.RIGHT_ARM_ARMOR_BONE_IDENT;
    private static final String LEFT_SLEEVE = DefaultBipedBoneIdents.LEFT_ARM_ARMOR_BONE_IDENT;
    private static final String HELMET = DefaultBipedBoneIdents.HEAD_ARMOR_BONE_IDENT;

    private final RenderLayer hardCodedCapeLayer;

    public HumanoidRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        this.hardCodedCapeLayer = new ArmorCapeLayer(null, (poseStack) -> {
        });
        addRenderLayer(new ItemArmorGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
                // Return the items relevant to the bones being rendered for additional rendering
                return switch (bone.getName()) {
                    case LEFT_BOOT, RIGHT_BOOT, LEFT_BOOT_2, RIGHT_BOOT_2 -> this.bootsStack;
                    case LEFT_ARMOR_LEG, RIGHT_ARMOR_LEG, LEFT_ARMOR_LEG_2, RIGHT_ARMOR_LEG_2 -> this.leggingsStack;
                    case CHESTPLATE, RIGHT_SLEEVE, LEFT_SLEEVE -> this.chestplateStack;
                    case HELMET -> this.helmetStack;
                    default -> null;
                };
            }

            // Return the equipment slot relevant to the bone we're using
            @Nonnull
            @Override
            protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
                return switch (bone.getName()) {
                    case LEFT_BOOT, RIGHT_BOOT, LEFT_BOOT_2, RIGHT_BOOT_2 -> EquipmentSlot.FEET;
                    case LEFT_ARMOR_LEG, RIGHT_ARMOR_LEG, LEFT_ARMOR_LEG_2, RIGHT_ARMOR_LEG_2 -> EquipmentSlot.LEGS;
                    case RIGHT_SLEEVE -> !animatable.isLeftHanded() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    case LEFT_SLEEVE -> animatable.isLeftHanded() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                    case CHESTPLATE -> EquipmentSlot.CHEST;
                    case HELMET -> EquipmentSlot.HEAD;
                    default -> super.getEquipmentSlotForBone(bone, stack, animatable);
                };
            }

            // Return the ModelPart responsible for the armor pieces we want to render
            @Nonnull
            @Override
            protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, HumanoidModel<?> baseModel) {
                return switch (bone.getName()) {
                    case LEFT_BOOT, LEFT_BOOT_2, LEFT_ARMOR_LEG, LEFT_ARMOR_LEG_2 -> baseModel.leftLeg;
                    case RIGHT_BOOT, RIGHT_BOOT_2, RIGHT_ARMOR_LEG, RIGHT_ARMOR_LEG_2 -> baseModel.rightLeg;
                    case RIGHT_SLEEVE -> baseModel.rightArm;
                    case LEFT_SLEEVE -> baseModel.leftArm;
                    case CHESTPLATE -> baseModel.body;
                    case HELMET -> baseModel.head;
                    default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
                };
            }

        });

        // Add some held item rendering
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, T animatable) {
                if (animatable instanceof AbstractSpellCastingMob castingMob) {
                    var boneName = bone.getName();
                    if (isBoneMainHand(castingMob, boneName)) {
                        if (castingMob.isDrinkingPotion()) {
                            return AbstractSpellCastingMobRenderer.makePotion(castingMob);
                        }
                        if (shouldWeaponBeSheathed(castingMob) && castingMob.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof SwordItem) {
                            return ItemStack.EMPTY;
                        }
                    }
                    if (boneName.equals("torso")) {
                        if (shouldWeaponBeSheathed(castingMob) && castingMob.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof SwordItem) {
                            return castingMob.getItemBySlot(EquipmentSlot.MAINHAND);
                        }
                    }
                }
                // Retrieve the items in the entity's hands for the relevant bone
                return switch (bone.getName()) {
                    case LEFT_HAND -> animatable.isLeftHanded() ?
                            animatable.getMainHandItem() : animatable.getOffhandItem();
                    case RIGHT_HAND -> animatable.isLeftHanded() ?
                            animatable.getOffhandItem() : animatable.getMainHandItem();
                    default -> null;
                };
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
                // Apply the camera transform for the given hand
                return switch (bone.getName()) {
                    case RIGHT_HAND, "torso" -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    case LEFT_HAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                    default -> ItemDisplayContext.NONE;
                };
            }

            // Do some quick render modifications depending on what the item is
            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.translate(0, 0, -0.0625);
                poseStack.translate(0, -0.0625, 0);
                boolean offhand = stack == animatable.getOffhandItem();
//                if (stack.getItem() instanceof PotionItem) {
//                    poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
//                }
                if (!offhand) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

                    if (stack.getItem() instanceof ShieldItem)
                        poseStack.translate(0, 0.125, -0.25);
                } else {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

                    if (stack.getItem() instanceof ShieldItem) {
                        poseStack.translate(0, 0.125, 0.25);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    }
                }
                if (animatable instanceof AbstractSpellCastingMob mob && bone.getName().equals("torso")) {
                    if (shouldWeaponBeSheathed(mob)) {
                        float hipOffset = animatable.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? .25f : .325f;
                        poseStack.translate(animatable.isLeftHanded() ? hipOffset : -hipOffset, 0, -.4);

                        poseStack.mulPose(Axis.XP.rotationDegrees(215f));
                        //poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                        poseStack.scale(.85f, .85f, .85f);
                    }
                }
                adjustHandItemRendering(poseStack, stack, animatable, partialTick, offhand);
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    protected boolean isBoneMainHand(AbstractSpellCastingMob entity, String boneName) {
        return entity.isLeftHanded() && boneName.equals(DefaultBipedBoneIdents.LEFT_HAND_BONE_IDENT) || !entity.isLeftHanded() && boneName.equals(DefaultBipedBoneIdents.RIGHT_HAND_BONE_IDENT);
    }

    protected boolean shouldWeaponBeSheathed(AbstractSpellCastingMob entity) {
        return entity.shouldSheathSword() && !entity.isAggressive();
    }

    protected void adjustHandItemRendering(PoseStack poseStack, ItemStack stack, T animatable, float partialTick, boolean offhand) {

    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isInvisible() && entity.isInvisibleTo(ClientUtil.getClientPlayer())) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pushPose();
        float f = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - f));
        if (entity.deathTime > 0) {
            float deathRotation = (entity.deathTime + partialTick - 1f) / 20f * 1.6f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1) * getDeathMaxRotation(entity)));
        }
        model.getBone("torso").ifPresent(bone -> {
            RenderUtil.prepMatrixForBone(poseStack, bone);
        });
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        this.hardCodedCapeLayer.render(poseStack, bufferSource, packedLight, entity, 0, 0, partialTick, 0, 0, 0);
        poseStack.popPose();
    }
}
