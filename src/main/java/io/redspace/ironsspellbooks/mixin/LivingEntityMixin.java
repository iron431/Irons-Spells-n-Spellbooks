package io.redspace.ironsspellbooks.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.effect.IMobEffectEndCallback;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onEffectRemoved", at = @At(value = "HEAD"))
    public void onEffectRemoved(MobEffectInstance pEffectInstance, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level.isClientSide) {
            if (pEffectInstance.getEffect().value() instanceof IMobEffectEndCallback mobEffect) {
                mobEffect.onEffectRemoved(self, pEffectInstance.getAmplifier());
            }
        }
    }

    @Inject(method = "updateInvisibilityStatus", at = @At(value = "TAIL"))
    public void updateInvisibilityStatus(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(MobEffectRegistry.TRUE_INVISIBILITY))
            self.setInvisible(true);
    }

    @Inject(method = "isCurrentlyGlowing", at = @At(value = "HEAD"), cancellable = true)
    public void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level.isClientSide() && self.hasEffect(MobEffectRegistry.GUIDING_BOLT)) {
            cir.setReturnValue(true);
        }
    }

    @Shadow
    abstract ItemStack getLastHandItem(EquipmentSlot pSlot);

    @Unique
    private static final List<EquipmentSlot> handSlots = List.of(EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND);

    // The equipment change event fires 5 lines too early for this to have been able to be done via events
    @Inject(method = "collectEquipmentChanges", at = @At(value = "RETURN"))
    public void handleEquipmentChanges(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        // Last hand items are accurate at this point
        // Mainhand assigning/removing is handled by minecraft. All we are doing is fudging offhand handling
        // The return of this function is a map of equipmentslots to itemstacks, of itemstacks who have been changed
        var changedEquipment = cir.getReturnValue();
        if (changedEquipment == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack toStack = changedEquipment.get(EquipmentSlot.MAINHAND);
        if (toStack == null) {
            // If this stack was not changed, continue
            return;
        }
        ItemStack fromStack = getLastHandItem(EquipmentSlot.MAINHAND);
        ItemStack offhandStack = self.getOffhandItem();
        //offhand swap
        if (fromStack == offhandStack) {
            return;
        }
        //Do we even care
        if (!offhandStack.isEmpty() && offhandStack.has(ComponentRegistry.MULTIHAND_WEAPON)) {
            //did we equip a multihand item? (hide offhand)
            if (toStack.has(ComponentRegistry.MULTIHAND_WEAPON)) {
                if (!toStack.isEmpty()) {
                    self.getAttributes().removeAttributeModifiers(filterApplicableAttributes(offhandStack.getAttributeModifiers()));
                }
            }
            //did we unequip a multihand item? (reveal offhand)
            else if (fromStack.has(ComponentRegistry.MULTIHAND_WEAPON)) {
                if (!offhandStack.isEmpty()) {
                    self.getAttributes().addTransientAttributeModifiers(filterApplicableAttributes(offhandStack.getAttributeModifiers()));
                }
            }
        }
    }

    @Unique
    private static Multimap<Holder<Attribute>, AttributeModifier> filterApplicableAttributes(ItemAttributeModifiers modifiers) {
        var list = modifiers.modifiers().stream().filter(entry -> entry.slot() == EquipmentSlotGroup.MAINHAND).toList();
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        for (ItemAttributeModifiers.Entry entry : list) {
            var predicate = ServerConfigs.APPLY_ALL_MULTIHAND_ATTRIBUTES.get() ? Utils.NON_BASE_ATTRIBUTES : Utils.ONLY_MAGIC_ATTRIBUTES;
            if (predicate.test(entry.attribute())) {
                map.put(entry.attribute(), entry.modifier());
            }
        }
        return map;
    }

}