package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.sign;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;

/** Enlarges and centers a sign face when it contains exactly one visible character. */
@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class SingleCharacterSignTextMixin {
    @Inject(
            method = "renderText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/entity/AbstractSignBlockEntityRenderer;applyTextTransforms(Lnet/minecraft/client/util/math/MatrixStack;ZLnet/minecraft/util/math/Vec3d;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void notAGoodModForSurvival$enlargeSingleCharacterText(
            BlockPos pos,
            SignText signText,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int textLineHeight,
            int maxTextWidth,
            boolean front,
            CallbackInfo info
    ) {
        if (!Configs.Signs.ENLARGE_SINGLE_CHARACTER.getBooleanValue()) {
            return;
        }

        float scale = (float) Configs.Signs.SINGLE_CHARACTER_SCALE.getDoubleValue();
        if (scale <= 1.0F) {
            return;
        }

        int singleCharacterLine = this.notAGoodModForSurvival$findSingleCharacterLine(signText);
        if (singleCharacterLine < 0) {
            return;
        }

        float originalLineY = singleCharacterLine * textLineHeight - 2.0F * textLineHeight;
        float centeredLineY = -0.5F * textLineHeight
                + (float) Configs.Signs.SINGLE_CHARACTER_VERTICAL_OFFSET.getDoubleValue();
        float translationY = centeredLineY
                - scale * originalLineY;

        matrices.translate(0.0F, translationY, 0.0F);
        matrices.scale(scale, scale, scale);
    }

    @Unique
    private int notAGoodModForSurvival$findSingleCharacterLine(SignText signText) {
        Text[] messages = signText.getMessages(MinecraftClient.getInstance().shouldFilterText());
        int characterCount = 0;
        int singleCharacterLine = -1;

        for (int line = 0; line < messages.length; line++) {
            String message = messages[line].getString();
            int lineCharacterCount = (int) message.codePoints()
                    .filter(codePoint -> !Character.isWhitespace(codePoint))
                    .count();

            if (lineCharacterCount == 0) {
                continue;
            }

            characterCount += lineCharacterCount;
            if (characterCount > 1) {
                return -1;
            }

            singleCharacterLine = line;
        }

        return characterCount == 1 ? singleCharacterLine : -1;
    }
}
