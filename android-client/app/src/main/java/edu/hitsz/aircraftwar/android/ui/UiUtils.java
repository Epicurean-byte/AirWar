package edu.hitsz.aircraftwar.android.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import edu.hitsz.aircraftwar.android.R;

public final class UiUtils {
    private static final int COLOR_TEXT_PRIMARY = 0xFFF4F4E8;
    private static final int COLOR_TEXT_MUTED = 0xFFB8C39B;
    private static final int COLOR_GOLD = 0xFFD9B85F;
    private static final int COLOR_PANEL_TOP = 0xD61A2415;
    private static final int COLOR_PANEL_BOTTOM = 0xC6161E12;
    private static final int COLOR_PANEL_BORDER = 0xFF8D7747;
    private static final int COLOR_BUTTON_TOP = 0xFF566C2C;
    private static final int COLOR_BUTTON_BOTTOM = 0xFF314018;
    private static final int COLOR_BUTTON_SECONDARY_TOP = 0xFF1D2A20;
    private static final int COLOR_BUTTON_SECONDARY_BOTTOM = 0xFF131A14;
    private static final int COLOR_TAB_ACTIVE_TOP = 0xFF806229;
    private static final int COLOR_TAB_ACTIVE_BOTTOM = 0xFF5B451B;
    private static final int COLOR_TAB_IDLE_TOP = 0xAA192113;
    private static final int COLOR_TAB_IDLE_BOTTOM = 0xAA0F140D;
    private static final int COLOR_POSITIVE = 0xFF78C95F;
    private static final int COLOR_NEGATIVE = 0xFFE46A4F;
    private static final int COLOR_ACCENT = 0xFF77D9D5;

    private UiUtils() {
    }

    public static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    public static LinearLayout createScreenColumn(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.setBackgroundResource(R.drawable.pc_bg_easy);
        layout.setClipToPadding(false);
        int horizontalPadding = dp(context, 24);
        int verticalPadding = dp(context, 24);
        layout.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        ViewCompat.setOnApplyWindowInsetsListener(layout, (view, insets) -> {
            Insets systemBars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    horizontalPadding + systemBars.left,
                    verticalPadding + systemBars.top,
                    horizontalPadding + systemBars.right,
                    verticalPadding + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(layout);
        return layout;
    }

    public static HorizontalScrollView createHorizontalScroll(Context context) {
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return scrollView;
    }

    public static LinearLayout createPanel(Context context) {
        return createPanel(context, 18);
    }

    public static LinearLayout createPanel(Context context, int paddingDp) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(buildPanelDrawable(COLOR_PANEL_TOP, COLOR_PANEL_BOTTOM, COLOR_PANEL_BORDER, dp(context, 18)));
        int padding = dp(context, paddingDp);
        panel.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 14);
        panel.setLayoutParams(params);
        return panel;
    }

    public static LinearLayout createRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    public static TextView createTitle(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        textView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        textView.setGravity(Gravity.CENTER);
        textView.setLetterSpacing(0.05f);
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setShadowLayer(10.0f, 0.0f, 0.0f, 0xCC000000);
        return textView;
    }

    public static TextView createBody(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setLineSpacing(0.0f, 1.2f);
        textView.setTextColor(COLOR_TEXT_PRIMARY);
        textView.setShadowLayer(8.0f, 0.0f, 0.0f, 0xCC000000);
        return textView;
    }

