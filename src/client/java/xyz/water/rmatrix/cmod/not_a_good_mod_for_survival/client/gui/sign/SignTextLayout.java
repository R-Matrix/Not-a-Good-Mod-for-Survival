package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.sign;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Text layout helpers shared by the long-sign editor and the sign save path. */
public final class SignTextLayout {
    public static final int MAX_SIGN_LINES = 4;
    // UpdateSignC2SPacket/StringEncoding validates a Java String length of 384.
    public static final int MAX_SIGN_LINE_LENGTH = 384;
    // The editor intentionally keeps the total message limit at 384 as well.
    public static final int MAX_TOTAL_LENGTH = MAX_SIGN_LINE_LENGTH;

    private SignTextLayout() {
    }

    public static String joinSignLines(SignBlockEntity blockEntity, boolean front, boolean filtered) {
        Text[] messages = blockEntity.getText(front).getMessages(filtered);
        int lastNonEmptyLine = -1;

        for (int line = 0; line < messages.length; line++) {
            Text message = messages[line];
            if (!message.getString().isEmpty()) {
                lastNonEmptyLine = line;
            }
        }

        if (lastNonEmptyLine < 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int line = 0; line <= lastNonEmptyLine; line++) {
            if (line > 0) {
                result.append('\n');
            }
            result.append(messages[line].getString());
        }
        return result.toString();
    }

    public static LayoutResult wrapForSign(String text, TextRenderer textRenderer, int maxTextWidth) {
        int stringLength = text.length();
        if (stringLength > MAX_TOTAL_LENGTH) {
            return new LayoutResult(List.of(), 0, Failure.TOO_MANY_CHARACTERS);
        }

        List<VisualLine> visualLines = createVisualLines(text, textRenderer, maxTextWidth);
        if (visualLines.size() > MAX_SIGN_LINES) {
            return new LayoutResult(List.of(), visualLines.size(), Failure.TOO_MANY_LINES);
        }

        List<String> signLines = new ArrayList<>(MAX_SIGN_LINES);
        for (VisualLine visualLine : visualLines) {
            String line = text.substring(visualLine.start(), visualLine.end());
            if (line.length() > MAX_SIGN_LINE_LENGTH) {
                return new LayoutResult(List.of(), visualLines.size(), Failure.TOO_MANY_CHARACTERS);
            }
            signLines.add(line);
        }

        while (signLines.size() < MAX_SIGN_LINES) {
            signLines.add("");
        }

        return new LayoutResult(List.copyOf(signLines), visualLines.size(), Failure.NONE);
    }

    public static List<VisualLine> createVisualLines(String text, TextRenderer textRenderer, int maxTextWidth) {
        int width = Math.max(1, maxTextWidth);
        List<VisualLine> lines = new ArrayList<>();
        int paragraphStart = 0;

        for (int index = 0; index <= text.length(); index++) {
            if (index == text.length() || text.charAt(index) == '\n') {
                appendWrappedParagraph(lines, text, paragraphStart, index, textRenderer, width);
                paragraphStart = index + 1;
            }
        }

        return lines;
    }

    private static void appendWrappedParagraph(
            List<VisualLine> output,
            String text,
            int start,
            int end,
            TextRenderer textRenderer,
            int maxTextWidth
    ) {
        if (start == end) {
            output.add(new VisualLine(start, end));
            return;
        }

        int lineStart = start;
        while (lineStart < end) {
            int candidateEnd = lineStart;
            int lastFittingEnd = lineStart;
            int lastWhitespaceEnd = -1;

            while (candidateEnd < end) {
                int codePoint = text.codePointAt(candidateEnd);
                int nextEnd = candidateEnd + Character.charCount(codePoint);
                int candidateWidth = textRenderer.getWidth(text.substring(lineStart, nextEnd));

                if (candidateWidth > maxTextWidth && lastFittingEnd > lineStart) {
                    break;
                }

                lastFittingEnd = nextEnd;
                candidateEnd = nextEnd;
                if (Character.isWhitespace(codePoint)) {
                    lastWhitespaceEnd = nextEnd;
                }

                if (candidateWidth > maxTextWidth) {
                    break;
                }
            }

            int lineEnd = lastFittingEnd;
            if (lineEnd < end && lastWhitespaceEnd > lineStart) {
                lineEnd = lastWhitespaceEnd;
            }
            if (lineEnd <= lineStart) {
                lineEnd = lineStart + Character.charCount(text.codePointAt(lineStart));
            }

            output.add(new VisualLine(lineStart, lineEnd));
            lineStart = lineEnd;
        }
    }

    public record VisualLine(int start, int end) {
    }

    public record LayoutResult(List<String> lines, int requiredLines, Failure failure) {
        public boolean fits() {
            return this.failure == Failure.NONE;
        }
    }

    public enum Failure {
        NONE,
        TOO_MANY_LINES,
        TOO_MANY_CHARACTERS
    }
}
