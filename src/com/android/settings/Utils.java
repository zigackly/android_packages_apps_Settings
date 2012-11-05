/**
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity.Header;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TabWidget;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

// Needed for execute function
import java.io.*;

// Extras
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.os.PowerManager;
import com.android.settings.R;

public class Utils {

    private static final String TAG = "Utils";

    /**
     * Set the preference's title to the matching activity's label.
     */
    public static final int UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY = 1;

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the icon that should be displayed for the preference.
     */
    private static final String META_DATA_PREFERENCE_ICON = "com.android.settings.icon";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the title that should be displayed for the preference.
     */
    private static final String META_DATA_PREFERENCE_TITLE = "com.android.settings.title";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the summary text that should be displayed for the preference.
     */
    private static final String META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary";

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int mDeviceType = -1;

    public static final String MOUNT_SYSTEM_RW = "busybox mount -o rw,remount /system";
    public static final String MOUNT_SYSTEM_RO = "busybox mount -o ro,remount /system";

    private static Context mContext;

    public static void setContext(Context mc) {
        mContext = mc;
    }

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     * @param flags 0 or one or more of
     *            {@link #UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY}
     *            .
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     */
    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    if ((flags & UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY) != 0) {
                        // Set the preference title to the activity's label
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return true;
    }

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference. The icon, title and
     * summary of the preference will also be updated with the values retrieved
     * from the activity's meta-data elements. If no meta-data elements are
     * specified then the preference title will be set to match the label of the
     * activity, an icon and summary text will not be displayed.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     *
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     *
     * @see {@link #META_DATA_PREFERENCE_ICON}
     *      {@link #META_DATA_PREFERENCE_TITLE}
     *      {@link #META_DATA_PREFERENCE_SUMMARY}
     */
    public static boolean updatePreferenceToSpecificActivityFromMetaDataOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey) {

        IconPreferenceScreen preference = (IconPreferenceScreen)parentPreferenceGroup
                .findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags
                        & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    Drawable icon = null;
                    String title = null;
                    String summary = null;

                    // Get the activity's meta-data
                    try {
                        Resources res = pm
                                .getResourcesForApplication(resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;

                        if (res != null && metaData != null) {
                            icon = res.getDrawable(metaData.getInt(META_DATA_PREFERENCE_ICON));
                            title = res.getString(metaData.getInt(META_DATA_PREFERENCE_TITLE));
                            summary = res.getString(metaData.getInt(META_DATA_PREFERENCE_SUMMARY));
                        }
                    } catch (NameNotFoundException e) {
                        // Ignore
                    } catch (NotFoundException e) {
                        // Ignore
                    }

                    // Set the preference title to the activity's label if no
                    // meta-data is found
                    if (TextUtils.isEmpty(title)) {
                        title = resolveInfo.loadLabel(pm).toString();
                    }

                    // Set icon, title and summary for the preference
                    preference.setIcon(icon);
                    preference.setTitle(title);
                    preference.setSummary(summary);

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                   return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

    public static boolean updateHeaderToSpecificActivityFromMetaDataOrRemove(Context context,
            List<Header> target, Header header) {

        Intent intent = header.intent;
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {
                    Drawable icon = null;
                    String title = null;
                    String summary = null;

                    // Get the activity's meta-data
                    try {
                        Resources res = pm.getResourcesForApplication(
                                resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;

                        if (res != null && metaData != null) {
                            icon = res.getDrawable(metaData.getInt(META_DATA_PREFERENCE_ICON));
                            title = res.getString(metaData.getInt(META_DATA_PREFERENCE_TITLE));
                            summary = res.getString(metaData.getInt(META_DATA_PREFERENCE_SUMMARY));
                        }
                    } catch (NameNotFoundException e) {
                        // Ignore
                    } catch (NotFoundException e) {
                        // Ignore
                    }

                    // Set the preference title to the activity's label if no
                    // meta-data is found
                    if (TextUtils.isEmpty(title)) {
                        title = resolveInfo.loadLabel(pm).toString();
                    }

                    // Set icon, title and summary for the preference
                    // TODO:
                    //header.icon = icon;
                    header.title = title;
                    header.summary = summary;
                    // Replace the intent with this specific activity
                    header.intent = new Intent().setClassName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name);

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        if (target.remove(header)) System.err.println("Removed " + header.id);

        return false;
    }

    /**
     * Returns true if Monkey is running.
     */
    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    /**
     * Returns the WIFI IP Addresses, if any, taking into account IPv4 and IPv6 style addresses.
     * @param context the application context
     * @return the formatted and comma-separated IP addresses, or null if none.
     */
    public static String getWifiIpAddresses(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties prop = cm.getLinkProperties(ConnectivityManager.TYPE_WIFI);
        return formatIpAddresses(prop);
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     * @param context the application context
     * @return the formatted and comma-separated IP addresses, or null if none.
     */
    public static String getDefaultIpAddresses(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) return null;
        Iterator<InetAddress> iter = prop.getAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += ", ";
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        // TODO: is there a better way to actually construct a locale that will match?
        // The main problem is, on top of Java specs, locale.toString() and
        // new Locale(locale.toString()).toString() do not return equal() strings in
        // many cases, because the constructor takes the only string as the language
        // code. So : new Locale("en", "US").toString() => "en_US"
        // And : new Locale("en_US").toString() => "en_us"
        if (null == localeStr)
            return Locale.getDefault();
        String[] brokenDownLocale = localeStr.split("_", 3);
        // split may not return a 0-length array.
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        } else if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        } else {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
        }
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        int scale = batteryChangedIntent.getIntExtra("scale", 100);
        return String.valueOf(level * 100 / scale) + "%";
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent) {
        final Intent intent = batteryChangedIntent;

        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            statusString = res.getString(R.string.battery_info_status_charging);
            if (plugType > 0) {
                statusString = statusString
                        + " "
                        + res.getString((plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                ? R.string.battery_info_status_charging_ac
                                : R.string.battery_info_status_charging_usb);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = res.getString(R.string.battery_info_status_discharging);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = res.getString(R.string.battery_info_status_not_charging);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusString = res.getString(R.string.battery_info_status_full);
        } else {
            statusString = res.getString(R.string.battery_info_status_unknown);
        }

        return statusString;
    }

    /**
     * Prepare a custom preferences layout, moving padding to {@link ListView}
     * when outside scrollbars are requested. Usually used to display
     * {@link ListView} and {@link TabWidget} with correct padding.
     */
    public static void prepareCustomPreferencesList(
            ViewGroup parent, View child, ListView list, boolean ignoreSidePadding) {
        final boolean movePadding = list.getScrollBarStyle() == View.SCROLLBARS_OUTSIDE_OVERLAY;
        if (movePadding && parent instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) child.getLayoutParams()).removeBorders = true;

            final Resources res = list.getResources();
            final int paddingSide = res.getDimensionPixelSize(
                    com.android.internal.R.dimen.preference_fragment_padding_side);
            final int paddingBottom = res.getDimensionPixelSize(
                    com.android.internal.R.dimen.preference_fragment_padding_bottom);

            final int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
            list.setPadding(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
        }
    }

    /**
     * Return string resource that best describes combination of tethering
     * options available on this device.
     */
    public static int getTetheringLabel(ConnectivityManager cm) {
        String[] usbRegexs = cm.getTetherableUsbRegexs();
        String[] wifiRegexs = cm.getTetherableWifiRegexs();
        String[] bluetoothRegexs = cm.getTetherableBluetoothRegexs();

        boolean usbAvailable = usbRegexs.length != 0;
        boolean wifiAvailable = wifiRegexs.length != 0;
        boolean bluetoothAvailable = bluetoothRegexs.length != 0;

        if (wifiAvailable && usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        } else if (wifiAvailable && usbAvailable) {
            return R.string.tether_settings_title_all;
        } else if (wifiAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        } else if (wifiAvailable) {
            return R.string.tether_settings_title_wifi;
        } else if (usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_usb_bluetooth;
        } else if (usbAvailable) {
            return R.string.tether_settings_title_usb;
        } else {
            return R.string.tether_settings_title_bluetooth;
        }
    }

    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    public static String fileReadOneLine(String fname) {
        BufferedReader br;
        String line = null;

        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "IO Exception when reading /sys/ file", e);
        }
        return line;
    }

    public static boolean fileWriteOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }

    private static int getScreenType(Context con) {
        if (mDeviceType == -1) {
            WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
            android.view.Display display = wm.getDefaultDisplay();
            int shortSize = Math.min(display.getRawHeight(), display.getRawWidth());
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT /
                DisplayMetrics.DENSITY_DEVICE;

            boolean tabletUIEnabled = (Settings.System.getInt(con.getContentResolver(),
                Settings.System.TABLET_UI_ENABLED, 1) != 0);

            if (shortSizeDp < 600 && (!tabletUIEnabled)) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                mDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720 && (!tabletUIEnabled)) {
                // 600-719dp: "phone" UI with modifications for larger screens
                mDeviceType = DEVICE_HYBRID;
            } else if (!tabletUIEnabled) {
                // 720dp but Tablet UI is specifically switched off
                mDeviceType = DEVICE_HYBRID;                
            } else {
                // Tablet UI with a single combined status & navigation bar
                mDeviceType = DEVICE_TABLET;
            }
        }
        return mDeviceType;
    }

    public static boolean isPhone(Context con) {
        return getScreenType(con) == DEVICE_PHONE;
    }

    public static boolean isHybrid(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTablet(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    // Ported with thanks from the JellyBeer ROM
    // https://github.com/beerbong/android_packages_apps_Settings/commit/4d6185f145fef5386c22c3f4d2a8f57a0c245c5a

    public static void restartUI() {
        execute(new String[] {"pkill -TERM -f com.android.systemui"}, 0);
        execute(new String[] {"pkill -TERM -f com.android.settings"}, 0);
    }

    public static boolean execute(String[] command, int wait) {
        if(wait!=0){
            try {
                Thread.sleep(wait);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Process proc;
        try {
            proc = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(proc.getOutputStream());
            for (String tmpCmd : command) {
                os.writeBytes(tmpCmd+"\n");
            }
            os.flush();
            os.close();	
            proc.waitFor();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getProperty(String prop) {
       try {
           String output = null;
           Process p = Runtime.getRuntime().exec("getprop "+prop);
           p.waitFor();
           BufferedReader input = new BufferedReader (new InputStreamReader(p.getInputStream()));
           output = input.readLine();
           return output;
       } catch (Exception e) {
           e.printStackTrace();
           return null;
       }
   }

    public static void setProperty(String property, String value) {
        setProperty(property, value, false);
    }

    public static void setProperty(String property, String value, boolean toData) {
        if (readFile("/system/build.prop").contains(property + "="))
            execute(new String[] {
                MOUNT_SYSTEM_RW,
                "cd /system",
                "busybox sed -i 's|^"+property+"=.*|"+property+"=" + value + "|' build.prop",
                "busybox chmod 644 build.prop",
                MOUNT_SYSTEM_RO
            }, 0);
        else
            execute(new String[] {
                MOUNT_SYSTEM_RW,
                "cd /system",
                "chmod 777 build.prop",
                "busybox printf \"\\n%b\" " + property + "=" + value + " >> build.prop",
                "busybox chmod 644 build.prop",
                MOUNT_SYSTEM_RO
            }, 0);

        if (toData) {
            String fileName = "/data/local.prop";
            File file = new File(fileName);
            if (!file.exists()) {
                writeFile(fileName, new String[] {property + "=" + value});
            } else if(readFile(fileName).contains(property + "=")) {
                execute(new String[]{
                    "cd /data",
                    "busybox sed -i 's|^"+property+"=.*|"+property+"=" + value + "|' local.prop",
                }, 0);
            } else {
                execute(new String[] {
                    "cd /data",
                    "busybox printf \"\\n%b\" " + property + "=" + value + " >> local.prop"
                }, 0);
            }
        }
    }

    public static void reboot() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(R.string.alert_reboot);
        alert.setMessage(mContext.getString(R.string.alert_reboot_message));
        alert.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                pm.reboot("Settings Triggered Reboot");
            }
        });
        alert.setNegativeButton(R.string.alert_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    public static void writeFile(String filename, String[] lines) {
        try {
            boolean isSystem = filename.indexOf("system/") >= 0;
            if (isSystem) {
                execute(new String[]{MOUNT_SYSTEM_RW},0);
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                for (int i=0;i<lines.length;i++) {
                    out.write((lines[i] + "\n").getBytes());
                }
            } finally {
                if (out != null) {
                    out.close();
                }
                if (isSystem) {
                    execute(new String[]{MOUNT_SYSTEM_RO},0);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String readFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
            StringBuffer sb = new StringBuffer();
            try {
                String linea = reader.readLine();
                while (linea != null) {
                    sb.append(linea + "\n");
                    linea = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return sb.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return "";
        }
    }
}
