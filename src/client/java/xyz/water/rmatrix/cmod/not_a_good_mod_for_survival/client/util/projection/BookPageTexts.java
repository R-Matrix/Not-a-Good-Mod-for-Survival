package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts schematic book pages to the plain-text form the vanilla writable-book
 * flow can carry. A signed book only ever travels as plain strings per page, so
 * styled text is reduced to its characters and over-long pages are wrapped.
 */
public final class BookPageTexts {
    /** The vanilla writable-book page length limit. */
    public static final int MAX_PAGE_LENGTH = 1024;
    /** The vanilla writable-book page count limit. */
    public static final int MAX_PAGE_COUNT = 100;

    private BookPageTexts() {
    }

    /** Normalizes raw page text to what can be handed to the vanilla book editor. */
    public static List<String> toEditablePages(List<String> rawPages) {
        return toEditablePages(rawPages, MAX_PAGE_COUNT, MAX_PAGE_LENGTH);
    }

    /**
     * Normalizes raw page text with explicit limits. Over-long pages are wrapped
     * instead of truncated, trailing empty pages are dropped and the result always
     * holds at least one page.
     */
    public static List<String> toEditablePages(List<String> rawPages, int maxPages, int maxPageLength) {
        int pageLimit = Math.max(1, maxPages);
        int lengthLimit = Math.max(1, maxPageLength);
        List<String> pages = new ArrayList<>();

        if (rawPages != null) {
            for (String raw : rawPages) {
                if (pages.size() >= pageLimit) {
                    break;
                }

                for (String part : splitPage(sanitize(raw), lengthLimit)) {
                    if (pages.size() >= pageLimit) {
                        break;
                    }

                    pages.add(part);
                }
            }
        }

        while (pages.size() > 1 && pages.get(pages.size() - 1).isEmpty()) {
            pages.remove(pages.size() - 1);
        }

        if (pages.isEmpty()) {
            pages.add("");
        }

        return pages;
    }

    /** Returns whether there is no readable text on any page. */
    public static boolean isBlank(List<String> pages) {
        if (pages == null || pages.isEmpty()) {
            return true;
        }

        for (String page : pages) {
            if (page != null && !page.isBlank()) {
                return false;
            }
        }

        return true;
    }

    /** Normalizes line endings and drops characters the book codec cannot carry. */
    public static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        return raw.replace("\r\n", "\n").replace('\r', '\n').replace("\0", "");
    }

    /** Splits one page into chunks of at most {@code maxPageLength} characters. */
    public static List<String> splitPage(String page, int maxPageLength) {
        List<String> parts = new ArrayList<>();

        if (page == null || page.isEmpty()) {
            parts.add("");
            return parts;
        }

        int lengthLimit = Math.max(1, maxPageLength);
        int start = 0;

        while (start < page.length()) {
            int end = Math.min(start + lengthLimit, page.length());

            if (end < page.length()) {
                int lastSpace = page.lastIndexOf(' ', end - 1);

                if (lastSpace > start) {
                    end = lastSpace + 1;
                }
            }

            parts.add(page.substring(start, end));
            start = end;
        }

        return parts;
    }
}
