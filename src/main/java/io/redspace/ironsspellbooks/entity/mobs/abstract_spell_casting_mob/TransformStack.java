package io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob;


import net.minecraft.util.Mth;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.*;

public class TransformStack {
    private final Map<GeoBone, Stack<Vector3f>> positionStack = new HashMap<>();
    private final Map<GeoBone, Stack<Vector3f>> rotationStack = new HashMap<>();
    private final Set<GeoBone> toReset = new HashSet<>();

    public void pushPosition(GeoBone bone, Vector3f appendVec) {
        var stack = positionStack.getOrDefault(bone, new Stack<>());
        stack.push(appendVec);
        positionStack.put(bone, stack);
    }

    public void resetDirty() {
        toReset.forEach(bone -> {
            var snapshot = bone.getInitialSnapshot();
            bone.updatePosition(snapshot.getOffsetX(), snapshot.getOffsetY(), snapshot.getOffsetZ());
            bone.updateRotation(snapshot.getRotX(), snapshot.getRotY(), snapshot.getRotZ());
            bone.resetStateChanges();
        });
        toReset.clear();
    }

    public void pushPosition(GeoBone bone, float x, float y, float z) {
        pushPosition(bone, new Vector3f(x, y, z));
    }

    public void pushRotation(GeoBone bone, Vector3f appendVec) {
        var stack = rotationStack.getOrDefault(bone, new Stack<>());
        stack.push(appendVec);
        rotationStack.put(bone, stack);
    }

    public void pushRotation(GeoBone bone, float x, float y, float z) {
        pushRotation(bone, new Vector3f(x, y, z));
    }

    public void pushRotationDegrees(GeoBone bone, float x, float y, float z) {
        pushRotation(bone, new Vector3f(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD));
    }

    public void popStack() {
        positionStack.forEach((bone, stack) -> {
            toReset.add(bone);
            Vector3f position = bone.getPositionVector().get(new Vector3f());
            stack.forEach(position::add);
            setPosImpl(bone, position);
        });
        rotationStack.forEach((bone, stack) -> {
            toReset.add(bone);
            Vector3f rotation = bone.getRotationVector().get(new Vector3f(0, 0, 0));
            stack.forEach(rotation::add);
            setRotImpl(bone, rotation);
        });
        positionStack.clear();
        rotationStack.clear();
    }

    public void setRotImpl(GeoBone bone, Vector3f vector3f) {
        bone.updateRotation(
                wrapRadians(vector3f.x()),
                wrapRadians(vector3f.y()),
                wrapRadians(vector3f.z()));
    }

    public void setPosImpl(GeoBone bone, Vector3f vector3f) {
        bone.updatePosition(vector3f.x, vector3f.y, vector3f.z);
    }

    public static float wrapRadians(float pValue) {
        float twoPi = 6.2831f;
        float pi = 3.14155f;
        float f = pValue % twoPi;
        if (f >= pi) {
            f -= twoPi;
        }

        if (f < -pi) {
            f += twoPi;
        }

        return f;
    }
}
