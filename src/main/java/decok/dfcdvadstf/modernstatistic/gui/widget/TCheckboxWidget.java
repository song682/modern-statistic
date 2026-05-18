package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.gui.TElement;

/**
 * Checkbox widget with label.
 */
public class TCheckboxWidget extends TElement {

    public interface CheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    protected String label;
    protected boolean checked;
    protected CheckedChangeListener listener;

    private static final int COLOR_CHECK_BG = 0xCC000000;
    private static final int COLOR_CHECK_FILL = 0xFFFFFFFF;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_TEXT = 0xE0E0E0;
    private static final int CHECK_SIZE = 10;

    public TCheckboxWidget(int x, int y, int width, int height, String label, boolean initialChecked) {
        super(x, y, width, height);
        this.label = label;
        this.checked = initialChecked;
    }

    public boolean isChecked() { return checked; }

    public void setChecked(boolean checked) { this.checked = checked; }

    public void setCheckedChangeListener(CheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Checkbox
        int boxY = y + (height - CHECK_SIZE) / 2;
        fill(x, boxY, x + CHECK_SIZE, boxY + CHECK_SIZE, COLOR_CHECK_BG);
        drawOutline(x, boxY, x + CHECK_SIZE, boxY + CHECK_SIZE, COLOR_OUTLINE);
        if (checked) {
            fill(x + 2, boxY + 2, x + CHECK_SIZE - 2, boxY + CHECK_SIZE - 2, COLOR_CHECK_FILL);
        }

        // Label
        if (label != null && !label.isEmpty()) {
            drawString(getFontRenderer(), label, x + CHECK_SIZE + 4,
                    y + (height - 8) / 2, COLOR_TEXT);
        }
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            checked = !checked;
            if (listener != null) listener.onCheckedChanged(checked);
            return true;
        }
        return false;
    }
}
