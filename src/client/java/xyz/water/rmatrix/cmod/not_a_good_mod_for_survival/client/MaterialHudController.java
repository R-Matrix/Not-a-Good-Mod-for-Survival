package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.MinecraftClient;

/** Keeps the custom renderer attached to Litematica's current material list. */
public final class MaterialHudController {
    private static MaterialListBase currentMaterialList;
    private static OpenZenMaterialHudRenderer renderer;

    private MaterialHudController() {
    }

    public static void tick(MinecraftClient client) {
        MaterialListBase next = DataManager.getMaterialList();

        if (next != currentMaterialList) {
            if (currentMaterialList != null) {
                InfoHud.getInstance().removeInfoHudRenderer(currentMaterialList.getHudRenderer(), false);
            }
            if (renderer != null) {
                InfoHud.getInstance().removeInfoHudRenderer(renderer, false);
            }

            currentMaterialList = next;
            renderer = next == null ? null : new OpenZenMaterialHudRenderer(next);
        }

        if (currentMaterialList != null && renderer != null) {
            // Litematica may re-add its stock renderer when the material-list button is pressed.
            // Keep the stock renderer out so the list is drawn only once.
            InfoHud.getInstance().removeInfoHudRenderer(currentMaterialList.getHudRenderer(), false);
            InfoHud.getInstance().addInfoHudRenderer(renderer, false);
        }

    }

    public static boolean openEditor() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen == null && client.player != null && renderer != null) {
            GuiBase.openGui(new MaterialListClickGuiScreen(renderer));
            return true;
        }

        return false;
    }

}
