package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Text matching for global search, including full pinyin and pinyin initials. */
public final class GlobalSearchText {
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = createPinyinFormat();
    private static final Map<String, SearchForms> FORMS_CACHE = new ConcurrentHashMap<>();

    private GlobalSearchText() {
    }

    public static boolean contains(String source, String query) {
        return !findMatches(source, query, GlobalSearchSettings.isPinyinSearchEnabled()).isEmpty();
    }

    /**
     * Returns source-text spans matching either the original text or its pinyin.
     * Pinyin matches are mapped back to the Chinese characters that produced them.
     */
    public static List<Match> findMatches(String source, String query) {
        return findMatches(source, query, GlobalSearchSettings.isPinyinSearchEnabled());
    }

    public static List<Match> findMatches(String source, String query, boolean enablePinyin) {
        if (source == null || source.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<Match> matches = new ArrayList<>();
        addDirectMatches(source, normalizedQuery, matches);

        if (enablePinyin && isPinyinQuery(normalizedQuery)) {
            SearchForms forms = FORMS_CACHE.computeIfAbsent(source, GlobalSearchText::createSearchForms);
            String pinyinQuery = normalizePinyinQuery(normalizedQuery);
            addIndexedMatches(forms.fullPinyin(), pinyinQuery, matches);
            addIndexedMatches(forms.initials(), pinyinQuery, matches);
        }

        return mergeMatches(matches);
    }

    private static void addDirectMatches(String source, String query, List<Match> matches) {
        for (int start = 0; start <= source.length() - query.length(); start++) {
            if (source.regionMatches(true, start, query, 0, query.length())) {
                matches.add(new Match(start, start + query.length()));
            }
        }
    }

    private static void addIndexedMatches(IndexedText indexedText, String query, List<Match> matches) {
        if (query.isEmpty()) {
            return;
        }

        String searchableText = indexedText.text();
        int start = searchableText.indexOf(query);

        while (start >= 0) {
            int end = start + query.length();
            int sourceStart = Integer.MAX_VALUE;
            int sourceEnd = -1;

            for (int index = start; index < end; index++) {
                sourceStart = Math.min(sourceStart, indexedText.sourceStarts()[index]);
                sourceEnd = Math.max(sourceEnd, indexedText.sourceEnds()[index]);
            }

            if (sourceEnd > sourceStart) {
                matches.add(new Match(sourceStart, sourceEnd));
            }

            start = searchableText.indexOf(query, start + 1);
        }
    }

    private static List<Match> mergeMatches(List<Match> matches) {
        if (matches.isEmpty()) {
            return List.of();
        }

        matches.sort(Comparator.comparingInt(Match::start).thenComparingInt(Match::end));
        List<Match> merged = new ArrayList<>();
        Match current = matches.get(0);

        for (int index = 1; index < matches.size(); index++) {
            Match next = matches.get(index);

            if (next.start() <= current.end()) {
                current = new Match(current.start(), Math.max(current.end(), next.end()));
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return List.copyOf(merged);
    }

    private static SearchForms createSearchForms(String source) {
        IndexBuilder fullPinyin = new IndexBuilder();
        IndexBuilder initials = new IndexBuilder();

        for (int offset = 0; offset < source.length();) {
            int codePoint = source.codePointAt(offset);
            int end = offset + Character.charCount(codePoint);
            String pinyin = getPinyin(codePoint);

            if (pinyin != null) {
                fullPinyin.append(pinyin, offset, end);
                initials.append(pinyin.substring(0, 1), offset, end);
            } else if (isAsciiLetterOrDigit(codePoint)) {
                String original = new String(Character.toChars(codePoint)).toLowerCase(Locale.ROOT);
                fullPinyin.append(original, offset, end);
                initials.append(original, offset, end);
            }

            offset = end;
        }

        return new SearchForms(fullPinyin.build(), initials.build());
    }

    private static String getPinyin(int codePoint) {
        if (codePoint > Character.MAX_VALUE) {
            return null;
        }

        try {
            String[] values = PinyinHelper.toHanyuPinyinStringArray((char) codePoint, PINYIN_FORMAT);

            if (values != null && values.length > 0 && !values[0].isBlank()) {
                return normalizePinyinQuery(values[0]);
            }
        } catch (BadHanyuPinyinOutputFormatCombination ignored) {
            // The format is fixed above, so this should never happen. Treat it as non-pinyin text if it does.
        }

        return null;
    }

    private static boolean isPinyinQuery(String query) {
        if (query.length() < 2) {
            return false;
        }

        boolean hasLetter = false;

        for (int index = 0; index < query.length(); index++) {
            char character = query.charAt(index);

            if ((character >= 'a' && character <= 'z') || character == '\u00FC') {
                hasLetter = true;
            } else if (character < '0' || character > '9') {
                return false;
            }
        }

        return hasLetter;
    }

    private static String normalizePinyinQuery(String value) {
        return value.toLowerCase(Locale.ROOT).replace('\u00FC', 'v');
    }

    private static boolean isAsciiLetterOrDigit(int codePoint) {
        return (codePoint >= 'a' && codePoint <= 'z') ||
                (codePoint >= 'A' && codePoint <= 'Z') ||
                (codePoint >= '0' && codePoint <= '9');
    }

    private static HanyuPinyinOutputFormat createPinyinFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        return format;
    }

    public record Match(int start, int end) {
    }

    private record SearchForms(IndexedText fullPinyin, IndexedText initials) {
    }

    private record IndexedText(String text, int[] sourceStarts, int[] sourceEnds) {
    }

    private static final class IndexBuilder {
        private final StringBuilder text = new StringBuilder();
        private final List<Integer> sourceStarts = new ArrayList<>();
        private final List<Integer> sourceEnds = new ArrayList<>();

        private void append(String value, int sourceStart, int sourceEnd) {
            this.text.append(value);

            for (int index = 0; index < value.length(); index++) {
                this.sourceStarts.add(sourceStart);
                this.sourceEnds.add(sourceEnd);
            }
        }

        private IndexedText build() {
            int[] starts = new int[this.sourceStarts.size()];
            int[] ends = new int[this.sourceEnds.size()];

            for (int index = 0; index < starts.length; index++) {
                starts[index] = this.sourceStarts.get(index);
                ends[index] = this.sourceEnds.get(index);
            }

            return new IndexedText(this.text.toString(), starts, ends);
        }
    }
}
