package io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob;


import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class TransformStack {
    private final Map<GeoBone, Stack<Vector3f>> positionStack = new HashMap<>();
    private final Map<GeoBone, Stack<Vector3f>> rotationStack = new HashMap<>();
    private boolean needsReset;

    public Stack<Vector3f> create(Vector3f defaultValue) {
        var stack = new Stack<Vector3f>();
        stack.push(defaultValue);
        return stack;
    }

    public void pushPosition(GeoBone bone, Vector3f appendVec) {
        var stack = positionStack.getOrDefault(bone, create(bone.getPositionVector().get(new Vector3f())));
        stack.push(appendVec);
        positionStack.put(bone, stack);
    }

    public void pushPosition(GeoBone bone, float x, float y, float z) {
        pushPosition(bone, new Vector3f(x, y, z));
    }

    public void pushRotation(GeoBone bone, Vector3f appendVec) {
        var stack = rotationStack.getOrDefault(bone, create(bone.getRotationVector().get(new Vector3f())));
        stack.push(appendVec);
        rotationStack.put(bone, stack);
    }

    public void pushRotation(GeoBone bone, float x, float y, float z) {
        pushRotation(bone, new Vector3f(x, y, z));
    }

    public void popStack(AnimationState<?> animationState) {
        var bonesBeingAnimated = animationState.getController().getBoneAnimationQueues().keySet();
        positionStack.forEach((bone, stack) -> {
            if (!bonesBeingAnimated.contains(bone.getName())) {
                stack.removeFirst();
            }
            Vector3f position = new Vector3f(0, 0, 0);
            stack.forEach(position::add);
            setPosImpl(bone, position);
        });
        rotationStack.forEach((bone, stack) -> {
            if (!bonesBeingAnimated.contains(bone.getName())) {
                stack.removeFirst();
            }
            Vector3f rotation = new Vector3f(0, 0, 0);
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
