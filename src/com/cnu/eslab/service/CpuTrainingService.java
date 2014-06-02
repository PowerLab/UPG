package com.cnu.eslab.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cnu.eslab.suite.ITrainingService;
import com.cnu.eslab.suite.R;
import com.cnu.eslab.suite.TrainingSuites;
import com.cnu.eslab.suite.ITrainingService.Stub;
import com.cnu.eslab.suite.R.drawable;
import com.cnu.eslab.suite.R.string;
import com.cnu.eslab.suite.preferences.Preferences;
import com.cnu.eslab.suite.utility.CpuUsage;
import com.cnu.eslab.suite.utility.StandardDeviation;
import com.eslab.suite.providerDB.DatabaseHelper;

public class CpuTrainingService extends Service {
	
	static final int CPU_MODE = 0;
	static final int WIFI_MODE = 1;
	
	static int ServiceMode = CPU_MODE;
	
	private static final String TAG = "CpuTrainingService";
	
	
	private static final int NOTIFYID = 20120724; //통지의 아이디 값이다.
	private static final int NOTIFYID2 = 20120727; //통지의 아이디 값이다.
	private static int powerMI = 0;
	private NotificationManager serviceNM = null;
	private Notification serviceNotify = null;
	private Notification powerModelNotify = null;

	private Context serviceContext = null;
	private int UpdateInterval = 1000;	//Runable의 통지 인터벌을 설정 한다. 
	private boolean CPULoadFixed = false;
	
	private int TemporalInterval;	//테스트를 위해서 임시로 사용한다. 
	private static CpuTrainingService single = null;
	
	private PowerManager mPowermanager;	//Power Manager의 객체이다.
	private WakeLock mWakeLock;	//Wake Lock 객체이다.
	
	private static int battLevel = 0;  // percentage, or -1 for unknown
	private static int temperature = 0; 
	private static int voltage = 0;
	private int mRandomValue = 0;
	
	private int mOldPowerCap = 0;	//이전의 battery cap의 수치 값이다.
	private int mNewPowerCap = 0;	//현재의 battery cap의 수치 값이다.
	
	private boolean TimeUpdate = false;	// 프리퍼런스 값에 따른 서비스 업데이트를 할때
										// 중복해서 계속 실행되는것을 막아 준다.
	private CpuUsage mOCpuUsage;	//CPU 이용률을 계산하기 위함이다.
	SharedPreferences settings;
	/*
	 * 값을 저장한 다음 수학 연산을 하기위한 객체이다.
	 */
	private StandardDeviation mTotalCpuLoad;
	private StandardDeviation mUserCpuLoad;
	private int mDBCount = 0; //DB에 저장을 하기위한 시점이다. 
	
	//main activity에서 동작 시키기 위함 이다.
	public static CpuTrainingService getInstance()
	{
		if(single != null)
			return single;
		return null;
	}
	
	public class OSMonitorBinder extends Binder 
	{
		CpuTrainingService getService()
		{
			return CpuTrainingService.this;
		}
	}
	
	ITrainingService.Stub mBinder = new ITrainingService.Stub() {
		
		@Override
		public void setServiceMode(int mode) throws RemoteException {
			// TODO Auto-generated method stub
			ServiceMode = mode; // mode를 설정 한다. 
		}
	};
	
