package com.cnu.eslab.suite.utility;

import com.cnu.eslab.suite.TrainingSuites;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

public class DisplayControl {
	private TrainingSuites activity;
	public DisplayControl(TrainingSuites arActivity){
		activity = arActivity;
		
	}
	
    public void setBright(int value) { 
    	
    	//밝기 값에 value 값을 적용한다. ( value : 0~ 255 값 )
    	Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        //변경된 밝기 값을 적용한다. ( temp 값은 밝기 값 )
        Window w = activity.getWindow();
    	WindowManager.LayoutParams lp=w.getAttributes();
    	float brightness = lp.screenBrightness;
    	lp.screenBrightness = value / (float)255; 
     	w.setAttributes(lp);
    }
	
}
