package io.redspace.ironsspellbooks.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RenderHelper {

    public static int colorLerp(float f, int colorA, int colorB) {
        int redA = (colorA >> 16) & 0xFF;
        int greenA = (colorA >> 8) & 0xFF;
        int blueA = colorA & 0xFF;
        int redB = (colorB >> 16) & 0xFF;
        int greenB = (colorB >> 8) & 0xFF;
        int blueB = colorB & 0xFF;
        return color255(
                (int) Mth.lerp(f, redA, redB),
                (int) Mth.lerp(f, greenA, greenB),
                (int) Mth.lerp(f, blueA, blueB)
        );
    }

    public static int color255(int pRed, int pGreen, int pBlue, int pAlpha) {
        return pAlpha << 24 | pRed << 16 | pGreen << 8 | pBlue;
    }

    public static int color255(int pRed, int pGreen, int pBlue) {
        return color255(pRed, pGreen, pBlue, 255);
    }

    public static int colorf(float pRed, float pGreen, float pBlue, float pAlpha) {
        return color255((int) (255 * pRed), (int) (255 * pGreen), (int) (255 * pBlue), (int) (255 * pAlpha));
    }

    public static int colorf(float pRed, float pGreen, float pBlue) {
        return colorf(pRed, pGreen, pBlue, 1f);
    }

    public static QuadBuilder quadBuilder() {
        return new QuadBuilder();
    }

    public static class QuadBuilder {
        List<Vector3f> verticies;
        List<Vector3f> normals;
        List<Vec2> uvs;
        List<Integer> colors;
        Integer light = null;
        Integer overlay = null;
        @Nullable Matrix4f matrix;

        private QuadBuilder() {
            this.verticies = new ArrayList<>();
            this.normals = new ArrayList<>();
            this.uvs = new ArrayList<>();
            this.colors = new IntArrayList();
        }

        public QuadBuilder vertex(float x, float y) {
            this.verticies.add(new Vector3f(x, y, 0));
            return this;
        }

        public QuadBuilder vertex(float x, float y, float z) {
            this.verticies.add(new Vector3f(x, y, z));
            return this;
        }

        public QuadBuilder uv(float u, float v) {
            this.uvs.add(new Vec2(u, v));
            return this;
        }


        public QuadBuilder normal(float x, float y, float z) {
            this.normals.add(new Vector3f(x, y, z));
            return this;
        }

        public QuadBuilder matrix(Matrix4f matrix) {
            this.matrix = matrix;
            return this;
        }

        /**
         * Color in the format 0-1f, rgba
         */
        public QuadBuilder color(Vector4f color) {
            this.colors.add(colorf(color.x, color.y, color.z, color.w));
            return this;
        }

        public QuadBuilder color(int color) {
            this.colors.add(color);
            return this;
        }

        /**
         * Color in the format 0-1f, rgb
         */
        public QuadBuilder color(Vector3f color) {
            return color(new Vector4f(color.x, color.y, color.z, 1f));
        }

        /**
         * Color in the format 0-1f, rgb
         */
        public QuadBuilder color(float r, float g, float b) {
            return color(r, g, b, 1f);
        }

        /**
         * Color in the format 0-1f, rgba
         */
        public QuadBuilder color(float r, float g, float b, float a) {
            return color(new Vector4f(r, g, b, a));
        }

        public QuadBuilder light(int light) {
            this.light = light;
            return this;
        }

        public QuadBuilder overlay(int overlay) {
            this.overlay = overlay;
            return this;
        }

        public void build(VertexConsumer consumer) {
            for (int i = 0; i < verticies.size(); i++) {
                var vertex = verticies.get(i);
                int color;
                if (colors.isEmpty()) {
                    color = 0xFFFFFF;
                } else if (colors.size() == 1 || colors.size() != verticies.size()) {
                    color = colors.get(0);
                } else {
                    color = colors.get(i);
                }
                if (matrix != null) {
                    vertex = matrix.transformPosition(vertex.x, vertex.y, vertex.z, new Vector3f());
                }
                consumer.addVertex(vertex.x, vertex.y, vertex.z).setColor(color);
                if (!uvs.isEmpty()) {
                    consumer.setUv(uvs.get(i).x, uvs.get(i).y);
                }
                if (!normals.isEmpty()) {
                    consumer.setNormal(normals.get(i).x, normals.get(i).y, normals.get(i).z);
                }
                if (light != null) {
                    consumer.setLight(light);
                }
                if (overlay != null) {
                    consumer.setOverlay(overlay);
                }
            }
        }

        public void build(GuiGraphics graphics, RenderType renderType) {
            build(graphics.bufferSource().getBuffer(renderType));
            graphics.flush();
        }

        public void build(GuiGraphics graphics) {
            build(graphics, RenderType.gui());
        }
    }

    public static class CustomerRenderType extends RenderType {
        public CustomerRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }

        public static @NotNull RenderType darkGlow(@NotNull ResourceLocation pLocation) {
            return DARK_PORTAL_GLOW.apply(pLocation);
        }

        protected static final RenderStateShard.TransparencyStateShard ONE_MINUS = new RenderStateShard.TransparencyStateShard("one_minus", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        });

        private static final Function<ResourceLocation, RenderType> DARK_PORTAL_GLOW = Util.memoize(
                pLocation -> {
                    return create("crumbling", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false)).setTransparencyState(ONE_MINUS).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
                }
        );
        private static final Function<ResourceLocation, RenderType> MAGIC = Util.memoize(
                pLocation -> {
                    return create("magic_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false)).setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
                }
        );
        private static final Function<ResourceLocation, RenderType> MAGIC_NO_CULL = Util.memoize(
                pLocation -> {
                    return create("magic_glow_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false)).setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
                }
        );

        public static RenderType magic(ResourceLocation pLocation) {
            return MAGIC.apply(pLocation);
        }

        public static RenderType magicNoCull(ResourceLocation pLocation) {
            return MAGIC_NO_CULL.apply(pLocation);
        }

        public static RenderType magicSwirl(ResourceLocation pLocation, float pU, float pV) {
            return create("magic_glow_swirl", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false)).setTexturingState(new RenderStateShard.OffsetTexturingStateShard(pU, pV)).setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        }


    }
}
