package io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;

public class NotIdioticNavigation extends GroundPathNavigation {
    public NotIdioticNavigation(Mob pMob, Level pLevel) {
        super(pMob, pLevel);
    }

    private static final boolean debugparticles = false;

    @Override
    protected void trimPath() {
        super.trimPath();
        //try to optimize out excessive direction jitter by eliminating close-angled nodes, or intermediary nodes when we have a direct line of sight (path)
        ArrayList<Node> dumbNodes = new ArrayList<Node>();

        if (path == null || path.getNextNodeIndex() >= path.nodes.size()) {
            return;
        }
        try {
            var lastImportantNode = path.getNextNode().asVec3();
            var finalNode = path.getEndNode().asVec3();
            if (Math.abs(lastImportantNode.y - finalNode.y) <= 2 && level.clip(new ClipContext(lastImportantNode.add(0, 0.75, 0), finalNode.add(0, 0.75, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS
                    && isTraversable(lastImportantNode, finalNode)) {
                // if we have direct line of sight from our current position to our target destination (and there is not a large vertical changes between them), ignore all intermediary nodes and just walk towards our destination
                for (int i = path.getNextNodeIndex() + 1; i < path.nodes.size() - 1; i++) {
                    dumbNodes.add(path.getNode(i));
                }
            } else {
                // evaluate each node to see if it actually meaningfully changes our pathing direction
                // if it is mostly collinear with the last important node, remove it to simplify and smooth the path
                // however, do not do such if the last anchor is not traversable to the current node (pitfalls, fire blocks, etc)
                for (int i = path.getNextNodeIndex() + 2; i < path.nodes.size(); i++) {
                    var node1 = path.getNode(i - 1).asVec3();
                    var node2 = path.getNode(i).asVec3();
                    var delta1 = node1.subtract(lastImportantNode).multiply(1, 3, 1).normalize(); // scale vertically in order to exacerbate vertical changes
                    var delta2 = node2.subtract(node1).multiply(1, 3, 1).normalize();
                    if (delta1.dot(delta2) > .88 && isTraversable(lastImportantNode, node2)) {
                        dumbNodes.add(path.getNode(i - 1));
                    } else {
                        lastImportantNode = node1;
                    }
                }
            }
            if (debugparticles) {
                for (Node node : dumbNodes) {
                    MagicManager.spawnParticles(level, ParticleHelper.FIRE_EMITTER, node.x, node.y + .1, node.z, 2, 0, 0, 0, 0.0, true);
                }
            }
            path.nodes.removeAll(dumbNodes);
        } catch (Exception e) {
            IronsSpellbooks.LOGGER.error(e.getMessage());
            //cancel navigation
            this.path = null;
        }
    }

    private boolean isTraversable(Vec3 pos1, Vec3 pos2) {
        Vec3 step = pos2.subtract(pos1);
        double distance = step.length();
        step = step.scale(1 / distance); // normalize
        for (int i = 0; i < distance; i++) {
            BlockPos currentPos = BlockPos.containing(pos1.add(step.scale(i)));
            if (mob.getType().isBlockDangerous(level.getBlockState(currentPos))) {
                if (debugparticles) {
                    MagicManager.spawnParticles(level, ParticleTypes.LAVA, currentPos.getX() + .5, currentPos.getY() + .5, currentPos.getZ() + .5, 2, 0, 0, 0, 0.0, true);
                }
                return false; // block is dangerous to traverse through (ie fire)
            } else if (!level.getBlockState(currentPos.below()).isFaceSturdy(level, currentPos.below(), Direction.UP)) {
                if (debugparticles) {
                    MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, currentPos.getX() + .5, currentPos.getY() + .5, currentPos.getZ() + .5, 2, 0, 0, 0, 0.0, true);
                }
                return false; // block does not support upwards (ie is pitfall or no collision)
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (debugparticles) {
            if (path != null) {
                for (Node node : path.nodes) {
                    MagicManager.spawnParticles(level, ParticleHelper.UNSTABLE_ENDER, node.x, node.y + .1, node.z, 2, 0, 0.2, 0, 0, true);
                }
            }
        }
    }
}
