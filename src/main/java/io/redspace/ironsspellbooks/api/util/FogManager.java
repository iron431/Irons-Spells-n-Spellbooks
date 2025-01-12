package io.redspace.ironsspellbooks.api.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber
public class FogManager {
    private static double interpolation;
    private static final int INTERP_MAX = 40;
    private static FogEvent lastEvent = null;

    public record FogEvent(Optional<Vector3f> color, boolean fullbright) {
    }

    private static final LinkedHashMap<UUID, FogEvent> FOG_EVENTS = new LinkedHashMap<>();

    public static void createEvent(UUID id, FogEvent event) {
        if (FOG_EVENTS.isEmpty()) {
            interpolation = INTERP_MAX;
        }
        FOG_EVENTS.put(id, event);
    }

    public static void stopEvent(UUID id) {
        lastEvent = FOG_EVENTS.remove(id);
        if (FOG_EVENTS.isEmpty()) {
            interpolation = INTERP_MAX;
        }
    }

    public static void clear() {
        FOG_EVENTS.clear();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        if (!FOG_EVENTS.isEmpty() || lastEvent != null) {
            FogEvent fogEvent = FOG_EVENTS.isEmpty() ? lastEvent : FOG_EVENTS.lastEntry().getValue();
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
            if (interpolation > 0) {
                interpolation -= event.getPartialTick();
                float f = Mth.clamp((float) (interpolation / INTERP_MAX), 0, 1);
                if (FOG_EVENTS.isEmpty()) {
                    f = 1 - f;
                }
                event.setRed(Mth.lerp(f, fogRed, event.getRed()));
                event.setGreen(Mth.lerp(f, fogGreen, event.getGreen()));
                event.setBlue(Mth.lerp(f, fogBlue, event.getBlue()));
                if (interpolation <= 0) {
                    lastEvent = null;
                }
            } else {
                event.setRed(fogRed);
                event.setGreen(fogGreen);
                event.setBlue(fogBlue);
                lastEvent = null;
            }
        }
    }
}
