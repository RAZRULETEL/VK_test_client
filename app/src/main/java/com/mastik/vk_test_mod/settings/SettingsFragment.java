package com.mastik.vk_test_mod.settings;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.mastik.vk_test_mod.R;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    private boolean isSetting = false;
    private LinearLayout[] settings_layouts;
    private DisplayMetrics displayMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        FrameLayout mainLayout = root.findViewById(R.id.settings);

        LinearLayout tabOpeners = root.findViewById(R.id.tabs_openers);

        settings_layouts = new LinearLayout[AppSettingsTab.values().length];

        displayMetrics = root.getContext().getResources().getDisplayMetrics();

        int i = 0;
        for(AppSettingsTab tabEnum : AppSettingsTab.values()){
            Button tabOpener = new Button(root.getContext());
            tabOpener.setText(tabEnum.stringID);
            LinearLayout tabLayout = tabEnum.getTabBuilder().getTab(root.getContext());
            tabOpener.setOnClickListener(getTabOpener(tabLayout));
            tabOpeners.addView(tabOpener);
            mainLayout.addView(tabLayout);
            tabLayout.animate().translationX(-displayMetrics.widthPixels).translationZ(1).setDuration(0).start();
            settings_layouts[i++] = tabLayout;
        }

        return root;
    }
    public boolean isSettingOpened(){
        return isSetting;
    }
    public void closeSettings(){
        isSetting = false;
        for(LinearLayout el : settings_layouts)
            el.animate().translationX(-displayMetrics.widthPixels).setDuration(200).start();
    }

    private View.OnClickListener getTabOpener(LinearLayout tabToOpen){
        return new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                tabToOpen.animate().translationX(0).setDuration(200).start();
                isSetting = true;
                System.out.println("click");
            }
        };
    }
}