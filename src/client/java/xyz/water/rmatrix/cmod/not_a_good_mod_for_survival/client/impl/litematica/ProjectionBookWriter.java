package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.BookPageTexts;

/**
 * Copies the book text stored in a projection into the held writable book.
 *
 * <p>The text is only handed to the vanilla book editor. Signing it there sends the
 * ordinary {@code BookUpdateC2SPacket}, so a server that never sees a schematic still
 * accepts the resulting written book. That packet only carries plain page strings,
 * which is why styled text is reduced to its characters.
 */
public final class ProjectionBookWriter {
    /** The vanilla editor's title field refuses to type once it reaches this length. */
    public static final int EDITABLE_TITLE_LENGTH = 16;
    private static final int REQUEST_VALID_TICKS = 5;

    @Nullable
    private static String pendingTitle;
    private static int pendingExpiryTick = -1;

    private ProjectionBookWriter() {
    }

    /**
     * Opens the vanilla book editor with the projected book text already typed in.
     * Returns whether the gesture was handled.
     */
    public static boolean tryCopyBookText(MinecraftClient client, ItemStack book) {
        if (client.player == null || client.currentScreen != null) {
            return false;
        }

        List<String> pages = BookPageTexts.toEditablePages(extractPages(client, book));

        if (BookPageTexts.isBlank(pages)) {
            ProjectionAidMessages.print("lecternNoBook");
            return true;
        }

        List<RawFilteredPair<String>> pairs = new ArrayList<>(pages.size());

        for (String page : pages) {
            pairs.add(RawFilteredPair.of(page));
        }

        WrittenBookContentComponent written = book.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        pendingTitle = written == null ? "" : written.title().get(client.shouldFilterText());
        pendingExpiryTick = client.player.age + REQUEST_VALID_TICKS;
        client.setScreen(new BookEditScreen(client.player, client.player.getMainHandStack(),
                Hand.MAIN_HAND, new WritableBookContentComponent(pairs)));
        ProjectionAidMessages.print("bookTextCopied", pages.size());
        return true;
    }

    /** Returns whether a book editor was just opened by this feature. */
    public static boolean hasPendingRequest(MinecraftClient client) {
        return pendingTitle != null && client.player != null && client.player.age <= pendingExpiryTick;
    }

    /**
     * Consumes the pending request and returns the title to pre-fill, or an empty string
     * when the projected title is unusable in the vanilla editor.
     */
    public static String consumePendingTitle() {
        String title = pendingTitle == null ? "" : pendingTitle.strip();
        pendingTitle = null;
        pendingExpiryTick = -1;

        return title.isEmpty() || title.length() >= EDITABLE_TITLE_LENGTH ? "" : title;
    }

    /** Clears a pending request that never reached a book editor. */
    public static void clearPendingRequest() {
        pendingTitle = null;
        pendingExpiryTick = -1;
    }

    private static List<String> extractPages(MinecraftClient client, ItemStack book) {
        List<String> pages = new ArrayList<>();
        WrittenBookContentComponent written = book.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

        if (written != null) {
            for (Text page : written.getPages(client.shouldFilterText())) {
                pages.add(page.getString());
            }

            return pages;
        }

        WritableBookContentComponent writable = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);

        if (writable != null) {
            pages.addAll(writable.stream(client.shouldFilterText()).toList());
        }

        return pages;
    }
}
