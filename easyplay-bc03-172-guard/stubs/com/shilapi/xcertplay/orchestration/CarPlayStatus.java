package com.shilapi.xcertplay.orchestration;
/** Compile-only. Existing APK owns these singleton types and fields. */
public abstract class CarPlayStatus {
    public static final class RunningWireless extends CarPlayStatus {
        public static final RunningWireless INSTANCE = new RunningWireless();
    }
    public static final class WirelessActive extends CarPlayStatus {
        public static final WirelessActive INSTANCE = new WirelessActive();
    }
}
