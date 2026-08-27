/*
 * Design reference: Fallen_Breath's TweakerMore, which exposes optional-rule
 * availability and dependency information in its Malilib configuration UI.
 *
 * Source: https://github.com/Fallen-Breath/TweakerMore
 * This file is an independent implementation and does not copy TweakerMore
 * source code. The project license in LICENSE applies to this implementation.
 */
package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.config;

/** Describes whether a configuration option can operate in the current environment. */
public interface IConfigAvailability {
    boolean isAvailable();

    String getRequiredModId();

    String getRequiredModName();
}
