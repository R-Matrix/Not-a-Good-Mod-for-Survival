package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.fireworks;

import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;

/** Makes the start of the vanilla animated-particle fade configurable for firework sparks. */
@Mixin(AnimatedParticle.class)
public abstract class AnimatedParticleMixin extends SpriteBillboardParticle {
    @Shadow
    private float targetRed;

    @Shadow
    private float targetGreen;

    @Shadow
    private float targetBlue;

    @Shadow
    private boolean changesColor;

    protected AnimatedParticleMixin(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
    }

    @Unique
    private boolean notAGoodModForSurvival$hasTargetColor;

    @Unique
    private boolean notAGoodModForSurvival$customFadeInitialized;

    @Unique
    private float notAGoodModForSurvival$baseAlpha;

    @Inject(method = "setTargetColor", at = @At("TAIL"))
    private void notAGoodModForSurvival$rememberTargetColor(int color, CallbackInfo info) {
        this.notAGoodModForSurvival$hasTargetColor = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void notAGoodModForSurvival$disableVanillaFade(CallbackInfo info) {
        if (Configs.Fireworks.FADE_START_TICK.getIntegerValue() >= 0) {
            if (!this.notAGoodModForSurvival$customFadeInitialized) {
                this.notAGoodModForSurvival$customFadeInitialized = true;
                this.notAGoodModForSurvival$baseAlpha = this.alpha;
            }

            // The original method would otherwise start changing color at maxAge / 2.
            this.changesColor = false;
        } else if (this.notAGoodModForSurvival$hasTargetColor) {
            // Restore vanilla behavior if the setting is changed back while a particle is alive.
            this.changesColor = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void notAGoodModForSurvival$applyConfiguredFade(CallbackInfo info) {
        int fadeStartTick = Configs.Fireworks.FADE_START_TICK.getIntegerValue();

        if (fadeStartTick < 0 || !this.notAGoodModForSurvival$hasTargetColor) {
            return;
        }

        int start = Math.min(fadeStartTick, this.maxAge);

        if (this.age <= start) {
            this.setAlpha(this.notAGoodModForSurvival$baseAlpha);
            return;
        }

        float duration = Math.max(1.0F, this.maxAge - start);
        float progress = Math.min(1.0F, (this.age - start) / duration);

        this.setAlpha(this.notAGoodModForSurvival$baseAlpha * (1.0F - progress));
        this.red += (this.targetRed - this.red) * 0.2F;
        this.green += (this.targetGreen - this.green) * 0.2F;
        this.blue += (this.targetBlue - this.blue) * 0.2F;
    }
}
