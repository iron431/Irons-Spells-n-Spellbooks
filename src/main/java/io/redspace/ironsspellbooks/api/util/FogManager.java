package io.redspace.ironsspellbooks.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

import java.util.*;

@EventBusSubscriber
public class FogManager {
    private static final int INTERP_MAX = 80;
    private static final Map<ResourceKey<Level>, FogManager> FOG_MANAGERS = new HashMap<>();
    private double interpolation;
    private FogEvent lastEvent = null;
    private final LinkedHashMap<UUID, FogEvent> fogEvents = new LinkedHashMap<>();

    public record FogEvent(Optional<Vector3f> color, boolean fullbright) {
    }

    public static void createEvent(Entity entity, FogEvent event) {
        createEvent(entity.level.dimension(), entity.getUUID(), event);
    }

    public static void createEvent(ResourceKey<Level> dimension, UUID id, FogEvent event) {
        var manager = getManagerFor(dimension);
        if (manager.fogEvents.isEmpty()) {
            manager.interpolation = INTERP_MAX;
        }
        manager.fogEvents.put(id, event);
    }

    public static void stopEvent(UUID id) {
        // while we only create events per-dimension, if something in any dimension calls for a specific uuid to be cancelled, we cancel it
        for (FogManager manager : FOG_MANAGERS.values()) {
            if (manager.fogEvents.containsKey(id)) {
                manager.lastEvent = manager.fogEvents.remove(id);
                if (manager.fogEvents.isEmpty()) {
                    manager.interpolation = INTERP_MAX;
                }
            }
        }
    }

    private static FogManager getManagerFor(ResourceKey<Level> dimension) {
        return FOG_MANAGERS.computeIfAbsent(dimension, (dim) -> new FogManager());
    }

    public static void clear() {
        FOG_MANAGERS.clear();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        if (Minecraft.getInstance().player != null) {
            var manager = getManagerFor(Minecraft.getInstance().player.level.dimension());
            if (!manager.fogEvents.isEmpty() || manager.lastEvent != null) {
                FogEvent fogEvent = manager.fogEvents.isEmpty() ? manager.lastEvent : manager.fogEvents.lastEntry().getValue();
                float fogRed, fogGreen, fogBlue;
                if (fogEvent.color.isPresent()) {
                    fogRed = fogEvent.color.get().x;
                    fogGreen = fogEvent.color.get().y;
                    fogBlue = fogEvent.color.get().z;
                } else {
                    fogRed = event.getRed();
                    fogGreen = event.getGreen();
                    fogBlue = event.getBlue();
                }
                if (fogEvent.fullbright) {
                    float f9 = Math.max(fogRed, Math.max(fogGreen, fogBlue));
                    fogRed /= f9;
                    fogGreen /= f9;
                    fogBlue /= f9;
                }
                if (manager.interpolation > 0) {
                    manager.interpolation -= event.getPartialTick();
                    float f = Mth.clamp((float) (manager.interpolation / INTERP_MAX), 0, 1);
                    if (manager.fogEvents.isEmpty()) {
                        f = 1 - f;
                    }
                    event.setRed(Mth.lerp(f, fogRed, event.getRed()));
                    event.setGreen(Mth.lerp(f, fogGreen, event.getGreen()));
                    event.setBlue(Mth.lerp(f, fogBlue, event.getBlue()));
                    if (manager.interpolation <= 0) {
                        manager.lastEvent = null;
                    }
                } else {
                    event.setRed(fogRed);
                    event.setGreen(fogGreen);
                    event.setBlue(fogBlue);
                    manager.lastEvent = null;
                }
            }
        }
    }
}
