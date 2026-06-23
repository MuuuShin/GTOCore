package com.gtocore.client.model;

import com.gtocore.client.model.ShaderItemModelLoader.UniformValue;
import com.gtocore.client.renderer.GTORenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.Map;

public class ShaderItemBakedModel extends WrappedItemModel {

    private static final float ITEM_FRONT_Z = 8.5F / 16.0F;
    private static final float ITEM_BACK_Z = 7.5F / 16.0F;
    private static final float OVERLAY_Z_OFFSET = 0.001F;

    private final ResourceLocation maskSprite;
    private final ResourceLocation shaderLocation;
    private final Map<String, UniformValue> params;

    public ShaderItemBakedModel(BakedModel wrapped, ResourceLocation maskSprite, ResourceLocation shaderLocation, Map<String, UniformValue> params) {
        super(wrapped);
        this.maskSprite = maskSprite;
        this.shaderLocation = shaderLocation;
        this.params = params;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer,
                           int packedLight, int packedOverlay) {
        renderWrapped(stack, poseStack, buffer, packedLight, packedOverlay, true);
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        ShaderInstance shader = GTORenderTypes.getShader(shaderLocation);
        if (shader == null) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        TextureAtlasSprite maskAtlasSprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(maskSprite);
        RenderType overlayRenderType = GTORenderTypes.getItemShaderOverlay(shaderLocation);

        applyParams(shader, maskAtlasSprite);

        float overlayPadding = getOverlayPadding();
        float min = -overlayPadding;
        float max = 1.0F + overlayPadding;

        MultiBufferSource.BufferSource overlayBuffer = MultiBufferSource.immediate(new BufferBuilder(256));
        VertexConsumer consumer = overlayBuffer.getBuffer(overlayRenderType);
        Matrix4f matrix = poseStack.last().pose();
        // Vanilla generated items have distinct front/back faces, so the shader overlay
        // needs to be drawn on both sides instead of only the front plane.
        renderOverlayQuad(consumer, matrix, min, max, ITEM_FRONT_Z + OVERLAY_Z_OFFSET);
        renderOverlayQuad(consumer, matrix, min, max, ITEM_BACK_Z - OVERLAY_Z_OFFSET);
        overlayBuffer.endBatch(overlayRenderType);
    }

    @Override
    public PerspectiveModelState getModelState() {
        if (parentState instanceof PerspectiveModelState state) {
            return state;
        }
        return PerspectiveModelState.IDENTITY;
    }

    private void applyParams(ShaderInstance shader, TextureAtlasSprite maskAtlasSprite) {
        for (Map.Entry<String, UniformValue> entry : params.entrySet()) {
            applyParam(entry.getKey(), shader, entry.getValue());
        }
        var minecraft = Minecraft.getInstance();
        float guiWidth = minecraft.getWindow().getGuiScaledWidth();
        float guiHeight = minecraft.getWindow().getGuiScaledHeight();
        float screenWidth = minecraft.getWindow().getScreenWidth();
        float screenHeight = minecraft.getWindow().getScreenHeight();
        applyParam("time", shader, new UniformValue(new float[] { (float) (System.currentTimeMillis() % 100000L) / 1000.0F }));
        float mouseX = (float) (minecraft.mouseHandler.xpos() * guiWidth / screenWidth);
        float mouseY = (float) (minecraft.mouseHandler.ypos() * guiHeight / screenHeight);
        applyParam("mousePos", shader, new UniformValue(new float[] { mouseX, mouseY }));
        applyParam("resolution", shader, new UniformValue(new float[] { screenWidth, screenHeight }));
        applyParam("maskUvMin", shader, new UniformValue(new float[] { maskAtlasSprite.getU0(), maskAtlasSprite.getV0() }));
        applyParam("maskUvMax", shader, new UniformValue(new float[] { maskAtlasSprite.getU1(), maskAtlasSprite.getV1() }));
        applyParam("maskUvShrinkRatio", shader, new UniformValue(new float[] { maskAtlasSprite.uvShrinkRatio() }));
        applyParam("maskFrameSize", shader, new UniformValue(new float[] { maskAtlasSprite.contents().width(), maskAtlasSprite.contents().height() }));
        applyParam("overlayPadding", shader, new UniformValue(new float[] { getOverlayPadding() }));
    }

    private float getOverlayPadding() {
        UniformValue overlayPadding = params.get("overlayPadding");
        if (overlayPadding != null && overlayPadding.values().length > 0) {
            return Math.max(0.0F, overlayPadding.values()[0]);
        }

        UniformValue maxDistance = params.get("maxDistance");
        if (maxDistance != null && maxDistance.values().length > 0) {
            return Math.max(0.0F, maxDistance.values()[0] / 16.0F);
        }
        return 0.0F;
    }

    private void applyParam(String name, ShaderInstance shader, UniformValue value) {
        AbstractUniform uniform = shader.safeGetUniform(name);
        applyUniform(uniform, value.values());
    }

    private static void applyUniform(AbstractUniform uniform, float... values) {
        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> uniform.set(values);
        }
    }

    private static void renderOverlayQuad(VertexConsumer consumer, Matrix4f matrix, float min, float max, float z) {
        float minU = 0.0F;
        float maxU = 1.0F;
        consumer.vertex(matrix, max, max, z).uv(maxU, 0.0F).endVertex();
        consumer.vertex(matrix, min, max, z).uv(minU, 0.0F).endVertex();
        consumer.vertex(matrix, min, min, z).uv(minU, 1.0F).endVertex();
        consumer.vertex(matrix, max, min, z).uv(maxU, 1.0F).endVertex();
    }
}
