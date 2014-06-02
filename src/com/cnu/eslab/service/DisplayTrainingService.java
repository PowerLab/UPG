package com.cnu.eslab.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.cnu.eslab.suite.R;
import com.cnu.eslab.suite.TrainingSuites;
import com.cnu.eslab.suite.preferences.Preferences;
import com.cnu.eslab.suite.utility.CpuUsage;
import com.cnu.eslab.suite.utility.DisplayControl;

public class DisplayTrainingService extends Service {

	static final int CPU_MODE = 0;
	static final int WIFI_MODE = 1;
	static final int DISPLAY_MODE = 2;

	static int ServiceMode = DISPLAY_MODE;

	private static final String TAG = "DisplayTrainingService";

	private static final int NOTIFYID = 20110724; // 통지의 아이디 값이다.
	private NotificationManager serviceNM = null;
	private Notification serviceNotify = null;

	private Context serviceContext = null;

	private PowerManager mPowermanager; // Power Manager의 객체이다.
	private WakeLock mWakeLock; // Wake Lock 객체이다.
	
	private static int battLevel = 0; // percentage, or -1 for unknown

	private int mOldPowerCap = 0; // 이전의 battery cap의 수치 값이다.
	private int mNewPowerCap = 0; // 현재의 battery cap의 수치 값이다.

	private boolean TimeUpdate = false; // 프리퍼런스 값에 따른 서비스 업데이트를 할때
	
	private int BrightStartLevel;
	private int BrightInterval;

	private SharedPreferences settings;

	private CpuUsage mOCpuUsage; // CPU 이용률을 계산하기 위함이다.
	
	private static DisplayTrainingService single = null;
	
	private int debugCount = 0; // 디버깅을 위해서 만든 것이다.
	
	private static TrainingSuites mActivity; //서비스 함수로 부터 받아온다.
	
	private static DisplayControl mDisplayControl;
	
	//main activity에서 동작 시키기 위함 이다.
	public static DisplayTrainingService getInstance()
	{
		if(single != null)
			return single;
		return null;
	}
	
	@Override
	public void onCreate() {
    	//CPU Usage를 읽어들이기 위한 객체를 만든다.
    	mOCpuUsage = new CpuUsage();
    	
		serviceNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		InitNotification();
		Notify();
		
		single = this;	
	}
	//main으로 부터 activity 객체를 얻온다.
	public void setActivity(TrainingSuites activity){
		mActivity = activity;
		initDisplaySuite();
	}
	//Display Training suite를 위한 초기화 함수이다.
	public void initDisplaySuite(){
		//디스플레이를 컨트롤하기위해 메인 엑티비티를 생성자로 넘겨 준다.
		mDisplayControl = new DisplayControl(mActivity);
		//일단 먼저 프리퍼런스의 시작 밝기레벨로 현재 디스플레이 화면을 변경 시켜 준다.
		mDisplayControl.setBright(BrightStartLevel);
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Disable();
	}

	public void Notify() {
		Enable();
	}

	// 실제로 Packet을 sending하는 handler이다. 
	private Handler mHandler = new Handler();
	private Runnable mRefresh = new Runnable() {
		@Override
		public void run() {
			//debug
			/*if(debugCount > 5){
				debugCount = 0;
				battLevel--;
			}
			debugCount ++;*/
			
			
			// notification에 표시하는 정보들을 기록한다.
			mNewPowerCap = battLevel; // 현재 Battery cap을 읽는다.(1초에 한번씩)
			if (mOldPowerCap == mNewPowerCap) {
				
				//cpu 이용률을 갱신 한다.
				mOCpuUsage.readStats();
				
				String maininfo = "Bright:" + BrightStartLevel+" /interval:"+BrightInterval; 
				String extendinfo = "CPU Load: " + mOCpuUsage.getTotalCPUInt(); 
				
				serviceNotify.setLatestEventInfo(serviceContext, maininfo,
						extendinfo, serviceNotify.contentIntent);
								
			} else if (mOldPowerCap > mNewPowerCap) {
				//Brightness를 조절하는 기능을 한다.
								
				BrightStartLevel -= BrightInterval;
				mDisplayControl.setBright(BrightStartLevel);	//화면 밝기를 설정 한다.
				
				//30 이하면 다시 초기화를 해준다.
				if(BrightStartLevel < 10){
					BrightStartLevel = Integer.parseInt(settings.
							getString(Preferences.PREF_DISPLAY_BRIGHTNESS_LEVEL, "255"));
					mDisplayControl.setBright(BrightStartLevel);	//화면 밝기를 설정 한다.
				}
				
				mOldPowerCap = mNewPowerCap; // 값을 다시 같게 해준다.(낮아진 값으로)
			} else {
				// 이경우는 NewPowerCap이 Old보다 높은 경우 이다.
				mOldPowerCap = mNewPowerCap; // 값을 맞춰주어서 정확히 동작하도록 만들어준다.

			}


			try {
				serviceNM.notify(NOTIFYID, serviceNotify);
			} catch (Exception e) {
			}

			mHandler.postDelayed(mRefresh, 1000);
			// mHandler.postDelayed(mRefresh, 0 * 1);
		}
	};
	
