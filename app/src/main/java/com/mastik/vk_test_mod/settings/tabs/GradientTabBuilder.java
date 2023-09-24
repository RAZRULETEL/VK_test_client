package com.mastik.vk_test_mod.settings.tabs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mastik.vk_test_mod.settings.AppSettingsTab;

public class GradientTabBuilder extends AbstractTabBuilder {
    public GradientTabBuilder() {}

    @Override
    public LinearLayout getTab(Context context) {
        LinearLayout gradientTab = new LinearLayout(context);
        gradientTab.setOrientation(LinearLayout.VERTICAL);

        SharedPreferences prefs = context.getSharedPreferences(AppSettingsTab.GradientSettings.sharedPreferencesName, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEdit = prefs.edit();

        for(GradientSetting setting : GradientSetting.values()) {
            LinearLayout propertyContainer = new LinearLayout(context);
            propertyContainer.setBackgroundColor(Color.WHITE);
            propertyContainer.setOrientation(LinearLayout.HORIZONTAL);
            propertyContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView propertyName = new TextView(context);
            propertyName.setText(setting.stringID);
            propertyName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            propertyName.setTextSize(24);
            propertyName.setTextColor(Color.BLACK);
            propertyContainer.addView(propertyName);

            EditText propertyValue = new EditText(context);
            propertyValue.setText(prefs.getAll().get(setting.nameInPreferences).toString());
            propertyValue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            propertyValue.setInputType(InputType.TYPE_CLASS_NUMBER);
            propertyValue.setHint(setting.DEFAULT_VALUE+"");
            propertyContainer.addView(propertyValue);

            propertyValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        if (!editable.toString().equals("") && Integer.parseInt(editable.toString()) >= 0) {
                            prefsEdit.putInt(setting.nameInPreferences, Integer.parseInt(editable.toString())).apply();
                        } else
                            prefsEdit.putInt(setting.nameInPreferences, setting.DEFAULT_VALUE).apply();
                    } catch (NumberFormatException ignored) {

                    }
                }
            });
            gradientTab.addView(propertyContainer);
        }
        return gradientTab;
    }
}
