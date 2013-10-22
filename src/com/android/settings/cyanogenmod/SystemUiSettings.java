/*
 * Copyright (C) 2012 The CyanogenMod project
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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;  
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern; 

public class SystemUiSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener { 
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";

    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_QUICK_SETTINGS = "quick_settings_panel";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String KEY_SCREEN_ON_NOTIFICATION_LED = "screen_on_notification_led";
    private static final String KEY_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String PREF_RECENTS_CLEAR_ALL_ON_RIGHT = "recents_clear_all_on_right";   
    private static final String PREF_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_PIE_SETTINGS = "pie_settings";   

    private PreferenceScreen mPieControl;
    private ListPreference mExpandedDesktopPref; 
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mLowBatteryWarning;
    private CheckBoxPreference mScreenOnNotificationLed;
    private Preference mRamBar;
    private CheckBoxPreference mClearAll; 
    private CheckBoxPreference mUseAltResolver;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;   
    
    private boolean mIsPrimary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

	// Only show the hardware keys config on a device that does not have a navbar
        // and the navigation bar config on phones that has a navigation bar
        boolean removeKeys = false;
        boolean removeNavbar = false;

        //PreferenceCategory navbarCategory =
        //        (PreferenceCategory) findPreference(KEY_NAVIGATION_BAR_CATEGORY);

        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                removeKeys = true;
            //} else {
            //   removeNavbar = true;
            }
        } catch (RemoteException e) {
            // Do nothing
        }

        //if (removeNavbar) {
        //    prefScreen.removePreference(navbarCategory);
        //}

	//ListView Animations
        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_ANIMATION, 1);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);

        // Low battery warning
	mLowBatteryWarning = (ListPreference) findPreference(KEY_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 0);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this);

	mRamBar = findPreference(KEY_RECENTS_RAM_BAR);
        updateRamBar();

	mClearAll = (CheckBoxPreference) findPreference(PREF_RECENTS_CLEAR_ALL_ON_RIGHT);
        mClearAll.setOnPreferenceChangeListener(this);
        mClearAll.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.RECENTS_CLEAR_ALL_ON_RIGHT, 0) == 1); 

	mUseAltResolver = (CheckBoxPreference) findPreference(PREF_USE_ALT_RESOLVER);
        mUseAltResolver.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.ACTIVITY_RESOLVER_USE_ALT, 0) == 1);   

	int statusScreenOnNotificationLed = Settings.System.getInt(getContentResolver(),
                Settings.System.SCREEN_ON_NOTIFICATION_LED, 1);
        mScreenOnNotificationLed = (CheckBoxPreference) findPreference(KEY_SCREEN_ON_NOTIFICATION_LED);
        mScreenOnNotificationLed.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SCREEN_ON_NOTIFICATION_LED, 0) == 1); 

        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);
        if (mPieControl != null && removeNavbar) {
            // Remove on devices without a navbar to start with
            prefScreen.removePreference(mPieControl);
            mPieControl = null;
        }
	
        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) prefScreen.findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                        Settings.System.EXPANDED_DESKTOP_STYLE, 0);
        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        mExpandedDesktopPref.setSummary(mExpandedDesktopPref.getEntries()[expandedDesktopValue]); 

        // Hide no-op "Status bar visible" mode on devices without navbar
        //try {
        //    if (WindowManagerGlobal.getWindowManagerService().hasNavigationBar()) {
        //        mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        //        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        //        updateExpandedDesktop(expandedDesktopValue);
        //        prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
        //    } else {
		// enable "Status bar visible" mode on devices without navbar
		// even in devices with no nav bar support by default
	//	mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        //        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        //        updateExpandedDesktop(expandedDesktopValue);
        //        prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
                //mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                //mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                //prefScreen.removePreference(mExpandedDesktopPref);
        //    }
        //} catch (RemoteException e) {
        //    Log.e(TAG, "Error getting navigation bar status");
        //}

	final boolean hasSlimPieByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_slimPie);

        if (!hasSlimPieByDefault) {
            // remove SlimPie entry if not supported
            getPreferenceScreen().removePreference(findPreference(KEY_PIE_SETTINGS));
        } 	
    }

    @Override
    public void onResume() {
        super.onResume();
	updateRamBar();

        if (mPieControl != null) {
            updatePieControlDescription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
	updateRamBar();
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            int index = mExpandedDesktopPref.findIndexOfValue((String) objValue);
            if (expandedDesktopValue == 0) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.EXPANDED_DESKTOP_STYLE, expandedDesktopValue);
            mExpandedDesktopPref.setSummary(mExpandedDesktopPref.getEntries()[index]);
            return true; 
	} else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            return true;
        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true;
	} else if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) objValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY,
                    lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
	} else if (preference == mClearAll) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.RECENTS_CLEAR_ALL_ON_RIGHT,
                (Boolean) objValue ? 1 : 0);
            return true;  
	/*
        } else if (preference == mExpandedDesktopNoNavbarPref) {
            boolean value = (Boolean) objValue;
            updateExpandedDesktop(value ? 2 : 0);
            return true;
	*/
        }
        
	return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mScreenOnNotificationLed) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_ON_NOTIFICATION_LED,
                    mScreenOnNotificationLed.isChecked() ? 1 : 0);
	} else if (preference == mUseAltResolver) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACTIVITY_RESOLVER_USE_ALT,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true; 
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    } 

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }
}
