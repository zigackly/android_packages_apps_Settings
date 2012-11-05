/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.CheckBoxPreference;
import android.view.View;
import android.util.Log;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class TabletUI extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String TABLET_UI_ENABLED = "tablet_ui_enabled";
    private static final String TABLET_UI_CATEGORY_MODE = "tablet_ui_mode";

    private CheckBoxPreference mTabletUIEnabled;
    private PreferenceCategory mPrefCategoryMode;

    private static final String PROPERTY = "ro.sf.lcd_density";
    private static final String TAG = "zigackly/Dpi";

    private static final String DPI_PREF = "system_dpi_window";
    private static final String CUSTOM_DPI_PREF = "custom_dpi_text";

    private ListPreference mDpiWindow;
    private EditTextPreference mCustomDpi;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        Utils.setContext(mContext);

        addPreferencesFromResource(R.xml.tablet_ui);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver cr = getActivity().getApplicationContext().getContentResolver();

        String prop = Utils.getProperty(PROPERTY);

        mTabletUIEnabled = (CheckBoxPreference) prefSet.findPreference(TABLET_UI_ENABLED);
        mTabletUIEnabled.setChecked((
            Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
            Settings.System.TABLET_UI_ENABLED, 1) == 1));

        mPrefCategoryMode = (PreferenceCategory) findPreference(TABLET_UI_CATEGORY_MODE);

        mDpiWindow = (ListPreference) prefSet.findPreference(DPI_PREF);
        mDpiWindow.setValue(prop);
        mDpiWindow.setOnPreferenceChangeListener(this);
 
        mCustomDpi = (EditTextPreference) findPreference(CUSTOM_DPI_PREF);
        mCustomDpi.setOnPreferenceClickListener(this); 
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDpiWindow) {
            String prop = Utils.getProperty(PROPERTY);
            Utils.setProperty(PROPERTY, newValue.toString());
            if (!prop.equals(newValue.toString())) {
                Utils.reboot();
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference == mCustomDpi) {
            final String prop = Utils.getProperty(PROPERTY);
            mCustomDpi.getEditText().setText(prop);
            mCustomDpi.getEditText().setSelection(prop.length());
            mCustomDpi.getDialog().findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int value = 213;
                    try {
                        value = Integer.parseInt(mCustomDpi.getEditText().getText().toString());
                    } catch (Throwable t) {}
                    if (value < 180) value = 180;
                    else if (value > 280) value = 280;
                    Utils.setProperty(PROPERTY, String.valueOf(value));
                    mCustomDpi.getDialog().dismiss();
                    if (!prop.equals(String.valueOf(value))) {
                        Utils.reboot();
                    }
                }
            });
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mTabletUIEnabled) {
            value = mTabletUIEnabled.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.TABLET_UI_ENABLED, value ? 1 : 0);
            Utils.restartUI();
            return true;
        } 
        return false;
    }
}