	@Override
    public void onCreate() {
    	serviceNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	InitNotification();
    	Notify();
    	single = this;    	//동적으로 동작하는 서비스를 Activity에서 제어하기 위해서 
    	//CPU Usage를 읽어들이기 위한 객체를 만든다.
    	mOCpuUsage = new CpuUsage();
    	//수학계산을 위한 객체이다. 
    	mTotalCpuLoad = new StandardDeviation(StandardDeviation.DOUBLE);;
    	mUserCpuLoad = new StandardDeviation(StandardDeviation.DOUBLE);;
    	TemporalInterval = 1000;
    	
    	//Intent를 얻어온다.
    	
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

    public void Notify()
    {
    	Enable();
    }
    // CPU에 부하를 걸기 위한 핸들러이다
    private Handler mHandler = new Handler();
	private Runnable mRefresh = new Runnable() 
	{
		@Override  
            public void run() {
				//notification에 표시하는 정보들을 기록한다.
				double argument_a=4321.4321, argument_b=1234.1234;
				//임의의 복잡한 연산
				for(int i=0; i<50; i++){
					argument_a = Math.pow(Math.sqrt((argument_a / argument_b)), 2);
					mRandomValue = (int)Math.random()*100 +1;
				}
				String maininfo = "Random: "+ mRandomValue+"argument: "+argument_a; //그냥 의미없는 부하 값이다. 
				String extendinfo = "Interval: "+UpdateInterval;	//현재 setting Interval을 출력한다. 
				serviceNotify.setLatestEventInfo(serviceContext, maininfo,
												 extendinfo, serviceNotify.contentIntent);
				
				//App-Widget에 대한 동작 부분을 설정한다.
				try
				{
					serviceNM.notify(NOTIFYID, serviceNotify);
				} catch(Exception e) {}

				mHandler.postDelayed(mRefresh, UpdateInterval * 1);
				//mHandler.postDelayed(mRefresh, 0 * 1);
            }
    };
    
    //CPU LOAD정보를 삽입하며 CPU 부하 핸들러의 타임 주기를 Battery Cap level의 값에 따라서 제어를 한다. 
    private Handler mHandler_Load = new Handler();
    private Runnable mLoadRunable = new Runnable() 
	{
    	@Override  
        public void run() {
    		
    		mNewPowerCap = battLevel;	//현재 Battery cap을 읽는다.(1초에 한번씩)

    		//battery 변화가 없을때는 LOAD를 추출하는 작업을 그냥 반복한다.
    		if(mOldPowerCap == mNewPowerCap){
    			//CPU 이용률을 얻어온다.
        		String maininfo = "CPU LOAD SUITE"; 
    			String extendinfo = "";
    			mOCpuUsage.readStats(); // 갱신
    			extendinfo = "LOAD: "+mOCpuUsage.getTotalCPUInt()+
    			"%("+mOCpuUsage.getUserCpuUsage()+" "+mOCpuUsage.getOtherCpuUsage()+") mode:"+ServiceMode+"";

    			serviceNotify.setLatestEventInfo(serviceContext, maininfo,
    											 extendinfo, serviceNotify.contentIntent);
    			/*
    			 * 이 부분 코드는 Option에 다른 CPU Load 증가 폭을 알기 위해서
    			 * DB에 샘플링을 하는 작업이다. 하지만 결론적으로 이것은 샘플링 해도 어렵다.
    			 * 따라서 이부분을 생략 한다.
    			 */
    			mDBCount++; //DB에 저장하는 시점을 알기 위해서 카운트를 센다.
    			if(mDBCount > 10){
    				ProcessDB();	//데이터 베이스에 저장을한 100개 단위로 평균 표준편차 MAX, MIN을 구해서 
    				mDBCount = 0;	//다시 카운팅 하기위해 0으로 초기화 한다.
    				if(UpdateInterval > 0){
    					UpdateInterval -= 100;	//다음 테스트를위해서 주기를 감소 시킨다.
    				}
    				else{
    					UpdateInterval = Integer.parseInt(settings.getString(Preferences.PREF_CPU_INTERVAL, "1000"));
    				}
    			}
    			else{
    				mTotalCpuLoad.add(mOCpuUsage.getTotalCPUInt());	//toal LOAD를 기록한다.
    				mUserCpuLoad.add(mOCpuUsage.getUserCpuUsage());	//user LOAD만 저장한다.
    			}
    		}
    		//Battery가 1%씩 줄어들 때마다 Training Suite의 Setting 값을 변경 시킨다. 
    		else if(mOldPowerCap > mNewPowerCap){
    			
    			//UpdateInterval을 감소시켜서 CPU 부하를 높인다.
    			//하지만 CPUMaxModFlag가 false여야 한다. 아니면 설정 값 상태를 지속적으로 유지 한다.
    			if(UpdateInterval > 0 && !(CPULoadFixed)){
    				UpdateInterval -= 2;	//다음 테스트를위해서 주기를 감소 시킨다.
				}
				else{
					UpdateInterval = Integer.parseInt(settings.getString(Preferences.PREF_CPU_INTERVAL, "1000"));
				}
    			mOldPowerCap = mNewPowerCap; // 값을 다시 같게 해준다.(낮아진 값으로)
    		}
    		else{
    			//이경우는 NewPowerCap이 Old보다 높은 경우 이다.
				mOldPowerCap = mNewPowerCap; //값을 맞춰주어서 정확히 동작하도록 만들어준다.
    		}
		
			try {
				serviceNM.notify(NOTIFYID2, serviceNotify);
			}catch(Exception e) {}
			
    		mHandler_Load.postDelayed(mLoadRunable,1000);
		}
	};
	private void ProcessDB(){
		//Total CPU Usage를 계산한다.
		double TotalCpuLoadAvg = mTotalCpuLoad.EvaluateAVG();
		double TotalCpuLoadSd = mTotalCpuLoad.EvaluateSD();
		double TotalCpuLoadMax = mTotalCpuLoad.EvaluateMAX();
		double TotalCpuLoadMin = mTotalCpuLoad.EvaluateMIN();
		
		//User CPU Usage를 계산한다.
		double UserCpuLoadAvg = mUserCpuLoad.EvaluateAVG();
		double UserCpuLoadSd = mUserCpuLoad.EvaluateSD();
		double UserCpuLoadMax = mUserCpuLoad.EvaluateMAX();
		double UserCpuLoadMin = mUserCpuLoad.EvaluateMIN();
		
		//db에 기록을 하는 코드 이다.
		ContentValues values = new ContentValues();
		values.put("cpu_setting", UpdateInterval);
		values.put("total_cpu_load_avg", TotalCpuLoadAvg);						
		values.put("total_cpu_load_sd",TotalCpuLoadSd);						
		values.put("total_cpu_load_max", TotalCpuLoadMax);		
		values.put("total_cpu_load_min", TotalCpuLoadMin);						
		values.put("user_cpu_load_avg", UserCpuLoadAvg);
		values.put("user_cpu_load_sd", UserCpuLoadSd);
		values.put("user_cpu_load_max", UserCpuLoadMax);
		values.put("user_cpu_load_min", UserCpuLoadMin);
		values.put("count",mDBCount);
		insertPowerModelData(values);
		
		//계산이 끝낫기 때문에 Arrary List를 초기화 해준다.
		mTotalCpuLoad.clear();
		mUserCpuLoad.clear();
	}
	//DB에 데이터를 삽입하는 함수이다.
	public boolean insertPowerModelData(ContentValues values){
		Log.d(TAG, "insertBatteryData: " + values);
		
		DatabaseHelper mOpenHelper = null;
		SQLiteDatabase db = null;
		boolean rc = true;
		
		try {
			mOpenHelper = new DatabaseHelper(this);
			db = mOpenHelper.getReadableDatabase();
			Long rowid = db.insert(DatabaseHelper.TRAINING_TABLE, "PowerModel", values);
			if (rowid < 0) {
				Log.e(TAG, "database insert failed: " + rowid);
				rc = false;
			} else {
				Log.d(TAG, "sample collected, rowid=" + rowid);
			}
		} catch (Exception e) {
			Log.e(TAG, "database exception");
			rc = false;
		}
		if (db != null) db.close();
		if (mOpenHelper != null) mOpenHelper.close();
		return rc;
	}
	
    private void Enable()
    {	
		// load settings
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			UpdateInterval = Integer.parseInt(settings.getString(Preferences.PREF_CPU_INTERVAL, "1000"));
			CPULoadFixed = settings.getBoolean(Preferences.PREF_CPU_MAX_MODE,false);
		} catch(Exception e) {}		
		if(TimeUpdate == false)
		{
			mHandler.postDelayed(mRefresh, UpdateInterval * 1);
			mHandler_Load.postDelayed(mLoadRunable, 1000);
			TimeUpdate = true;
		}
		//battery 관련 리시버만 등록 시킨다. 
		startBatteryMonitor();
		
		//Wake Lock를 얻어온다.
    	mPowermanager = (PowerManager) getSystemService(Context.POWER_SERVICE); //파워 매니저를 얻어온다. (스크린 상태를 조사한다.)
    	mWakeLock = mPowermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeAlways"); //WakeLock을 만들어 낸다.
    	mWakeLock.acquire(); // sleep mode에서도 CPU를 활성화 시키기위한 WakeLock이다.
    }
    
