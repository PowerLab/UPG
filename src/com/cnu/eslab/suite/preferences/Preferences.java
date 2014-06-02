/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.cnu.eslab.suite.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.cnu.eslab.suite.R;

public class Preferences extends PreferenceActivity {
	
	public static final String PREF_CPU_INTERVAL = "cpu_intervanl_time_preference";
	public static final String PREF_CPU_MAX_MODE = "cpu_max_mode";
	
	public static final String PREF_WIFI_INTERVAL = "wifi_intervanl_time_preference";
	public static final String PREF_WIFI_PACKET_RATE = "wifi_packet_mode_fixed";
	public static final String PREF_WIFI_START_INTERVAL = "wifi_start_time_preference";
	
	public static final String PREF_DISPLAY_BRIGHTNESS_LEVEL = "display_starting_brightness";
	public static final String PREF_DISPLAY_BRIGHTNESS_INTERVAL = "display_brightness_Interval";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
       
    }

}