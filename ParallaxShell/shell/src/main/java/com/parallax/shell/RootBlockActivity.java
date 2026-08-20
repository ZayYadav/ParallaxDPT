package com.parallax.shell;

import android.app.Activity;
import android.os.Bundle;

/** Activity substituted by the shell when a rooted environment is detected. */
public class RootBlockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShellGuard.showRootDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ShellGuard.showRootDialog(this);
    }

    @Override
    public void onBackPressed() {
        // Root-block dialog is intentionally non-cancelable.
    }
}
