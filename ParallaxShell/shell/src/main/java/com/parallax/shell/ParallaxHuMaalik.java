package com.parallax.shell;

import android.app.Activity;
import android.os.Bundle;

/** Root-block screen substituted by the shell security gate. */
public class ParallaxHuMaalik extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParallaxHuYaarBhai.showRootDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ParallaxHuYaarBhai.showRootDialog(this);
    }

    @Override
    public void onBackPressed() {
        // The security dialog is intentionally non-cancelable.
    }
}
