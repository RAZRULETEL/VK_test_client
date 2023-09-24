package com.mastik.vk_test_mod.settings;

import com.mastik.vk_test_mod.R;
import com.mastik.vk_test_mod.settings.tabs.AbstractTabBuilder;
import com.mastik.vk_test_mod.settings.tabs.GradientTabBuilder;

public enum AppSettingsTab {
    GradientSettings("gradient_settings", R.string.gradient, new GradientTabBuilder());
    public final String sharedPreferencesName;
    public final int stringID;
    private final AbstractTabBuilder tabBuilder;
     AppSettingsTab(String sharedPreferencesName, int stringID, AbstractTabBuilder tabBuilder){
         this.sharedPreferencesName = sharedPreferencesName;
         this.stringID = stringID;
         this.tabBuilder = tabBuilder;
     }

     public AbstractTabBuilder getTabBuilder() { return tabBuilder; }
}
