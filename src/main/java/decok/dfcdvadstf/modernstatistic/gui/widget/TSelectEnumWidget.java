package decok.dfcdvadstf.modernstatistic.gui.widget;

import decok.dfcdvadstf.modernstatistic.gui.TElement;

/**
 * A button that cycles through enum values — used for tab selection.
 */
public class TSelectEnumWidget<T extends Enum<T>> extends TElement {

    public interface SelectionChangeListener {
        void onSelectionChanged(Enum<?> newValue);
    }

    public interface EnumLabelProvider {
        String getLabel(Enum<?> value);
    }

    protected T[] values;
    protected int selectedIndex = 0;
    protected SelectionChangeListener listener;
    protected EnumLabelProvider labelProvider;

    private static final int COLOR_BG = 0xCC000000;
    private static final int COLOR_ARROW_NORMAL = 0xFF888888;
    private static final int COLOR_ARROW_HOVERED = 0xFFFFFFFF;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_TEXT = 0xE0E0E0;

    @SuppressWarnings("unchecked")
    public TSelectEnumWidget(int x, int y, int width, int height, Class<T> enumClass) {
        super(x, y, width, height);
        this.values = enumClass.getEnumConstants();
    }

    public T getSelected() { return values[selectedIndex]; }

    public void setSelected(T value, boolean notify) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                selectedIndex = i;
                if (notify && listener != null) listener.onSelectionChanged(value);
                return;
            }
        }
    }

    public void setOnSelectionChange(SelectionChangeListener listener) {
        this.listener = listener;
    }

    public void setLabelProvider(EnumLabelProvider provider) {
        this.labelProvider = provider;
    }

    private String getDisplayLabel() {
        T val = values[selectedIndex];
        if (labelProvider != null) return labelProvider.getLabel(val);
        return val.name();
    }

    private void next() {
        selectedIndex = (selectedIndex + 1) % values.length;
        if (listener != null) listener.onSelectionChanged(values[selectedIndex]);
    }

    private void prev() {
        selectedIndex = (selectedIndex - 1 + values.length) % values.length;
        if (listener != null) listener.onSelectionChanged(values[selectedIndex]);
    }

    @Override
    protected void renderSelf(int mouseX, int mouseY, float partialTicks) {
        // Background
        fill(x, y, getEndX(), getEndY(), COLOR_BG);

        // Left arrow
        int arrW = 14;
        int arrowColor = COLOR_ARROW_NORMAL;
        int leftArrowCX = x + 7;
        int rightArrowCX = getEndX() - 7;
        int arrowCY = y + height / 2;

        if (mouseX >= x && mouseX < x + arrW) arrowColor = COLOR_ARROW_HOVERED;
        drawString(getFontRenderer(), "<", leftArrowCX - 2, arrowCY - 4, arrowColor);

        // Label
        drawCenteredString(getFontRenderer(), getDisplayLabel(),
                x + width / 2, y + (height - 8) / 2, COLOR_TEXT);

        // Right arrow
        arrowColor = COLOR_ARROW_NORMAL;
        if (mouseX >= getEndX() - arrW && mouseX < getEndX()) arrowColor = COLOR_ARROW_HOVERED;
        drawString(getFontRenderer(), ">", rightArrowCX - 2, arrowCY - 4, arrowColor);
    }

    @Override
    protected void postRenderSelf(int mouseX, int mouseY, float partialTicks) {
        drawOutline(x, y, getEndX(), getEndY(), COLOR_OUTLINE);
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        int arrW = 14;
        if (mouseX >= x && mouseX < x + arrW) {
            prev();
            return true;
        }
        if (mouseX >= getEndX() - arrW && mouseX < getEndX()) {
            next();
            return true;
        }
        next(); // click center = next
        return true;
    }
}
