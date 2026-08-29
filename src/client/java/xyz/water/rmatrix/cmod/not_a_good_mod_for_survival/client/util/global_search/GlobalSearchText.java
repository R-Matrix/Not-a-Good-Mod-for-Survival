package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Text matching for global search, including full pinyin and pinyin initials. */
public final class GlobalSearchText {
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = createPinyinFormat();
    private static final Map<String, SearchForms> FORMS_CACHE = new ConcurrentHashMap<>();

    private GlobalSearchText() {
    }

    public static boolean contains(String source, String query) {
        return hasMatch(source, query, GlobalSearchSettings.isPinyinSearchEnabled());
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
            searchPinyinMatches(forms.tokens(), pinyinQuery, false, matches);
            searchPinyinMatches(forms.tokens(), pinyinQuery, true, matches);
        }

        return mergeMatches(matches);
    }

    /** Existence-only counterpart of {@link #findMatches} that stops at the first hit. */
    public static boolean hasMatch(String source, String query, boolean enablePinyin) {
        if (source == null || source.isEmpty() || query == null || query.isBlank()) {
            return false;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);

        if (hasDirectMatch(source, normalizedQuery)) {
            return true;
        }

        if (enablePinyin && isPinyinQuery(normalizedQuery)) {
            SearchForms forms = FORMS_CACHE.computeIfAbsent(source, GlobalSearchText::createSearchForms);
            String pinyinQuery = normalizePinyinQuery(normalizedQuery);

            return searchPinyinMatches(forms.tokens(), pinyinQuery, false, null)
                    || searchPinyinMatches(forms.tokens(), pinyinQuery, true, null);
        }

        return false;
    }

    private static boolean hasDirectMatch(String source, String query) {
        for (int start = 0; start <= source.length() - query.length(); start++) {
            if (source.regionMatches(true, start, query, 0, query.length())) {
                return true;
            }
        }

        return false;
    }

    private static void addDirectMatches(String source, String query, List<Match> matches) {
        for (int start = 0; start <= source.length() - query.length(); start++) {
            if (source.regionMatches(true, start, query, 0, query.length())) {
                matches.add(new Match(start, start + query.length()));
            }
        }
    }

    /**
     * Searches pinyin spans. A null {@code matches} list selects existence-only mode where
     * the first completed match short-circuits the whole traversal; otherwise every match
     * is collected and the traversal always runs to completion.
     */
    private static boolean searchPinyinMatches(
            List<PinyinToken> tokens,
            String query,
            boolean initials,
            List<Match> matches
    ) {
        if (query.isEmpty()) {
            return false;
        }

        for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
            PinyinToken token = tokens.get(tokenIndex);
            List<String> pronunciations = initials ? token.initials() : token.pinyins();

            for (int pronunciationIndex = 0; pronunciationIndex < pronunciations.size(); pronunciationIndex++) {
                String pronunciation = pronunciations.get(pronunciationIndex);

                for (int characterOffset = 0; characterOffset < pronunciation.length(); characterOffset++) {
                    if (pronunciation.charAt(characterOffset) != query.charAt(0)) {
                        continue;
                    }

                    if (matchPinyin(
                            tokens, query, initials, tokenIndex, pronunciationIndex, characterOffset,
                            0, token.sourceStart(), new HashSet<>(), matches)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean matchPinyin(
            List<PinyinToken> tokens,
            String query,
            boolean initials,
            int tokenIndex,
            int pronunciationIndex,
            int characterOffset,
            int queryOffset,
            int sourceStart,
            Set<PinyinMatchState> visited,
            List<Match> matches
    ) {
        if (tokenIndex >= tokens.size()) {
            return false;
        }

        PinyinMatchState state = new PinyinMatchState(
                tokenIndex, pronunciationIndex, characterOffset, queryOffset);

        if (!visited.add(state)) {
            return false;
        }

        PinyinToken token = tokens.get(tokenIndex);
        List<String> pronunciations = initials ? token.initials() : token.pinyins();

        if (pronunciationIndex >= pronunciations.size()) {
            return false;
        }

        String pronunciation = pronunciations.get(pronunciationIndex);
        int charactersToMatch = Math.min(
                pronunciation.length() - characterOffset, query.length() - queryOffset);

        if (charactersToMatch <= 0 || !pronunciation.regionMatches(
                characterOffset, query, queryOffset, charactersToMatch)) {
            return false;
        }

        int nextQueryOffset = queryOffset + charactersToMatch;
        int nextCharacterOffset = characterOffset + charactersToMatch;

        if (nextQueryOffset == query.length()) {
            if (matches == null) {
                return true;
            }

            matches.add(new Match(sourceStart, token.sourceEnd()));
            return false;
        }

        if (nextCharacterOffset < pronunciation.length() || tokenIndex + 1 >= tokens.size()) {
            return false;
        }

        PinyinToken nextToken = tokens.get(tokenIndex + 1);
        List<String> nextPronunciations = initials ? nextToken.initials() : nextToken.pinyins();

        for (int nextPronunciationIndex = 0;
             nextPronunciationIndex < nextPronunciations.size();
             nextPronunciationIndex++) {
            if (matchPinyin(
                    tokens, query, initials, tokenIndex + 1, nextPronunciationIndex, 0,
                    nextQueryOffset, sourceStart, visited, matches)) {
                return true;
            }
        }

        return false;
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
        List<PinyinToken> tokens = new ArrayList<>();

        for (int offset = 0; offset < source.length();) {
            int codePoint = source.codePointAt(offset);
            int end = offset + Character.charCount(codePoint);
            List<String> pinyins = getPinyins(codePoint);

            if (!pinyins.isEmpty()) {
                List<String> initials = pinyins.stream()
                        .map(pinyin -> pinyin.substring(0, 1))
                        .distinct()
                        .toList();
                tokens.add(new PinyinToken(offset, end, pinyins, initials));
            } else if (isAsciiLetterOrDigit(codePoint)) {
                String original = new String(Character.toChars(codePoint)).toLowerCase(Locale.ROOT);
                tokens.add(new PinyinToken(offset, end, List.of(original), List.of(original)));
            }

            offset = end;
        }

        return new SearchForms(List.copyOf(tokens));
    }

    private static List<String> getPinyins(int codePoint) {
        if (codePoint > Character.MAX_VALUE) {
            return List.of();
        }

        try {
            String[] values = PinyinHelper.toHanyuPinyinStringArray((char) codePoint, PINYIN_FORMAT);

            if (values == null || values.length == 0) {
                return List.of();
            }

            Set<String> pinyins = new LinkedHashSet<>();

            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    pinyins.add(normalizePinyinQuery(value));
                }
            }

            return List.copyOf(pinyins);
        } catch (BadHanyuPinyinOutputFormatCombination exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not convert code point {} to pinyin.", codePoint, exception);
        }

        return List.of();
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

    private record SearchForms(List<PinyinToken> tokens) {
    }

    private record PinyinToken(
            int sourceStart,
            int sourceEnd,
            List<String> pinyins,
            List<String> initials
    ) {
    }

    private record PinyinMatchState(
            int tokenIndex,
            int pronunciationIndex,
            int characterOffset,
            int queryOffset
    ) {
    }
}
