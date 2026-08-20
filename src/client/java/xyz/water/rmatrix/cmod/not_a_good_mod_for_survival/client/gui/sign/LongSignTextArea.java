package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.sign;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** A small multiline text editor used by the long-sign editing screen. */
public final class LongSignTextArea {
    private static final int PADDING = 8;
    private static final int STATUS_GAP = 4;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int PLACEHOLDER_COLOR = 0xFF8B929C;
    private static final int CURSOR_COLOR = 0xFFFFFFFF;
    private static final int SELECTION_COLOR = 0xFF3F6EA8;

    private final TextRenderer textRenderer;
    private final int maxLength;
    private String text;
    private int cursor;
    private int selectionAnchor;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scrollLine;
    private int ticks;
    private int preferredCursorX = -1;
    private boolean focused;
    private boolean dragging;

    public LongSignTextArea(TextRenderer textRenderer, String text, int maxLength) {
        this.textRenderer = textRenderer;
        this.maxLength = maxLength;
        this.text = text.replace("\r", "");
        this.cursor = this.text.length();
        this.selectionAnchor = this.cursor;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.clampScroll();
    }

    public String getText() {
        return this.text;
    }

    /** Returns the Java string length used by the sign packet limit. */
    public int getTextLength() {
        return this.text.length();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void tick() {
        this.ticks++;
    }

    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            Text leftStatus,
            Text rightStatus,
            int leftStatusColor,
            int rightStatusColor
    ) {
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF11151B);
        this.drawBorder(context, 0xFF59616D);

        int contentWidth = Math.max(1, this.width - PADDING * 2);
        int lineHeight = this.textRenderer.fontHeight + 3;
        int visibleLines = this.getVisibleLineCount();
        List<SignTextLayout.VisualLine> lines = this.getVisualLines(contentWidth);
        this.ensureCursorVisible(lines, visibleLines);

        int selectionStart = Math.min(this.cursor, this.selectionAnchor);
        int selectionEnd = Math.max(this.cursor, this.selectionAnchor);
        int firstLine = this.scrollLine;
        int lastLine = Math.min(lines.size(), firstLine + visibleLines);

        for (int lineIndex = firstLine; lineIndex < lastLine; lineIndex++) {
            SignTextLayout.VisualLine line = lines.get(lineIndex);
            int lineY = this.y + PADDING + (lineIndex - firstLine) * lineHeight;
            int lineStart = line.start();
            int lineEnd = line.end();

            if (selectionStart != selectionEnd) {
                int highlightStart = Math.max(selectionStart, lineStart);
                int highlightEnd = Math.min(selectionEnd, lineEnd);
                if (highlightStart < highlightEnd
                        || (lineStart == lineEnd && selectionStart <= lineStart && selectionEnd > lineStart)) {
                    int startX = this.getTextX(line, highlightStart);
                    int endX = this.getTextX(line, highlightEnd);
                    if (startX == endX) {
                        endX++;
                    }
                    context.fill(startX, lineY - 1, endX, lineY + this.textRenderer.fontHeight + 1, SELECTION_COLOR);
                }
            }

            String lineText = this.text.substring(lineStart, lineEnd);
            if (!lineText.isEmpty()) {
                context.drawText(this.textRenderer, lineText, this.x + PADDING, lineY, TEXT_COLOR, false);
            }
        }

        if (this.text.isEmpty()) {
            context.drawText(
                    this.textRenderer,
                    Text.translatable("not-a-good-mod-for-survival.gui.long_sign_edit.placeholder"),
                    this.x + PADDING,
                    this.y + PADDING,
                    PLACEHOLDER_COLOR,
                    false
            );
        }

        if (this.focused && this.ticks / 6 % 2 == 0) {
            int cursorLine = this.findLineForCursor(lines);
            if (cursorLine >= firstLine && cursorLine < lastLine) {
                SignTextLayout.VisualLine line = lines.get(cursorLine);
                int cursorX = this.getTextX(line, this.cursor);
                int cursorY = this.y + PADDING + (cursorLine - firstLine) * lineHeight;
                context.fill(cursorX, cursorY - 1, cursorX + 1, cursorY + this.textRenderer.fontHeight + 1, CURSOR_COLOR);
            }
        }

