package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

import java.util.Optional;

/** A banner decoration transported by MapCatalogSync. */
public record MapCatalogBanner(
        int worldX,
        int worldZ,
        DyeColor color,
        Optional<Text> name
) {
    public MapCatalogBanner {
        name = name == null ? Optional.empty() : name;
    }
}
