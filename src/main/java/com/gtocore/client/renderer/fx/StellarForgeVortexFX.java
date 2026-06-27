package com.gtocore.client.renderer.fx;

import com.gtocore.client.renderer.GTORenderTypes;
import com.gtocore.common.machine.multiblock.electric.StellarForgeMachine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

public class StellarForgeVortexFX extends AbstractFX {

    private static final int STALE_TICKS = 12;
    private static final int STARTUP_FADE_TICKS = 12;
    private static final float MIN_VISIBLE_RADIUS = 0.001F;
    private static final VortexMesh MESH = new VortexMesh(3, 24, 96);

    private final ResourceKey<Level> dimension;
    private final BlockPos machinePos;
    private Vec3 center;
    private Direction facing;
    private float bandAngle;
    private float decayRadius;
    private float disappearRadius;
    private int lastRefreshAge;
    private static final long load = System.currentTimeMillis();
    private static final float timeScale = 6000.0F;

    public StellarForgeVortexFX(ResourceKey<Level> dimension, BlockPos machinePos, Vec3 center, Direction facing,
                                float bandAngle, float decayRadius, float disappearRadius) {
        this.dimension = dimension;
        this.machinePos = machinePos.immutable();
        refresh(center, facing, bandAngle, decayRadius, disappearRadius);
    }

    public void refresh(Vec3 center, Direction facing, float bandAngle, float decayRadius, float disappearRadius) {
        this.center = center;
        this.facing = facing;
        this.bandAngle = Math.max(bandAngle, 0.001F);
        this.decayRadius = Math.max(decayRadius, 0.0F);
        this.disappearRadius = Math.max(disappearRadius, this.decayRadius + 0.001F);
        this.lastRefreshAge = age;
    }

    @Override
    public boolean shouldDiscard() {
        if (age - lastRefreshAge > STALE_TICKS) {
            return true;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(dimension)) {
            return true;
        }
        if (!(level.getBlockEntity(machinePos) instanceof MetaMachineBlockEntity machineBlockEntity)) {
            return true;
        }
        MetaMachine metaMachine = machineBlockEntity.getMetaMachine();
        return !(metaMachine instanceof StellarForgeMachine machine) || !machine.isFormed();
    }

    @Override
    public void render(RenderLevelStageEvent.Stage stage, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f projectionMatrix,
                       float partialTick, Camera camera, Frustum frustum) {
        if (stage != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || center == null || facing == null) {
            return;
        }
        if (disappearRadius <= MIN_VISIBLE_RADIUS) {
            return;
        }
        if (frustum != null && !frustum.isVisible(new AABB(
                center.x - disappearRadius, center.y - disappearRadius, center.z - disappearRadius,
                center.x + disappearRadius, center.y + disappearRadius, center.z + disappearRadius))) {
            return;
        }

        ShaderInstance shader = GTORenderTypes.getStellarForgeVortexShader();
        if (shader == null) {
            return;
        }
        var sceneTarget = ScreenSpaceSceneCapture.capture(levelRenderer);
        if (sceneTarget == null) {
            return;
        }

        shader.setSampler("DiffuseSampler", sceneTarget.getColorTextureId());
        shader.safeGetUniform("ScreenSize").set((float) sceneTarget.viewWidth, (float) sceneTarget.viewHeight);
        float time = (System.currentTimeMillis() - load) / timeScale;
        shader.safeGetUniform("Time").set(time);
        shader.safeGetUniform("BandAngle").set(bandAngle);
        shader.safeGetUniform("DecayRadius").set(decayRadius);
        shader.safeGetUniform("DisappearRadius").set(disappearRadius);
        shader.safeGetUniform("VortexIntensity").set(1.0F);

        PoseStack worldStack = new PoseStack();
        worldStack.mulPoseMatrix(poseStack.last().pose());
        Vec3 cameraPos = camera.getPosition();
        worldStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        rotateFromLocalUpToFacing(worldStack, facing);

        float startupFade = Mth.clamp((age + partialTick) / STARTUP_FADE_TICKS, 0.0F, 1.0F);
        ScreenSpaceMeshRenderer.renderScaled(worldStack, MESH.getBuffer(bandAngle),
                GTORenderTypes.STELLAR_FORGE_VORTEX, shader,
                disappearRadius, disappearRadius, disappearRadius,
                1.0F, 1.0F, 1.0F, 0.9F * startupFade);
    }

    private static void rotateFromLocalUpToFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case UP -> {}
        }
    }

    public record Key(ResourceKey<Level> dimension, BlockPos machinePos) {

        public Key {
            machinePos = machinePos.immutable();
        }
    }

    private static final class VortexMesh {

        private final int phiSlices;
        private final int radialSegments;
        private final int angularSegments;
        private VertexBuffer buffer;

        private VortexMesh(int phiSlices, int radialSegments, int angularSegments) {
            this.phiSlices = phiSlices;
            this.radialSegments = radialSegments;
            this.angularSegments = angularSegments;
        }

        private VertexBuffer getBuffer(float bandAngle) {
            if (buffer == null) {
                buffer = buildBuffer(bandAngle);
            }
            return buffer;
        }

        private VertexBuffer buildBuffer(float bandAngle) {
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            PoseStack poseStack = new PoseStack();
            Matrix4f matrix = poseStack.last().pose();

            bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            float safeBandAngle = Math.max(bandAngle, 0.001F);
            for (int phiIndex = 0; phiIndex < phiSlices; phiIndex++) {
                float phiStep = phiSlices == 1 ? 0.0F : phiIndex / (float) (phiSlices - 1);
                float phi = Mth.lerp(phiStep, -safeBandAngle, safeBandAngle);
                float layer = 1.0F - Math.abs(phi / safeBandAngle);
                float alpha = 0.018F + layer * 0.028F;
                for (int radialIndex = 0; radialIndex < radialSegments; radialIndex++) {
                    float r0 = radialIndex / (float) radialSegments;
                    float r1 = (radialIndex + 1) / (float) radialSegments;
                    for (int angularIndex = 0; angularIndex < angularSegments; angularIndex++) {
                        float theta0 = (float) (Math.PI * 2.0D * angularIndex / angularSegments);
                        float theta1 = (float) (Math.PI * 2.0D * (angularIndex + 1) / angularSegments);

                        putVertex(bufferBuilder, matrix, r0, theta0, phi, alpha);
                        putVertex(bufferBuilder, matrix, r1, theta0, phi, alpha);
                        putVertex(bufferBuilder, matrix, r1, theta1, phi, alpha);

                        putVertex(bufferBuilder, matrix, r0, theta0, phi, alpha);
                        putVertex(bufferBuilder, matrix, r1, theta1, phi, alpha);
                        putVertex(bufferBuilder, matrix, r0, theta1, phi, alpha);
                    }
                }
            }

            vertexBuffer.bind();
            vertexBuffer.upload(bufferBuilder.end());
            VertexBuffer.unbind();
            return vertexBuffer;
        }

        private static void putVertex(BufferBuilder bufferBuilder, Matrix4f matrix, float r, float theta, float phi, float alpha) {
            float planeRadius = Mth.cos(phi) * r;
            float x = Mth.cos(theta) * planeRadius;
            float y = Mth.sin(phi) * r;
            float z = Mth.sin(theta) * planeRadius;
            bufferBuilder.vertex(matrix, x, y, z).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        }
    }
}
