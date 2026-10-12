package com.shilapi.xcertplay.legacy;

/** Added patch entry point, called only after a worker has saved the selected phone. */
public class LegacyActivity extends android.app.Activity {
    public void patchUseRememberedPhone(String address, String name) { throw new AssertionError(); }
}