        if (lines.size() > visibleLines) {
            int scrollbarX = this.x + this.width - 4;
            int trackTop = this.y + PADDING;
            int trackBottom = this.getStatusY() - STATUS_GAP;
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(12, trackHeight * visibleLines / lines.size());
            int maxScroll = lines.size() - visibleLines;
            int thumbY = trackTop + (trackHeight - thumbHeight) * this.scrollLine / Math.max(1, maxScroll);
            context.fill(scrollbarX, trackTop, scrollbarX + 2, trackBottom, 0xFF252B33);
            context.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbHeight, 0xFF8993A1);
        }

        int statusY = this.getStatusY();
        context.drawText(this.textRenderer, leftStatus, this.x + PADDING, statusY, leftStatusColor, false);
        int rightStatusX = this.x + this.width - PADDING - this.textRenderer.getWidth(rightStatus);
        context.drawText(this.textRenderer, rightStatus, rightStatusX, statusY, rightStatusColor, false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) {
            return false;
        }

        if (Screen.isSelectAll(keyCode)) {
            this.selectionAnchor = 0;
            this.cursor = this.text.length();
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            this.copySelection();
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            this.insert(SelectionManager.getClipboard(MinecraftClient.getInstance()));
            return true;
        }
        if (Screen.isCut(keyCode)) {
            this.copySelection();
            this.deleteSelection();
            return true;
        }

        boolean shift = Screen.hasShiftDown();
        boolean control = Screen.hasControlDown();
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                this.deleteBackward();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                this.deleteForward();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.insert("\n");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                this.moveHorizontal(-1, shift, control);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                this.moveHorizontal(1, shift, control);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                this.moveVertical(-1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                this.moveVertical(1, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (control) {
                    this.setCursor(0, shift);
                } else {
                    this.moveToLineEdge(false, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (control) {
                    this.setCursor(this.text.length(), shift);
                } else {
                    this.moveToLineEdge(true, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                this.insert("    ");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!this.focused || !StringHelper.isValidChar(chr)) {
            return false;
        }

        this.insert(Character.toString(chr));
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        List<SignTextLayout.VisualLine> lines = this.getVisualLines(Math.max(1, this.width - PADDING * 2));
        int lineHeight = this.textRenderer.fontHeight + 3;
        int visibleLines = this.getVisibleLineCount();
        int lineIndex = this.scrollLine + (int) ((mouseY - this.y - PADDING) / lineHeight);
        lineIndex = Math.max(this.scrollLine, Math.min(lineIndex, Math.min(lines.size() - 1, this.scrollLine + visibleLines - 1)));

        this.focused = true;
        this.dragging = true;
        int position = this.findCursorAtX(lines.get(lineIndex), (int) mouseX);
        this.setCursor(position, Screen.hasShiftDown());
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!this.dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        List<SignTextLayout.VisualLine> lines = this.getVisualLines(Math.max(1, this.width - PADDING * 2));
        int lineHeight = this.textRenderer.fontHeight + 3;
        int visibleLines = this.getVisibleLineCount();
        int lineIndex = this.scrollLine + (int) ((mouseY - this.y - PADDING) / lineHeight);
        lineIndex = Math.max(0, Math.min(lineIndex, Math.min(lines.size() - 1, this.scrollLine + visibleLines - 1)));
        this.cursor = this.findCursorAtX(lines.get(lineIndex), (int) mouseX);
        this.preferredCursorX = -1;
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean wasDragging = this.dragging;
            this.dragging = false;
            return wasDragging;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        this.scrollLine -= (int) Math.signum(verticalAmount) * 3;
        this.clampScroll();
        return true;
    }

    private void insert(String insertion) {
        insertion = insertion.replace("\r", "");
        int start = Math.min(this.cursor, this.selectionAnchor);
        int end = Math.max(this.cursor, this.selectionAnchor);
        String candidate = this.text.substring(0, start) + insertion + this.text.substring(end);
        if (candidate.length() > this.maxLength) {
            return;
        }

        this.text = candidate;
        this.cursor = start + insertion.length();
        this.selectionAnchor = this.cursor;
        this.preferredCursorX = -1;
    }

    private void deleteBackward() {
        if (this.deleteSelection()) {
            return;
        }
        if (this.cursor > 0) {
            int previous = this.text.offsetByCodePoints(this.cursor, -1);
            this.text = this.text.substring(0, previous) + this.text.substring(this.cursor);
            this.cursor = previous;
            this.selectionAnchor = previous;
            this.preferredCursorX = -1;
        }
    }

    private void deleteForward() {
        if (this.deleteSelection()) {
            return;
        }
        if (this.cursor < this.text.length()) {
            int next = this.text.offsetByCodePoints(this.cursor, 1);
            this.text = this.text.substring(0, this.cursor) + this.text.substring(next);
            this.selectionAnchor = this.cursor;
            this.preferredCursorX = -1;
        }
    }

    private boolean deleteSelection() {
        if (this.cursor == this.selectionAnchor) {
            return false;
        }
        int start = Math.min(this.cursor, this.selectionAnchor);
        int end = Math.max(this.cursor, this.selectionAnchor);
        this.text = this.text.substring(0, start) + this.text.substring(end);
        this.cursor = start;
        this.selectionAnchor = start;
        this.preferredCursorX = -1;
        return true;
    }

    private void copySelection() {
        if (this.cursor == this.selectionAnchor) {
            return;
        }
        int start = Math.min(this.cursor, this.selectionAnchor);
        int end = Math.max(this.cursor, this.selectionAnchor);
        SelectionManager.setClipboard(MinecraftClient.getInstance(), this.text.substring(start, end));
    }

    private void moveHorizontal(int direction, boolean shift, boolean byWord) {
        int position = this.cursor;
        if (!shift && this.cursor != this.selectionAnchor) {
            position = direction < 0
                    ? Math.min(this.cursor, this.selectionAnchor)
                    : Math.max(this.cursor, this.selectionAnchor);
        } else if (byWord) {
            position = TextHandler.moveCursorByWords(this.text, direction, this.cursor, true);
        } else {
            if (direction < 0) {
                position = this.cursor > 0
                        ? this.text.offsetByCodePoints(this.cursor, -1)
                        : 0;
            } else {
                position = this.cursor < this.text.length()
                        ? this.text.offsetByCodePoints(this.cursor, 1)
                        : this.text.length();
            }
        }

        this.setCursor(position, shift);
        this.preferredCursorX = -1;
    }

    private void moveVertical(int direction, boolean shift) {
        List<SignTextLayout.VisualLine> lines = this.getVisualLines(Math.max(1, this.width - PADDING * 2));
        int currentLine = this.findLineForCursor(lines);
        int targetLine = Math.max(0, Math.min(lines.size() - 1, currentLine + direction));
        if (targetLine == currentLine) {
            return;
        }

        SignTextLayout.VisualLine current = lines.get(currentLine);
        if (this.preferredCursorX < 0) {
            this.preferredCursorX = this.getTextX(current, this.cursor);
        }
        this.setCursor(this.findCursorAtX(lines.get(targetLine), this.preferredCursorX), shift);
    }

    private void moveToLineEdge(boolean end, boolean shift) {
        List<SignTextLayout.VisualLine> lines = this.getVisualLines(Math.max(1, this.width - PADDING * 2));
        SignTextLayout.VisualLine line = lines.get(this.findLineForCursor(lines));
        this.setCursor(end ? line.end() : line.start(), shift);
        this.preferredCursorX = -1;
    }

    private void setCursor(int position, boolean extendSelection) {
        this.cursor = Math.max(0, Math.min(position, this.text.length()));
        if (!extendSelection) {
            this.selectionAnchor = this.cursor;
        }
    }

    private List<SignTextLayout.VisualLine> getVisualLines(int contentWidth) {
        return SignTextLayout.createVisualLines(this.text, this.textRenderer, contentWidth);
    }

    private int findLineForCursor(List<SignTextLayout.VisualLine> lines) {
        for (int index = 0; index < lines.size(); index++) {
            SignTextLayout.VisualLine line = lines.get(index);
            if (this.cursor >= line.start() && this.cursor <= line.end()) {
                return index;
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    private int findCursorAtX(SignTextLayout.VisualLine line, int mouseX) {
        int contentLeft = this.x + PADDING;
        int relativeX = Math.max(0, mouseX - contentLeft);
        int position = line.start();

        while (position < line.end()) {
            int next = this.text.offsetByCodePoints(position, 1);
            int startWidth = this.textRenderer.getWidth(this.text.substring(line.start(), position));
            int endWidth = this.textRenderer.getWidth(this.text.substring(line.start(), next));
            if (relativeX < (startWidth + endWidth) / 2) {
                return position;
            }
            position = next;
        }

        return line.end();
    }

    private int getTextX(SignTextLayout.VisualLine line, int position) {
        int clamped = Math.max(line.start(), Math.min(position, line.end()));
        return this.x + PADDING + this.textRenderer.getWidth(this.text.substring(line.start(), clamped));
    }

    private void ensureCursorVisible(List<SignTextLayout.VisualLine> lines, int visibleLines) {
        int cursorLine = this.findLineForCursor(lines);
        if (cursorLine < this.scrollLine) {
            this.scrollLine = cursorLine;
        } else if (cursorLine >= this.scrollLine + visibleLines) {
            this.scrollLine = cursorLine - visibleLines + 1;
        }
        this.clampScroll();
    }

    private void clampScroll() {
        int visibleLines = this.getVisibleLineCount();
        int lineCount = this.getVisualLines(Math.max(1, this.width - PADDING * 2)).size();
        this.scrollLine = Math.max(0, Math.min(this.scrollLine, Math.max(0, lineCount - visibleLines)));
    }

    private int getVisibleLineCount() {
        int lineHeight = this.textRenderer.fontHeight + 3;
        int contentHeight = this.getStatusY() - STATUS_GAP - (this.y + PADDING);
        return Math.max(1, contentHeight / lineHeight);
    }

    private int getStatusY() {
        return this.y + this.height - PADDING - this.textRenderer.fontHeight;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x
                && mouseX < this.x + this.width
                && mouseY >= this.y
                && mouseY < this.y + this.height;
    }

    private void drawBorder(DrawContext context, int color) {
        context.fill(this.x, this.y, this.x + this.width, this.y + 1, color);
        context.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, color);
        context.fill(this.x, this.y, this.x + 1, this.y + this.height, color);
        context.fill(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, color);
    }
}
