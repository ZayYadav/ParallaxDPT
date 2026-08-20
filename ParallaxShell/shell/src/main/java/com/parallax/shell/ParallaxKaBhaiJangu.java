package com.parallax.shell;

import android.app.Activity;
import android.os.Bundle;

/** Developer-options / unauthorized-device block screen. */
public class ParallaxKaBhaiJangu extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParallaxBhaiya.showDeveloperDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ParallaxBhaiya.showDeveloperDialog(this);
    }

    @Override
    public void onBackPressed() {
        // The authorization dialog is intentionally non-cancelable.
    }
}
