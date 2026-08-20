package com.parallax.shell;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

/** Dialog-style shell screen used when execution is blocked. */
public class ParallaxAaGaya extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.78f;
        window.setAttributes(params);
        setFinishOnTouchOutside(false);

        boolean blocked = SecurityGate.isBlocked();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(22), dp(22), dp(22), dp(22));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(26), dp(24), dp(22));
        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.rgb(18, 20, 28));
        cardBackground.setCornerRadius(dp(24));
        cardBackground.setStroke(dp(1), Color.rgb(75, 83, 110));
        card.setBackground(cardBackground);

        TextView badge = new TextView(this);
        badge.setText(blocked ? "SECURITY BLOCK" : "PARALLAX SHELL");
        badge.setTextColor(blocked ? Color.rgb(255, 112, 112) : Color.rgb(132, 214, 255));
        badge.setTextSize(12);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setColor(blocked ? Color.rgb(54, 24, 29) : Color.rgb(23, 42, 55));
        badgeBackground.setCornerRadius(dp(50));
        badge.setBackground(badgeBackground);
        card.addView(badge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Space topSpace = new Space(this);
        card.addView(topSpace, new LinearLayout.LayoutParams(1, dp(18)));

        TextView title = new TextView(this);
        title.setText(blocked ? "Parallax Shell Not Work On Rooted Device" : "Parallax Shell Ready");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Space messageSpace = new Space(this);
        card.addView(messageSpace, new LinearLayout.LayoutParams(1, dp(12)));

        TextView message = new TextView(this);
        message.setText(blocked
                ? "Root, hooking, debugging, injection or modification environment detected. Protected app execution has been stopped."
                : "Shell startup completed without a blocked environment signal.");
        message.setTextColor(Color.rgb(191, 197, 213));
        message.setTextSize(15);
        message.setLineSpacing(0, 1.16f);
        message.setGravity(Gravity.CENTER);
        card.addView(message, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Space buttonSpace = new Space(this);
        card.addView(buttonSpace, new LinearLayout.LayoutParams(1, dp(22)));

        TextView close = new TextView(this);
        close.setText(blocked ? "CLOSE APP" : "CLOSE");
        close.setTextColor(Color.WHITE);
        close.setTextSize(15);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(18), dp(13), dp(18), dp(13));
        GradientDrawable closeBackground = new GradientDrawable();
        closeBackground.setColor(blocked ? Color.rgb(181, 48, 61) : Color.rgb(45, 103, 145));
        closeBackground.setCornerRadius(dp(14));
        close.setBackground(closeBackground);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndRemoveTask();
                if (SecurityGate.isBlocked()) {
                    Process.killProcess(Process.myPid());
                }
            }
        });
        card.addView(close, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(card, cardParams);
        setContentView(root);
    }

    @Override
    public void onBackPressed() {
        if (SecurityGate.isBlocked()) {
            finishAndRemoveTask();
            Process.killProcess(Process.myPid());
            return;
        }
        super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
