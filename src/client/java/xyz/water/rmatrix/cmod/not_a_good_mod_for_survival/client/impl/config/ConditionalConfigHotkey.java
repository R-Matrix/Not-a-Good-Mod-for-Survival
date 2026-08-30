/*
 * Design reference: Fallen_Breath's TweakerMore, especially its optional-rule
 * dependency footer and unavailable-rule presentation.
 *
 * Source: https://github.com/Fallen-Breath/TweakerMore
 * This file is an independent implementation and does not copy TweakerMore
 * source code. The project license in LICENSE applies to this implementation.
 */
package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.config.IConfigAvailability;

/** A hotkey whose feature depends on an optional runtime integration. */
public final class ConditionalConfigHotkey extends ConfigHotkey implements IConfigAvailability {
    private final BooleanSupplier availability;
    private final String requiredModId;
    private final String requiredModName;

    public ConditionalConfigHotkey(
            String name,
            String defaultStorageString,
            String comment,
            BooleanSupplier availability,
            String requiredModId,
            String requiredModName
    ) {
        this(name, defaultStorageString, KeybindSettings.DEFAULT, comment, availability, requiredModId, requiredModName);
    }

    public ConditionalConfigHotkey(
            String name,
            String defaultStorageString,
            KeybindSettings settings,
            String comment,
            BooleanSupplier availability,
            String requiredModId,
            String requiredModName
    ) {
        super(name, defaultStorageString, settings, comment);
        this.availability = Objects.requireNonNull(availability, "availability");
        this.requiredModId = Objects.requireNonNull(requiredModId, "requiredModId");
        this.requiredModName = Objects.requireNonNull(requiredModName, "requiredModName");
    }

    @Override
    public ConditionalConfigHotkey apply(String translationPrefix) {
        super.apply(translationPrefix);
        return this;
    }

    @Override
    public boolean isAvailable() {
        return this.availability.getAsBoolean();
    }

    @Override
    public String getRequiredModId() {
        return this.requiredModId;
    }

    @Override
    public String getRequiredModName() {
        return this.requiredModName;
    }

    @Override
    public String getComment() {
        String comment = super.getComment();

        String modColor = this.isAvailable() ? GuiBase.TXT_GRAY : GuiBase.TXT_RED;
        String relationLabel = GuiBase.TXT_DARK_GRAY + GuiBase.TXT_ITALIC
                + "Required mod:" + GuiBase.TXT_RST;
        String relation = GuiBase.TXT_DARK_GRAY + GuiBase.TXT_ITALIC + "- "
                + modColor + this.requiredModName + GuiBase.TXT_GRAY
                + " (" + this.requiredModId + ")" + GuiBase.TXT_RST;

        return comment + "\n" + relationLabel + "\n" + relation;
    }
}
