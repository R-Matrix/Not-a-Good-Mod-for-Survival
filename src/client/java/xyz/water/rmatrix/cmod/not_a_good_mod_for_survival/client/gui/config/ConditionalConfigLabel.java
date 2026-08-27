/*
 * This file contains an adaptation of the configuration-label presentation
 * used by TweakerMore.
 *
 * Original project: TweakerMore
 * Copyright (C) 2023 Fallen_Breath and contributors
 * Source: https://github.com/Fallen-Breath/TweakerMore
 * Original license: GNU Lesser General Public License v3.0 (LGPL-3.0-only)
 *
 * The adapted portions of this file remain available under the same license.
 * See THIRD_PARTY_NOTICES.md and LICENSES/LGPL-3.0.txt for the attribution
 * and license reference.
 */
package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.config;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;

/** Malilib config label that marks an unavailable option in dark red. */
public final class ConditionalConfigLabel extends WidgetLabel {
    public ConditionalConfigLabel(
            int x,
            int y,
            int width,
            int height,
            int textColor,
            String[] displayLines
    ) {
        super(x, y, width, height, textColor, displayLines);

        this.labels.replaceAll(s -> GuiBase.TXT_DARK_RED + s + GuiBase.TXT_RST);
    }
}
