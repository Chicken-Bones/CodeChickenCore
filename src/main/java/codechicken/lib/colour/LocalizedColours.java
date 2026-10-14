package codechicken.lib.colour;

import net.minecraft.util.StatCollector;

public final class LocalizedColours {

    private static final String KEY_PREFIX = "codechickencore.color.";

    public static int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;
    public static int BUTTON_TEXT_HOVER = 0xFFFFFFA0;
    public static int BUTTON_TEXT = 0xFFE0E0E0;
    public static int TEXT_FIELD_BORDER = 0xFFA0A0A0;
    public static int TEXT_FIELD_BACKGROUND = 0xFF000000;
    public static int TEXT_FIELD_TEXT = 0xE0E0E0;
    public static int TEXT_FIELD_TEXT_DISABLED = 0x707070;
    public static int SCROLL_PANE_BACKGROUND = 0xFF000000;
    public static int SCROLL_PANE_OVERLAY_TOP = 0xFFA0A0A0;
    public static int SCROLL_PANE_OVERLAY_BOTTOM = 0xFFA0A0A0;
    public static int SCROLL_PANE_OVERLAY_LEFT = 0xFFA0A0A0;
    public static int SCROLL_PANE_OVERLAY_RIGHT = 0xFFA0A0A0;
    public static int SCROLLBAR_CORNERS = 0xFF8B8B8B;
    public static int SCROLLBAR_TOP_LEFT = 0xFFF0F0F0;
    public static int SCROLLBAR_BOTTOM_RIGHT = 0xFF555555;
    public static int SCROLLBAR_FILL = 0xFFC6C6C6;
    public static int SCROLLBAR_GUIDE = 0xFF808080;
    public static int TOOLTIP_BG_START = 0xF0100010;
    public static int TOOLTIP_BG_END = 0xF0100010;
    public static int TOOLTIP_BORDER_START = 0x505000FF;
    public static int TOOLTIP_BORDER_END = 0x5028007F;
    public static int TOOLTIP_TEXT = 0xFFFFFFFF;
    public static int ITEM_QUANTITY_TEXT = 0xFFFFFF;
    public static int MOD_DESCRIPTION_TEXT = 0xDDDDDD;

    private LocalizedColours() {}

    public static void reloadLocalizedColours() {
        BUTTON_TEXT_DISABLED = getLocalizedColor("buttonTextDisabled", BUTTON_TEXT_DISABLED);
        BUTTON_TEXT_HOVER = getLocalizedColor("buttonTextHover", BUTTON_TEXT_HOVER);
        BUTTON_TEXT = getLocalizedColor("buttonText", BUTTON_TEXT);
        TEXT_FIELD_BORDER = getLocalizedColor("textFieldBorder", TEXT_FIELD_BORDER);
        TEXT_FIELD_BACKGROUND = getLocalizedColor("textFieldBackground", TEXT_FIELD_BACKGROUND);
        TEXT_FIELD_TEXT = getLocalizedColor("textFieldText", TEXT_FIELD_TEXT);
        TEXT_FIELD_TEXT_DISABLED = getLocalizedColor("textFieldTextDisabled", TEXT_FIELD_TEXT_DISABLED);
        SCROLL_PANE_BACKGROUND = getLocalizedColor("background", SCROLL_PANE_BACKGROUND);
        SCROLL_PANE_OVERLAY_TOP = getLocalizedColor("overlayTop", SCROLL_PANE_OVERLAY_TOP);
        SCROLL_PANE_OVERLAY_BOTTOM = getLocalizedColor("overlayBottom", SCROLL_PANE_OVERLAY_BOTTOM);
        SCROLL_PANE_OVERLAY_LEFT = getLocalizedColor("overlayLeft", SCROLL_PANE_OVERLAY_LEFT);
        SCROLL_PANE_OVERLAY_RIGHT = getLocalizedColor("overlayRight", SCROLL_PANE_OVERLAY_RIGHT);
        SCROLLBAR_CORNERS = getLocalizedColor("scrollbarCorners", SCROLLBAR_CORNERS);
        SCROLLBAR_TOP_LEFT = getLocalizedColor("scrollbarTopLeft", SCROLLBAR_TOP_LEFT);
        SCROLLBAR_BOTTOM_RIGHT = getLocalizedColor("scrollbarBottomRight", SCROLLBAR_BOTTOM_RIGHT);
        SCROLLBAR_FILL = getLocalizedColor("scrollbarFill", SCROLLBAR_FILL);
        SCROLLBAR_GUIDE = getLocalizedColor("scrollbarGuide", SCROLLBAR_GUIDE);
        TOOLTIP_BG_START = getLocalizedColor("tooltipBgStart", TOOLTIP_BG_START);
        TOOLTIP_BG_END = getLocalizedColor("tooltipBgEnd", TOOLTIP_BG_END);
        TOOLTIP_BORDER_START = getLocalizedColor("tooltipBorderStart", TOOLTIP_BORDER_START);
        TOOLTIP_BORDER_END = getLocalizedColor("tooltipBorderEnd", TOOLTIP_BORDER_END);
        TOOLTIP_TEXT = getLocalizedColor("tooltipText", TOOLTIP_TEXT);
        ITEM_QUANTITY_TEXT = getLocalizedColor("itemQuantityText", ITEM_QUANTITY_TEXT);
        MOD_DESCRIPTION_TEXT = getLocalizedColor("modDescriptionText", MOD_DESCRIPTION_TEXT);
    }

    /**
     * Resolve a optional localized ARGB color from lang files. Expected value format: AARRGGBB (for example FF555555).
     */
    private static int getLocalizedColor(String key, int defaultColor) {
        String fullKey = KEY_PREFIX + key;
        if (!StatCollector.canTranslate(fullKey)) {
            return defaultColor;
        }

        String raw = StatCollector.translateToLocal(fullKey);
        String hex = raw.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        } else if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        try {
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException ignored) {
            return defaultColor;
        }
    }
}
