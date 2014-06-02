package com.cnu.eslab.suite;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.cnu.eslab.service.CpuTrainingService;
import com.cnu.eslab.service.DisplayTrainingService;
import com.cnu.eslab.service.WiFiTrainingService;
import com.cnu.eslab.suite.preferences.Preferences;
import com.cnu.eslab.suite.utility.DisplayControl;

public class TrainingSuites extends Activity {
    /** Called when the activity is first created. */
	Button mCPUTrainingStartBtn;
	Button mCPUTrainingEndBtn;
	
	Button mWifiTrainingStartBtn;
	Button mWifiTrainingStopBtn;
	EditText mSettingEdtTxt;
	Context mContext= null;
	
	Button mLEDTrainingStartBtn;
	Button mLEDTrainingStopBtn;
	Button mLEDTrainingInitBtn;
	
	Intent serviceIntent;
	Intent serviceWifiIntent;
	Intent ServiceLEDIntent;
	ITrainingService counterService;
	TrainingConnection conn;
	  
	boolean State_CPUTrainingService = false;
	boolean State_WiFiTrainingService = false;
	boolean State_DisplayTrainingService = false;
	
	public DisplayControl displayControl;
	private static TrainingSuites single = null;
	int bright=0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mContext = this;
        mCPUTrainingStartBtn = (Button)findViewById(R.id.service_start_btn);
        mCPUTrainingStartBtn.setOnClickListener(pOnClickListener);
        mCPUTrainingEndBtn = (Button)findViewById(R.id.service_Stop_btn);
        mCPUTrainingEndBtn.setOnClickListener(pOnClickListener);
        
        mWifiTrainingStartBtn = (Button)findViewById(R.id.wifi_training_start_btn);	
        mWifiTrainingStartBtn.setOnClickListener(pOnClickListener);
        mWifiTrainingStartBtn = (Button)findViewById(R.id.wifi_training_stop_btn);	
        mWifiTrainingStartBtn.setOnClickListener(pOnClickListener);
        
        mLEDTrainingStartBtn = (Button)findViewById(R.id.led_service_startbtn);
        mLEDTrainingStartBtn.setOnClickListener(pOnClickListener);
        mLEDTrainingStopBtn = (Button)findViewById(R.id.led_service_stoptbtn);
        mLEDTrainingStopBtn.setOnClickListener(pOnClickListener);
    
        mLEDTrainingInitBtn = (Button)findViewById(R.id.led_service_initbtn);
        mLEDTrainingInitBtn.setOnClickListener(pOnClickListener);
        
        //미리 bindService object를 생성해 놓는다.
        serviceIntent = new Intent(this, CpuTrainingService.class);
        serviceWifiIntent = new Intent(this, WiFiTrainingService.class);
        ServiceLEDIntent = new Intent(this, DisplayTrainingService.class);
                
        conn = new TrainingConnection();
        displayControl = new DisplayControl(this);
        single = this;
    }
    
    private class TrainingConnection implements ServiceConnection {

    	public void onServiceConnected(ComponentName className, 
                                       IBinder boundService) {
          counterService = ITrainingService.Stub.asInterface((IBinder)boundService);
        }
    	
        //bind service가 연결 해지 됬을 때 실행 된다.
        public void onServiceDisconnected(ComponentName className) {
          counterService = null;
        }
      } 
    
    public void setBright(int value){
    	//밝기 값에 value 값을 적용한다. ( value : 0~ 255 값 )
    	Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        //변경된 밝기 값을 적용한다. ( temp 값은 밝기 값 )
        Window w = getWindow();
    	WindowManager.LayoutParams lp=w.getAttributes();
    	float brightness = lp.screenBrightness;
    	lp.screenBrightness = value / (float)255; 
     	w.setAttributes(lp);
     	
    }
   
   OnClickListener pOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()){
			case R.id.service_start_btn:
				startService(serviceIntent);
				State_CPUTrainingService = true;
				break;
			case R.id.service_Stop_btn:
				stopService(serviceIntent);
				State_CPUTrainingService = false;
				break;
			case R.id.wifi_training_start_btn:
				startService(serviceWifiIntent);
				State_WiFiTrainingService = true;
				break;
			case R.id.wifi_training_stop_btn:
				stopService(serviceWifiIntent);
				State_WiFiTrainingService = false;
				break;				
			case R.id.led_service_startbtn:
				startService(ServiceLEDIntent);
				State_DisplayTrainingService = true;
				break;
			case R.id.led_service_initbtn:
				DisplayTrainingService.getInstance().setActivity(single);
				break;
			case R.id.led_service_stoptbtn:
				stopService(ServiceLEDIntent);
				State_DisplayTrainingService = false;
				break;
			}
		}
    };
			
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
    	//restorePrefs();
	   	super.onResume();
	   	bindService(serviceIntent, conn, 0);
	}
        
    @Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unbindService(conn);
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	restorePrefs();
    }
    
	public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_text));
     	optionMenu.add(0, 2, 0, getResources().getString(R.string.db_text));
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        super.onOptionsItemSelected(item);
        switch(item.getItemId())
        {
        case 1:
       		Intent launchPreferencesIntent = new Intent().setClass(this, Preferences.class);
       		startActivityForResult(launchPreferencesIntent, 0);
        	break;
        case 2:
       		Intent launchDBIntent = new Intent().setClass(this, SaveInformationActivity.class);
       		startActivityForResult(launchDBIntent, 0);
        	break;
        }
        
        return true;
    }
    
	private void restorePrefs()
    {
		//각각의 서비스가 동작 중일 떄에만 설정값을 갱신 시켜 준다.
		if(State_CPUTrainingService)
        	CpuTrainingService.getInstance().Notify();
		if(State_WiFiTrainingService)
        	WiFiTrainingService.getInstance().Notify();
		if(State_DisplayTrainingService)
        	DisplayTrainingService.getInstance().Notify();
		
    }
	
}