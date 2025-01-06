package io.redspace.ironsspellbooks.api.events;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class CounterSpellEvent extends Event implements ICancellableEvent {
    public final Entity caster;
    public final Entity target;

    public CounterSpellEvent(Entity caster, Entity target){
        this.caster = caster;
        this.target = target;
    }
}
