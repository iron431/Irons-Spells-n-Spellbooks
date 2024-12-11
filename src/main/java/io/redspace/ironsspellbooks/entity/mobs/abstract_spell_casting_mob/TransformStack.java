package io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob;


import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Map;

public class TransformStack {
    private final Map<GeoBone, Vector3f> positionStack = new HashMap<>();
    private final Map<GeoBone, Vector3f> rotationStack = new HashMap<>();
    private boolean needsReset;

    public void pushPosition(GeoBone bone, Vector3f appendVec) {
        var vec = positionStack.getOrDefault(bone, new Vector3f(0, 0, 0));
        vec.add(appendVec);
        positionStack.put(bone, vec);
    }

    public void pushPosition(GeoBone bone, float x, float y, float z) {
        pushPosition(bone, new Vector3f(x, y, z));
    }

    public void overridePosition(GeoBone bone, Vector3f newVec) {
        positionStack.put(bone, newVec);
    }

    public void pushRotation(GeoBone bone, Vector3f appendVec) {
        var vec = rotationStack.getOrDefault(bone, new Vector3f(0, 0, 0));
        vec.add(appendVec);
        rotationStack.put(bone, vec);
    }

    public void pushRotation(GeoBone bone, float x, float y, float z) {
        pushRotation(bone, new Vector3f(x, y, z));
    }

    public void pushRotationWithBase(GeoBone bone, float x, float y, float z) {
        var base = new Vector3f(bone.getRotX(), bone.getRotY(), bone.getRotZ());
        base.add(x, y, z);
        // fixme: seems like 1.20 works differently with this
        pushRotation(bone, x, y, z);
    }

    public void overrideRotation(GeoBone bone, Vector3f newVec) {
        rotationStack.put(bone, newVec);
    }

    public void popStack() {
        positionStack.forEach(this::setPosImpl);
        rotationStack.forEach(this::setRotImpl);
        positionStack.clear();
        rotationStack.clear();
    }

    public void setRotImpl(GeoBone bone, Vector3f vector3f) {
        bone.getInitialSnapshot().updateRotation(
                wrapRadians(vector3f.x()),
                wrapRadians(vector3f.y()),
                wrapRadians(vector3f.z()));
    }

    public void setPosImpl(GeoBone bone, Vector3f vector3f) {
        bone.getInitialSnapshot().updateOffset(vector3f.x, vector3f.y, vector3f.z);
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