    public static TextView createSectionTitle(Context context, String text) {
        TextView textView = createBody(context, text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textView.setTextColor(COLOR_GOLD);
        textView.setLetterSpacing(0.04f);
        return textView;
    }

    public static TextView createCaption(Context context, String text) {
        TextView textView = createBody(context, text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        textView.setTextColor(COLOR_TEXT_MUTED);
        return textView;
    }

    public static TextView createChip(Context context, String text, int backgroundColor) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setTextColor(COLOR_TEXT_PRIMARY);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        chip.setBackground(buildPanelDrawable(backgroundColor, darken(backgroundColor, 0.22f), 0x55FFFFFF, dp(context, 999)));
        return chip;
    }

    public static EditText createTextInput(Context context, String hint) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setTextColor(COLOR_TEXT_PRIMARY);
        editText.setHintTextColor(COLOR_TEXT_MUTED);
        editText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        editText.setBackground(buildPanelDrawable(0xCC182113, 0xCC12170E, 0xFF5E7040, dp(context, 16)));
        editText.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 12);
        editText.setLayoutParams(params);
        return editText;
    }

    public static Button createActionButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(COLOR_TEXT_PRIMARY);
        button.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setAllCaps(false);
        button.setBackground(buildPanelDrawable(COLOR_BUTTON_TOP, COLOR_BUTTON_BOTTOM, 0xFFC7B06A, dp(context, 16)));
        button.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 12);
        button.setLayoutParams(params);
        return button;
    }

    public static Button createSecondaryButton(Context context, String text) {
        Button button = createActionButton(context, text);
        button.setBackground(buildPanelDrawable(COLOR_BUTTON_SECONDARY_TOP, COLOR_BUTTON_SECONDARY_BOTTOM, COLOR_PANEL_BORDER, dp(context, 16)));
        return button;
    }

    public static Button createSmallButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(COLOR_TEXT_PRIMARY);
        button.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        button.setBackground(buildPanelDrawable(COLOR_BUTTON_TOP, COLOR_BUTTON_BOTTOM, 0xFFC7B06A, dp(context, 12)));
        return button;
    }

    public static Button createTabButton(Context context, String text, boolean selected) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(COLOR_TEXT_PRIMARY);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        button.setAllCaps(false);
        button.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        params.rightMargin = dp(context, 8);
        button.setLayoutParams(params);
        applyTabStyle(button, selected);
        return button;
    }

    public static void applyTabStyle(Button button, boolean selected) {
        Context context = button.getContext();
        button.setBackground(buildPanelDrawable(
                selected ? COLOR_TAB_ACTIVE_TOP : COLOR_TAB_IDLE_TOP,
                selected ? COLOR_TAB_ACTIVE_BOTTOM : COLOR_TAB_IDLE_BOTTOM,
                selected ? COLOR_GOLD : 0xFF55623B,
                dp(context, 14)
        ));
        button.setAlpha(selected ? 1.0f : 0.85f);
    }

    public static void setTopMargin(View view, Context context, int marginDp) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        ViewGroup.MarginLayoutParams params;
        if (layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
            params = marginLayoutParams;
        } else {
            params = new ViewGroup.MarginLayoutParams(
                    layoutParams == null ? ViewGroup.LayoutParams.MATCH_PARENT : layoutParams.width,
                    layoutParams == null ? ViewGroup.LayoutParams.WRAP_CONTENT : layoutParams.height
            );
        }
        params.topMargin = dp(context, marginDp);
        view.setLayoutParams(params);
    }

    public static int colorPositive() {
        return COLOR_POSITIVE;
    }

    public static int colorNegative() {
        return COLOR_NEGATIVE;
    }

    public static int colorAccent() {
        return COLOR_ACCENT;
    }

    public static int colorGold() {
        return COLOR_GOLD;
    }

    public static int colorTextPrimary() {
        return COLOR_TEXT_PRIMARY;
    }

    public static int colorTextMuted() {
        return COLOR_TEXT_MUTED;
    }

    private static GradientDrawable buildPanelDrawable(int topColor, int bottomColor, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(2, strokeColor);
        return drawable;
    }

    private static int darken(int color, float factor) {
        int alpha = Color.alpha(color);
        int red = Math.max(0, (int) (Color.red(color) * (1.0f - factor)));
        int green = Math.max(0, (int) (Color.green(color) * (1.0f - factor)));
        int blue = Math.max(0, (int) (Color.blue(color) * (1.0f - factor)));
        return Color.argb(alpha, red, green, blue);
    }
}
