package edu.hitsz.aircraftwar.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import edu.hitsz.aircraftwar.android.R;

public final class UiUtils {
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

    public static TextView createTitle(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(0xFFFFFFFF);
        textView.setShadowLayer(10.0f, 0.0f, 0.0f, 0xCC000000);
        return textView;
    }

    public static TextView createBody(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setLineSpacing(0.0f, 1.2f);
        textView.setTextColor(0xFFFFFFFF);
        textView.setShadowLayer(8.0f, 0.0f, 0.0f, 0xCC000000);
        return textView;
    }

    public static Button createActionButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 12);
        button.setLayoutParams(params);
        return button;
    }
}
