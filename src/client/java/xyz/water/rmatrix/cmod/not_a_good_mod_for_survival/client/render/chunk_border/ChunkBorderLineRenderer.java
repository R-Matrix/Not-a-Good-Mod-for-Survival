package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.render.chunk_border;

import java.util.OptionalDouble;
import java.util.function.Function;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.Util;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;

/**
 * Adds screen-space thick lines to the vanilla chunk border renderer.
 *
 * <p>The vanilla debug line layer uses POSITION_COLOR and therefore cannot
 * consume the line-width uniform. This layer uses the rendertype_lines shader
 * and the LINES vertex format, which expands each line in the vertex shader.</p>
 */
public final class ChunkBorderLineRenderer {
    private static final float OCCLUDED_BLUE_LINE_WIDTH = 1.0F;

    private static final Function<Integer, RenderLayer> THICK_LINES = Util.memoize(
            lineWidth -> RenderLayer.of(
                    "not_a_good_mod_for_survival_thick_chunk_border_lines",
                    VertexFormats.LINES,
                    VertexFormat.DrawMode.LINES,
                    RenderLayer.DEFAULT_BUFFER_SIZE,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.LINES_PROGRAM)
                            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(lineWidth.doubleValue())))
                            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .build(false)
            )
    );

    private static final RenderLayer OCCLUDED_BLUE_LINES = RenderLayer.of(
            "not_a_good_mod_for_survival_occluded_chunk_border_lines",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            RenderLayer.DEFAULT_BUFFER_SIZE,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(OCCLUDED_BLUE_LINE_WIDTH)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    private ChunkBorderLineRenderer() {
    }

    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        boolean renderThickLines = Configs.DebugRender.THICK_CHUNK_BORDER_LINES.getBooleanValue();
        boolean renderOccludedCurrentSubchunk = Configs.DebugRender.SHOW_OCCLUDED_CURRENT_SUBCHUNK_BLUE_LINES.getBooleanValue();
        if (!renderThickLines && !renderOccludedCurrentSubchunk) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        Entity entity = client.gameRenderer.getCamera().getFocusedEntity();
        if (entity == null) {
            return;
        }

        float bottomY = (float)(client.world.getBottomY() - cameraY);
        float topY = (float)(client.world.getTopYInclusive() + 1 - cameraY);
        ChunkPos chunkPos = entity.getChunkPos();
        float chunkStartX = (float)(chunkPos.getStartX() - cameraX);
        float chunkStartZ = (float)(chunkPos.getStartZ() - cameraZ);
        MatrixStack.Entry entry = matrices.peek();
        if (renderThickLines) {
            VertexConsumer vertices = vertexConsumers.getBuffer(
                    THICK_LINES.apply(Configs.DebugRender.CHUNK_BORDER_LINE_WIDTH.getIntegerValue())
            );

            // Vanilla's red lines are the 4x4 lattice of vertical chunk-corner lines.
            for (int x = -16; x <= 32; x += 16) {
                for (int z = -16; z <= 32; z += 16) {
                    drawLine(
                            vertices,
                            entry,
                            chunkStartX + x,
                            bottomY,
                            chunkStartZ + z,
                            chunkStartX + x,
                            topY,
                            chunkStartZ + z,
                            1.0F,
                            0.0F,
                            0.0F
                    );
                }
            }

            // Vanilla's blue lines are the four vertical edges of the current chunk.
            drawBlueLines(vertices, entry, client, cameraY, chunkStartX, chunkStartZ, bottomY, topY);
        }

        if (renderOccludedCurrentSubchunk) {
            // Only the current 16x16x16 subchunk gets a depth-independent thin outline.
            ChunkSectionPos currentSection = ChunkSectionPos.from(entity);
            float currentSectionBottom = (float)(currentSection.getMinY() - cameraY);
            float currentSectionTop = currentSectionBottom + 16.0F;
            VertexConsumer occludedBlueVertices = vertexConsumers.getBuffer(OCCLUDED_BLUE_LINES);
            drawCurrentSubchunkBlueLines(
                    occludedBlueVertices,
                    entry,
                    chunkStartX,
                    chunkStartZ,
                    currentSectionBottom,
                    currentSectionTop
            );
        }
    }

    private static void drawBlueLines(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            MinecraftClient client,
            double cameraY,
            float chunkStartX,
            float chunkStartZ,
            float bottomY,
            float topY
    ) {
        // The four vertical edges of the current chunk.
        for (int x = 0; x <= 16; x += 16) {
            for (int z = 0; z <= 16; z += 16) {
                drawLine(
                        vertices,
                        entry,
                        chunkStartX + x,
                        bottomY,
                        chunkStartZ + z,
                        chunkStartX + x,
                        topY,
                        chunkStartZ + z,
                        0.25F,
                        0.25F,
                        1.0F
                );
            }
        }

        // A blue square every 16 blocks vertically.
        for (int y = client.world.getBottomY(); y <= client.world.getTopYInclusive() + 1; y += 16) {
            float level = (float)(y - cameraY);
            drawLine(vertices, entry, chunkStartX, level, chunkStartZ, chunkStartX, level, chunkStartZ + 16.0F, 0.25F, 0.25F, 1.0F);
            drawLine(vertices, entry, chunkStartX, level, chunkStartZ + 16.0F, chunkStartX + 16.0F, level, chunkStartZ + 16.0F, 0.25F, 0.25F, 1.0F);
            drawLine(vertices, entry, chunkStartX + 16.0F, level, chunkStartZ + 16.0F, chunkStartX + 16.0F, level, chunkStartZ, 0.25F, 0.25F, 1.0F);
            drawLine(vertices, entry, chunkStartX + 16.0F, level, chunkStartZ, chunkStartX, level, chunkStartZ, 0.25F, 0.25F, 1.0F);
        }
    }

    private static void drawCurrentSubchunkBlueLines(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float chunkStartX,
            float chunkStartZ,
            float sectionBottom,
            float sectionTop
    ) {
        for (int x = 0; x <= 16; x += 16) {
            for (int z = 0; z <= 16; z += 16) {
                drawLine(
                        vertices,
                        entry,
                        chunkStartX + x,
                        sectionBottom,
                        chunkStartZ + z,
                        chunkStartX + x,
                        sectionTop,
                        chunkStartZ + z,
                        0.25F,
                        0.25F,
                        1.0F
                );
            }
        }

        drawBlueSquare(vertices, entry, chunkStartX, chunkStartZ, sectionBottom);
        drawBlueSquare(vertices, entry, chunkStartX, chunkStartZ, sectionTop);
    }

    private static void drawBlueSquare(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float chunkStartX,
            float chunkStartZ,
            float level
    ) {
        drawLine(vertices, entry, chunkStartX, level, chunkStartZ, chunkStartX, level, chunkStartZ + 16.0F, 0.25F, 0.25F, 1.0F);
        drawLine(vertices, entry, chunkStartX, level, chunkStartZ + 16.0F, chunkStartX + 16.0F, level, chunkStartZ + 16.0F, 0.25F, 0.25F, 1.0F);
        drawLine(vertices, entry, chunkStartX + 16.0F, level, chunkStartZ + 16.0F, chunkStartX + 16.0F, level, chunkStartZ, 0.25F, 0.25F, 1.0F);
        drawLine(vertices, entry, chunkStartX + 16.0F, level, chunkStartZ, chunkStartX, level, chunkStartZ, 0.25F, 0.25F, 1.0F);
    }

    private static void drawLine(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float startX,
            float startY,
            float startZ,
            float endX,
            float endY,
            float endZ,
            float red,
            float green,
            float blue
    ) {
        float normalX = endX - startX;
        float normalY = endY - startY;
        float normalZ = endZ - startZ;
        vertices.vertex(entry, startX, startY, startZ)
                .color(red, green, blue, 1.0F)
                .normal(entry, normalX, normalY, normalZ);
        vertices.vertex(entry, endX, endY, endZ)
                .color(red, green, blue, 1.0F)
                .normal(entry, normalX, normalY, normalZ);
    }
}
