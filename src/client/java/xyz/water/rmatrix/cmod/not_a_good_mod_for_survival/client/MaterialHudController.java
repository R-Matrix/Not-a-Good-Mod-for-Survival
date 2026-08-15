package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Keeps the custom renderer attached to Litematica's current material list. */
public final class MaterialHudController {
    private static MaterialListBase currentMaterialList;
    private static OpenZenMaterialHudRenderer renderer;
    private static KeyBinding editHudKey;

    private MaterialHudController() {
    }

    public static void initialize() {
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.not_a_good_mod_for_survival.edit_material_hud",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_H,
                "category.not_a_good_mod_for_survival"
        ));
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

        if (editHudKey != null && editHudKey.wasPressed()
                && client.currentScreen == null && client.player != null && renderer != null) {
            client.setScreen(new MaterialListClickGuiScreen(renderer));
        }
    }

    public static boolean matchesEditKey(int keyCode, int scanCode) {
        return editHudKey != null && editHudKey.matchesKey(keyCode, scanCode);
    }

}
