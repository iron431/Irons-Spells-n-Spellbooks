package io.redspace.ironsspellbooks.api.magic;

import io.redspace.ironsspellbooks.api.network.ISerializable;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

//TODO: refactor learned spell data to use INBTSerializable instead of this custom deal
public class LearnedSpellData implements ISerializable {
    public static final String LEARNED_SPELLS = "learnedSpells";

    public final Set<ResourceLocation> learnedSpells = new HashSet<>();

    public void saveToNBT(CompoundTag compound) {
        if (!learnedSpells.isEmpty()) {
            ListTag listTag = new ListTag();
            for (ResourceLocation resourceLocation : learnedSpells) {
                listTag.add(StringTag.valueOf(resourceLocation.toString()));
            }
            compound.put(LEARNED_SPELLS, listTag);
        }
    }

    public void loadFromNBT(CompoundTag compound) {
        ListTag learnedTag = (ListTag) compound.get(LEARNED_SPELLS);
        if (learnedTag != null && !learnedTag.isEmpty()) {
            for (Tag tag : learnedTag) {
                if (tag instanceof StringTag stringTag) {
                    ResourceLocation resourceLocation = ResourceLocation.parse(stringTag.getAsString());
                    if (SpellRegistry.getSpell(resourceLocation) != null) {
                        learnedSpells.add(resourceLocation);
                    }
                }
            }
        }
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeInt(learnedSpells.size());
        for (ResourceLocation resourceLocation : learnedSpells) {
            buf.writeResourceLocation(resourceLocation);
        }
    }

    @Override
    public void readFromBuffer(FriendlyByteBuf buf) {
        int i = buf.readInt();
        if (i > 0) {
            for (int j = 0; j < i; j++) {
                ResourceLocation resourceLocation = buf.readResourceLocation();
                if (SpellRegistry.REGISTRY.get(resourceLocation) != null) {
                    learnedSpells.add(resourceLocation);
                }
            }
        }
    }
}