	private void Enable() {
		// load settings
		settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		//시작할 밝기 레벨을 설정 한다.
		BrightStartLevel = Integer.parseInt(settings.getString(Preferences.PREF_DISPLAY_BRIGHTNESS_LEVEL, "255"));
		//밝기 조절 인터벌을 설정 한다. 
		BrightInterval = Integer.parseInt(settings.getString(Preferences.PREF_DISPLAY_BRIGHTNESS_INTERVAL,"10"));
		
		// 먼저 현재 밝기 모드를 수동 밝기 모드로 변경 한다. 
		 Settings.System.putInt(getContentResolver(),
	    		Settings.System.SCREEN_BRIGHTNESS_MODE,
	    		Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		
		// Wake Lock를 얻어온다.
		/* 파워 매니저를 얻어온다.( 스크린 상태를 조사한다.)*/
		mPowermanager = (PowerManager) getSystemService(Context.POWER_SERVICE); 
		mWakeLock = mPowermanager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"WakeAlways"); // WakeLock을 만들어 낸다.
		mWakeLock.acquire(); // sleep mode에서도 CPU를 활성화 시키기위한 WakeLock이다.
		
		if (TimeUpdate == false) {
			mHandler.postDelayed(mRefresh, 1000);
			TimeUpdate = true;
		}
		// battery 관련 리시버만 등록 시킨다.
		startBatteryMonitor();
		
	}

	public void Disable() {
		if (TimeUpdate) {
			mHandler.removeCallbacks(mRefresh);
			TimeUpdate = false;
		}
		serviceNM.cancel(NOTIFYID);
		stopBatteryMonitor();
		mWakeLock.release(); // WakeLock을 제거한다.
				
	}

	// 베터리 브로드 케스트 리시버를 등록한다.
	private void startBatteryMonitor() {
		IntentFilter battFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(battReceiver, battFilter);
	}

	// 베터리 브로드케스트 리시버를 제거한다.
	private void stopBatteryMonitor() {
		unregisterReceiver(battReceiver);
	}

	// Battery가 감소 되면 이 핸들러가 실행 된다.
	private static BroadcastReceiver battReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {

			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1);

			if (rawlevel >= 0 && scale > 0) {
				//debug;
				battLevel = (rawlevel * 100) / scale;
			}
		}
	};

	// 통지 초기화
	private void InitNotification() {
		int thisIcon = R.drawable.serviceicon; // icon from resources
		long thisTime = System.currentTimeMillis(); // notification time

		serviceContext = this;
		CharSequence tickerText = getResources().getString(R.string.bar_title);
		CharSequence contentText = getResources().getString(
				R.string.notify_text);
		CharSequence contentTitle = getResources()
				.getString(R.string.app_title);

		serviceNotify = new Notification(thisIcon, tickerText, thisTime);
		serviceNotify.flags |= Notification.FLAG_NO_CLEAR
				| Notification.FLAG_ONGOING_EVENT;

		Intent notificationIntent = new Intent(this, TrainingSuites.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		serviceNotify.contentIntent = contentIntent;

		serviceNotify.setLatestEventInfo(this, contentTitle, contentText,
				contentIntent);

		serviceNM.notify(NOTIFYID, serviceNotify);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}