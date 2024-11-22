package io.redspace.ironsspellbooks.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.network.spells.LearnSpellPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EldritchResearchScreen extends Screen {
    private static final ResourceLocation WINDOW_LOCATION = new ResourceLocation(IronsSpellbooks.MODID, "textures/gui/eldritch_research_screen/window.png");
    private static final ResourceLocation FRAME_LOCATION = new ResourceLocation(IronsSpellbooks.MODID, "textures/gui/eldritch_research_screen/spell_frame.png");
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 256;
    private static final int WINDOW_INSIDE_X = 9;
    private static final int WINDOW_INSIDE_Y = 18;
    public static final int WINDOW_INSIDE_WIDTH = 234;
    public static final int WINDOW_INSIDE_HEIGHT = 229;
    private static final int WINDOW_TITLE_X = 8;
    private static final int WINDOW_TITLE_Y = 6;
    public static final int BACKGROUND_TILE_WIDTH = 16;
    public static final int BACKGROUND_TILE_HEIGHT = 16;
    public static final int BACKGROUND_TILE_COUNT_X = 14;
    public static final int BACKGROUND_TILE_COUNT_Y = 7;

    int leftPos, topPos;
    InteractionHand activeHand;

    public EldritchResearchScreen(Component pTitle, InteractionHand activeHand) {
        super(pTitle);
        this.activeHand = activeHand;
    }

    List<AbstractSpell> learnableSpells;
    List<SpellNode> nodes;
    SyncedSpellData playerData;
    Vec2 maxViewportOffset;
    Vec2 viewportOffset;

    boolean isMouseHoldingSpell, isMouseDragging;
    int heldSpellIndex = -1;
    int heldSpellTime = -1;
    int lastPlayerTick;
    static final int TIME_TO_HOLD = 15;

    protected void init() {
        learnableSpells = SpellRegistry.getEnabledSpells().stream().filter(AbstractSpell::requiresLearning).toList();
        if (this.minecraft != null) {
            playerData = ClientMagicData.getSyncedSpellData(minecraft.player);
        }
        viewportOffset = Vec2.ZERO;
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;
        float f = 6.282f / learnableSpells.size();
        //RandomSource randomSource = RandomSource.create(431L);
        //int gridsize = 64;

        RandomSource randomSource = RandomSource.create(431L);
        nodes = new ArrayList<>();
        ArrayList<Vec2> nodePoses = new ArrayList<>();
        int nodeDim = 32;
        int centerX = leftPos + WINDOW_WIDTH / 2;
        int centerY = topPos + WINDOW_HEIGHT / 2;
        for (int i = 0; i < learnableSpells.size(); i++) {
            float r = 35;
            float angle = randomSource.nextFloat() * Mth.TWO_PI;
            int x, y;

            if (i == 0) {
                x = centerX;
                y = centerY;
            } else {
                var node = nodePoses.get(randomSource.nextInt(nodePoses.size()));
                x = (int) node.x;
                y = (int) node.y;
            }
            int posx = 0;
            int posy = 0;
            for (int j = 0; j < 100; j++) {
                posx = x + (int) (r * Mth.cos(angle));
                posy = y + (int) (r * Mth.sin(angle));
                //int x = leftPos + WINDOW_WIDTH / 2 - 8 + (int) (r * Mth.cos(f * i));
                //int posy = topPos + WINDOW_HEIGHT / 2 - 8 + (int) (r * Mth.sin(f * i));
                //resolve collisions
                boolean anyIntersect = false;
                for (Vec2 node : nodePoses) {
                    int x11 = (int) node.x;
                    int x12 = (int) node.x + nodeDim;
                    int y11 = (int) node.y;
                    int y12 = (int) node.y + nodeDim;
                    int x21 = posx;
                    int x22 = posx + nodeDim;
                    int y21 = posy;
                    int y22 = posy + nodeDim;
                    boolean intersects = !(x12 < x21 || x22 < x11 || y12 < y21 || y22 < y11);
                    if (intersects) {
                        r += 10;
                        anyIntersect = true;
                        break;
                    }
                }
                if (!anyIntersect) {
                    break;
                }
            }
            nodePoses.add(new Vec2(posx, posy));
        }
        int averageX = 0;
        int averageY = 0;
        for (Vec2 node : nodePoses) {
            averageY += (int) node.x;
            averageY += (int) node.y;
        }
        averageX = averageX / nodePoses.size() - centerX;
        averageY = averageY / nodePoses.size() - centerY;
        for (int i = 0; i < nodePoses.size(); i++) {
            nodes.add(new SpellNode(learnableSpells.get(i), (int) nodePoses.get(i).x-averageX, (int) nodePoses.get(i).y-averageY));
        }
        float maxDistX = 0;
        float maxDistY = 0;
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 1; j < nodes.size(); j++) {
                int x = Math.abs(nodes.get(i).x - nodes.get(j).x);
                if (x > maxDistX) {
                    maxDistX = x;
                }
                int y = Math.abs(nodes.get(i).y - nodes.get(j).y);
                if (y > maxDistY) {
                    maxDistY = y;
                }
            }
        }
        //TODO: wait this makes no sense
        maxViewportOffset = new Vec2((int) maxDistX, (int) maxDistY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        drawBackdrop(guiGraphics, leftPos + WINDOW_INSIDE_X, topPos + WINDOW_INSIDE_Y);

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.tickCount != lastPlayerTick) {
            lastPlayerTick = player.tickCount;
            if (isMouseHoldingSpell && heldSpellIndex >= 0 && heldSpellIndex < nodes.size() && !nodes.get(heldSpellIndex).spell.isLearned(player)) {
                if (heldSpellTime > TIME_TO_HOLD) {
                    heldSpellTime = -1;
                    PacketDistributor.sendToServer(new LearnSpellPacket(this.activeHand, nodes.get(heldSpellIndex).spell.getSpellId()));
                    player.playNotifySound(SoundRegistry.LEARN_ELDRITCH_SPELL.get(), SoundSource.MASTER, 1f, Utils.random.nextIntBetweenInclusive(9, 11) * .1f);
                }
                heldSpellTime++;
                if (lastPlayerTick % 2 == 0) {
                    player.playNotifySound(SoundEvents.SOUL_ESCAPE.value(), SoundSource.MASTER, 1f, Mth.lerp(heldSpellTime / (float) TIME_TO_HOLD, .5f, 1.5f));
                    player.playNotifySound(SoundRegistry.UI_TICK.get(), SoundSource.MASTER, 1f, Mth.lerp(heldSpellTime / (float) TIME_TO_HOLD, .5f, 1.5f));
                }
            } else if (heldSpellTime >= 0) {
                heldSpellTime = Math.max(heldSpellTime - 3, -1);
            }
        }
        handleConnections(guiGraphics, partialTick);
        List<FormattedCharSequence> tooltip = null;
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            drawNode(guiGraphics, node, player, i == heldSpellIndex && heldSpellTime > 0);
            if (isHoveringNode(node, mouseX, mouseY)) {
                tooltip = buildTooltip(node.spell, font);
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(WINDOW_LOCATION, leftPos, topPos, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        if (tooltip != null) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
//        String mousePos = String.format("(%s\t%s)", mouseX, mouseY);
//        String boundX = String.format("%s + %s - %s = %s", mouseX, (int) viewportOffset.x, leftPos, mouseX + (int) viewportOffset.x - leftPos);
//        String boundY = String.format("%s + %s - %s = %s", mouseY, (int) viewportOffset.y, topPos, mouseY + (int) viewportOffset.y - topPos);
//        String pX = String.format("%s", (mouseX + (int) viewportOffset.x - leftPos) / (float)WINDOW_WIDTH);
//        String pY = String.format("%s", (mouseY + (int) viewportOffset.y - topPos) / (float)WINDOW_HEIGHT);
//        String sx = String.format("x:%s", ( Math.clamp(Mth.clamp(mouseX + viewportOffset.x - leftPos, 0, WINDOW_WIDTH)/20f,0,1)) / (float)WINDOW_WIDTH);
//        guiGraphics.drawString(font, mousePos, 0, 0, 0xFFFFFF);
//        guiGraphics.drawString(font, boundX, 0, 10, 0xFFFFFF);
//        guiGraphics.drawString(font, boundY, 0, 20, 0xFFFFFF);
//        guiGraphics.drawString(font, pX, 0, 30, 0xFFFFFF);
//        guiGraphics.drawString(font, pY, 0, 40, 0xFFFFFF);
//        guiGraphics.drawString(font, sx, 0, 50, 0xFFFFFF);

    }

    private void renderProgressOverlay(GuiGraphics gui, int x, int y, float progress) {
        x += (int) viewportOffset.x;
        y += (int) viewportOffset.y;
        gui.fill(x, y, x + Mth.ceil(16.0F * progress), y + 16, FastColor.ARGB32.color(127, 244, 65, 255));
    }

    private void drawNode(GuiGraphics guiGraphics, SpellNode node, LocalPlayer player, boolean drawProgress) {
        drawWithClipping(node.spell.getSpellIconResource(),
                guiGraphics,
                node.x,
                node.y,
                0, 0,
                16, 16,
                16, 16,
                leftPos + WINDOW_INSIDE_X, topPos + WINDOW_INSIDE_Y,
                WINDOW_INSIDE_WIDTH, WINDOW_INSIDE_HEIGHT);
        if (drawProgress) {
            renderProgressOverlay(guiGraphics, node.x, node.y, heldSpellTime / (float) TIME_TO_HOLD);
        }
        drawWithClipping(FRAME_LOCATION,
                guiGraphics,
                node.x - 8,
                node.y - 8,
                node.spell.isLearned(player) ? 32 : 0, 0,
                32, 32,
                64, 32,
                leftPos + WINDOW_INSIDE_X, topPos + WINDOW_INSIDE_Y,
                WINDOW_INSIDE_WIDTH, WINDOW_INSIDE_HEIGHT);
    }

    private void drawWithClipping(ResourceLocation texture, GuiGraphics guiGraphics, int x, int y, int uvx, int uvy, int width, int height, int imageWidth, int imageHeight, int bbx, int bby, int bbw, int bbh) {
        x += (int) viewportOffset.x;
        if (x < bbx) {
            int xDiff = bbx - x;
            width -= xDiff;
            uvx += xDiff;
            x += xDiff;
        } else if (x > bbx + bbw - width) {
            int xDiff = x - (bbx + bbw - width);
            width -= xDiff;
        }
        y += (int) viewportOffset.y;
        if (y < bby) {
            int yDiff = bby - y;
            height -= yDiff;
            uvy += yDiff;
            y += yDiff;
        } else if (y > bby + bbh - height) {
            int yDiff = y - (bby + bbh - height);
            height -= yDiff;
        }
        if (width > 0 && height > 0) {
            guiGraphics.blit(texture, x, y, width, height, uvx, uvy, width, height, imageWidth, imageHeight);
        }
    }

    private static final Component ALREADY_LEARNED = Component.translatable("ui.irons_spellbooks.research_already_learned").withStyle(ChatFormatting.DARK_AQUA);
    private static final Component UNLEARNED = Component.translatable("ui.irons_spellbooks.research_warning").withStyle(ChatFormatting.RED);

    public static List<FormattedCharSequence> buildTooltip(AbstractSpell spell, Font font) {
        boolean learned = spell.isLearned(Minecraft.getInstance().player);
        var name = spell.getDisplayName(null).withStyle(learned ? ChatFormatting.DARK_AQUA : ChatFormatting.RED);
        var description = font.split(Component.translatable(String.format("%s.guide", spell.getComponentId())).withStyle(ChatFormatting.GRAY), 180);
        var hoverText = new ArrayList<FormattedCharSequence>();
        hoverText.add(FormattedCharSequence.forward(name.getString(), name.getStyle().withUnderlined(true)));
        hoverText.addAll(description);
        hoverText.add(FormattedCharSequence.EMPTY);
        hoverText.add((learned ? ALREADY_LEARNED : UNLEARNED).getVisualOrderText());
        return hoverText;
    }

    private void handleConnections(GuiGraphics guiGraphics, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0);
        RenderSystem.enableDepthTest();
        float f = Mth.sin((Minecraft.getInstance().player.tickCount + partialTick) * .1f);
        float glowIntensity = f * f * .8f + .2f;
        var color = new Vector4f(135 / 255f, 154 / 255f, 174 / 255f, 0.5f);
        var glowcolor = new Vector4f(244 / 255f, 65 / 255f, 255 / 255f, 0.5f);
        for (int i = 0; i < nodes.size() - 1; i++) {

            Vec2 a = new Vec2(nodes.get(i).x, nodes.get(i).y);
            Vec2 b = new Vec2(nodes.get(i + 1).x, nodes.get(i + 1).y);
            Vec2 orth = new Vec2(-(b.y - a.y), b.x - a.x).normalized().scale(1.5f);

            final float x1m1 = a.x + orth.x + 8 + (int) viewportOffset.x;
            final float x2m1 = b.x + orth.x + 8 + (int) viewportOffset.x;
            final float y1m1 = a.y + orth.y + 8 + (int) viewportOffset.y;
            final float y2m1 = b.y + orth.y + 8 + (int) viewportOffset.y;

            final float x1m2 = a.x - orth.x + 8 + (int) viewportOffset.x;
            final float x2m2 = b.x - orth.x + 8 + (int) viewportOffset.x;
            final float y1m2 = a.y - orth.y + 8 + (int) viewportOffset.y;
            final float y2m2 = b.y - orth.y + 8 + (int) viewportOffset.y;

            var color1 = lerpColor(color, glowcolor, glowIntensity * (nodes.get(i).spell.isLearned(Minecraft.getInstance().player) ? 1 : 0));
            var color2 = lerpColor(color, glowcolor, glowIntensity * (nodes.get(i + 1).spell.isLearned(Minecraft.getInstance().player) ? 1 : 0));
            RenderHelper.quadBuilder()
                    .vertex(x1m1, y1m1).color(fadeOutTowardEdges(guiGraphics, x1m1, y1m1, color1))
                    .vertex(x2m1, y2m1).color(fadeOutTowardEdges(guiGraphics, x2m1, y2m1, color2))
                    .vertex(x2m2, y2m2).color(fadeOutTowardEdges(guiGraphics, x2m2, y2m2, color2))
                    .vertex(x1m2, y1m2).color(fadeOutTowardEdges(guiGraphics, x1m2, y1m2, color1))
                    .build(guiGraphics, RenderType.gui());
        }
    }

    private Vector4f fadeOutTowardEdges(GuiGraphics guiGraphics, double x, double y, Vector4f color) {
        float margin = 40;
        int maxWidth = WINDOW_WIDTH;
        int maxHeight = WINDOW_HEIGHT;
        int boundXMin = (int) Mth.clamp(x + viewportOffset.x - leftPos, 0, maxWidth);
        int boundXMax = maxWidth - (int) Mth.clamp(x + viewportOffset.x - leftPos, 0, maxWidth);
        int boundYMin = (int) Mth.clamp(y + viewportOffset.y - topPos, 0, maxHeight);
        int boundYMax = maxHeight - (int) Mth.clamp(y + viewportOffset.y - topPos, 0, maxHeight);
        float px = Mth.clamp(Math.min(boundXMin, boundXMax) / margin, 0, 1);
        float py = Mth.clamp(Math.min(boundYMin, boundYMax) / margin, 0, 1);
        float alpha = Mth.sqrt(px * py);
        return new Vector4f(color.x, color.y, color.z, color.w * alpha);

    }

    private int colorFromRGBA(Vector4f rgba) {
        var r = (int) (rgba.x() * 255) & 0xFF;
        var g = (int) (rgba.y() * 255) & 0xFF;
        var b = (int) (rgba.z() * 255) & 0xFF;
        var a = (int) (rgba.w() * 255) & 0xFF;

        return (r << 24) + (g << 16) + (b << 8) + (a);
    }

    private void drawBackdrop(GuiGraphics guiGraphics, int left, int top) {
        float f = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount * .02f : 0f;
        float color = (Mth.sin(f) + 1) * .25f + .15f;
        var definitelynothowabuilderworks = RenderHelper.quadBuilder()
                .vertex(left, top + EldritchResearchScreen.WINDOW_INSIDE_HEIGHT)
                .vertex(left + EldritchResearchScreen.WINDOW_INSIDE_WIDTH, top + EldritchResearchScreen.WINDOW_INSIDE_HEIGHT)
                .vertex(left + EldritchResearchScreen.WINDOW_INSIDE_WIDTH, top)
                .vertex(left, top)
                .color(0, 0, 0, color);
        definitelynothowabuilderworks.build(guiGraphics, RenderType.endPortal());
        definitelynothowabuilderworks.build(guiGraphics, RenderType.guiOverlay());
    }

    private static Vector4f lerpColor(Vector4f a, Vector4f b, float pDelta) {
        float f = 1.0F - pDelta;
        var x = a.x() * f + b.x() * pDelta;
        var y = a.y() * f + b.y() * pDelta;
        var z = a.z() * f + b.z() * pDelta;
        var w = a.w() * f + b.w() * pDelta;
        return new Vector4f(x, y, z, w);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        int mouseX = (int) pMouseX;
        int mouseY = (int) pMouseY;
        //Only allow initiating the learn process if they are holding a manuscript
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getItemInHand(activeHand).is(ItemRegistry.ELDRITCH_PAGE.get())) {
            for (int i = 0; i < nodes.size(); i++) {
                if (isHoveringNode(nodes.get(i), mouseX, mouseY)) {
                    heldSpellIndex = i;
                    isMouseHoldingSpell = true;
                    break;
                }
            }
        }
        if (!isMouseHoldingSpell) {
            if (isHovering(leftPos + WINDOW_INSIDE_X, topPos + WINDOW_INSIDE_Y, WINDOW_INSIDE_WIDTH, WINDOW_INSIDE_HEIGHT, mouseX, mouseY)) {
                isMouseDragging = true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public boolean isHoveringNode(SpellNode node, int mouseX, int mouseY) {
        //TODO: make outside screen unclickable
        return isHovering(node.x - 2 + (int) viewportOffset.x, node.y - 2 + (int) viewportOffset.y, 16 + 4, 16 + 4, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        isMouseHoldingSpell = false;
        isMouseDragging = false;
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.isMouseDragging && false) {
            viewportOffset = new Vec2((float) (viewportOffset.x + pDragX), (float) (viewportOffset.y + pDragY));
            return true;
        } else {
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
    }


    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(pKeyCode, pScanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    record SpellNode(AbstractSpell spell, int x, int y) {
    }

    record NodeConnection(SpellNode node1, SpellNode node2) {
    }

//    private static void setTexture(ResourceLocation texture) {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, texture);
//    }
//
//    private static void setTranslucentTexture(ResourceLocation texture) {
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);
//        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        RenderSystem.setShaderTexture(0, texture);
//    }
}