    public void Disable()
    {
    	if(TimeUpdate)
    	{	
    		mHandler.removeCallbacks(mRefresh);
    		mHandler_Load.removeCallbacks(mLoadRunable);
    		TimeUpdate = false;
    	}
    	serviceNM.cancel(NOTIFYID);
    	serviceNM.cancel(NOTIFYID2);
    	stopBatteryMonitor();
    	mWakeLock.release(); //WakeLock을 제거한다.
    }
    
    //베터리 브로드 케스트 리시버를 등록한다.
    private void startBatteryMonitor()
    {
    	IntentFilter battFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	registerReceiver(battReceiver, battFilter);		        		
    }
    //베터리 브로드케스트 리시버를 제거한다. 
    private void stopBatteryMonitor()
    {
    	unregisterReceiver(battReceiver);
    }
     
	private static BroadcastReceiver battReceiver = new BroadcastReceiver() 
	{
		public void onReceive(Context context, Intent intent) {
			
			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1);

			voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);	//베터리의 전압을 조사한다.
			temperature = intent.getIntExtra("temperature", -1);
			
			if (rawlevel >= 0 && scale > 0) {
				//debug;
				battLevel = (rawlevel * 100) / scale;
			}
		}
	};
     
    @Override
    public IBinder onBind(Intent intent) {
            return mBinder;
    }
	
    //통지 초기화
    private void InitNotification() 
    {
	    int thisIcon = R.drawable.serviceicon;        		// icon from resources
	    long thisTime = System.currentTimeMillis();     // notification time
	    
	    serviceContext = this; 
	    CharSequence tickerText = getResources().getString(R.string.bar_title);
	    CharSequence contentText =  getResources().getString(R.string.notify_text);
	    CharSequence contentTitle = getResources().getString(R.string.app_title);
	    
	    serviceNotify = new Notification(thisIcon, tickerText, thisTime);
	    serviceNotify.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;


    	Intent notificationIntent = new Intent(this, TrainingSuites.class);
    	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    	serviceNotify.contentIntent = contentIntent;

	    serviceNotify.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

    	serviceNM.notify(NOTIFYID, serviceNotify);
    	serviceNM.notify(NOTIFYID2, serviceNotify);
    }
}
