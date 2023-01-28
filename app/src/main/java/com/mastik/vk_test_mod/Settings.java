package com.mastik.vk_test_mod;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    private boolean isSetting = false;
    private LinearLayout[] settings_layouts;
    private DisplayMetrics displayMetrics;
    private FrameLayout menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FrameLayout main_frame = findViewById(R.id.frameLayout);
        settings_layouts = new LinearLayout[1];

        menu = findViewById(R.id.menu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, MainActivity.class);
                startActivity(intent);
            }
        });

        TextView unread = findViewById(R.id.unread_count);
        unread.setText(LongPollService.unread_msg_count+"");
        unread.setVisibility(View.VISIBLE);

        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();

        Button gradient_button = findViewById(R.id.button_grad);
        LinearLayout gradient = findViewById(R.id.gradient_layout);
        settings_layouts[0] = gradient;

        gradient.setTranslationX(-displayMetrics.widthPixels);
        gradient_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("gradient_settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor prefs_edit = prefs.edit();
                for(int i = 0; i < gradient.getChildCount()-1; i++) {
                    EditText property = (EditText) ((ViewGroup)gradient.getChildAt(i)).getChildAt(1);
                    String prop_name = getResources().getResourceName(((ViewGroup)gradient.getChildAt(i)).getChildAt(1).getId()).split(":id/")[1];
                    Log.i("prop1", prop_name+"");
                    property.setText(String.valueOf(prefs.getInt(prop_name, 0)));
                    property.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void afterTextChanged(Editable editable) {
                            if(!editable.toString().equals("")) {
                                prefs_edit.putInt(prop_name, Integer.parseInt(editable.toString())).apply();
                            }else
                                prefs_edit.putInt(prop_name, Integer.parseInt(property.getHint().toString())).apply();
                        }
                    });
                }
                gradient.animate().translationX(0).setDuration(200).start();
                isSetting = true;
                menu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for(LinearLayout el : settings_layouts)
                            el.animate().translationX(-displayMetrics.widthPixels).setDuration(200).start();
                        menu.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                    }
                });
            }
        });

        //frequency.setText(readSourceData("grad_length").equals("") ? MovingRgbGradient.frequency+"" : readSourceData("grad_length"));
        //scale.setText(readSourceData("grad_scale").equals("") ? MovingRgbGradient.scale+"" : readSourceData("grad_scale"));
        //speed.setText(readSourceData("grad_speed").equals("") ? MovingRgbGradient.speed+"" : readSourceData("grad_speed"));
    }
    @Override
    public void onBackPressed(){
        if(isSetting){
            for(LinearLayout el : settings_layouts){
                el.animate().translationX(-displayMetrics.widthPixels).setDuration(200).start();
            }
            menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            isSetting = false;
        }else
            finish();
    }
}