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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class TabletUI extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TABLET_UI_ENABLED = "tablet_ui_enabled";
    private static final String TABLET_UI_CATEGORY_MODE = "tablet_ui_mode";

    private CheckBoxPreference mTabletUIEnabled;
    private PreferenceCategory mPrefCategoryMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tablet_ui);

        PreferenceScreen prefSet = getPreferenceScreen();

        mTabletUIEnabled = (CheckBoxPreference) prefSet.findPreference(TABLET_UI_ENABLED);
        mTabletUIEnabled.setChecked((
            Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
            Settings.System.TABLET_UI_ENABLED, 1) == 1));

        mPrefCategoryMode = (PreferenceCategory) findPreference(TABLET_UI_CATEGORY_MODE);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
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
