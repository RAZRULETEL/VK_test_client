package com.mastik.vk_test_mod.settings.tabs;

import com.mastik.vk_test_mod.MovingRgbGradient;
import com.mastik.vk_test_mod.R;

public enum GradientSetting {
    Frequency("length", R.string.frequency, MovingRgbGradient.DEFAULT_FREQUENCY), Scale("scale", R.string.scale, MovingRgbGradient.DEFAULT_SCALE), Speed("speed", R.string.speed, MovingRgbGradient.DEFAULT_SPEED), Darkening("nightness", R.string.darkening, MovingRgbGradient.DEFAULT_DARKENING);
    public final String nameInPreferences;
    public final int stringID, DEFAULT_VALUE;
    GradientSetting(String preferenceName, int nameInUI, int defaultValue) {
        this.nameInPreferences = preferenceName;
        this.stringID = nameInUI;
        this.DEFAULT_VALUE = defaultValue;
    }
}
