package com.cnu.eslab.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cnu.eslab.suite.R;
import com.cnu.eslab.suite.TrainingSuites;
import com.cnu.eslab.suite.R.drawable;
import com.cnu.eslab.suite.R.string;
import com.cnu.eslab.suite.preferences.Preferences;
import com.cnu.eslab.suite.utility.CpuUsage;
import com.cnu.eslab.suite.utility.StandardDeviation;

public class WiFiTrainingService extends Service {

	static final int CPU_MODE = 0;
	static final int WIFI_MODE = 1;

	static int ServiceMode = CPU_MODE;

	private static final String TAG = "WiFiTrainingService";

	private static final int NOTIFYID = 20130724; // 통지의 아이디 값이다.
	private static final int NOTIFYID2 = 20130727; // 통지의 아이디 값이다.
	private static int powerMI = 0;
	private NotificationManager serviceNM = null;
	private Notification serviceNotify = null;
	private Notification powerModelNotify = null;

	private Context serviceContext = null;
	private int UpdateInterval = 100;
	private int StartInterval;
	private boolean PacketRateFixed;
	private int TemporalInterval; // 테스트를 위해서 임시로 사용한다.
	private static WiFiTrainingService single = null;

	private PowerManager mPowermanager; // Power Manager의 객체이다.
	private WakeLock mWakeLock; // Wake Lock 객체이다.
	private WifiManager.WifiLock wifiLock = null; // wifi 연결을 유지 하기 위함이다.

	private static int battLevel = 0; // percentage, or -1 for unknown
	private static int temperature = 0;
	private static int voltage = 0;
	private int mRandomValue = 0;

	private int mOldPowerCap = 0; // 이전의 battery cap의 수치 값이다.
	private int mNewPowerCap = 0; // 현재의 battery cap의 수치 값이다.

	private boolean TimeUpdate = false; // 프리퍼런스 값에 따른 서비스 업데이트를 할때
	// 중복해서 계속 실행되는것을 막아 준다.
	private CpuUsage mOCpuUsage; // CPU 이용률을 계산하기 위함이다.

	// Socket과 관련된 멤버
	private Socket socket;
	OutputStream out;

	// Packet Rate를 계산하기 위한 멤버이다.
	private long lastReceivePackets;
	private long lastTransmitPackets;
	private long totalPackets;

	/*
	 * 값을 저장한 다음 수학 연산을 하기위한 객체이다.
	 */
	private StandardDeviation mTotalCpuLoad;
	private StandardDeviation mUserCpuLoad;
	private int mDBCount = 0; // DB에 저장을 하기위한 시점이다.
	private SharedPreferences settings;

	// main activity에서 동작 시키기 위함 이다.
	public static WiFiTrainingService getInstance() {
		if (single != null)
			return single;
		return null;
	}

	@Override
	public void onCreate() {
		serviceNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		InitNotification();
		Notify();
		single = this; // 동적으로 동작하는 서비스를 Activity에서 제어하기 위해서
		// CPU Usage를 읽어들이기 위한 객체를 만든다.
		mOCpuUsage = new CpuUsage();
		// 수학계산을 위한 객체이다.
		mTotalCpuLoad = new StandardDeviation(StandardDeviation.DOUBLE);
		mUserCpuLoad = new StandardDeviation(StandardDeviation.DOUBLE);
		TemporalInterval = 100;
		initWifiSuite();
	}

	// wifi Training suite를 위한 초기화 함수이다.
	public void initWifiSuite() {
		totalPackets = 0;
		lastTransmitPackets = TrafficStats.getTotalTxPackets();
		lastReceivePackets = TrafficStats.getTotalRxPackets();
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
			// notification에 표시하는 정보들을 기록한다.

			mNewPowerCap = battLevel; // 현재 Battery cap을 읽는다.(1초에 한번씩)
			if (mOldPowerCap == mNewPowerCap) {

				String maininfo = "Start Interval: " + StartInterval;
				String extendinfo = "Update Interval: " + UpdateInterval;

				serviceNotify.setLatestEventInfo(serviceContext, maininfo,
						extendinfo, serviceNotify.contentIntent);
				// debug
				// server로 packet을 전송 한다.
				byte[] buf = new byte[1024];
				try {
					out.write(buf);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e("Traffic", "continue: Error", e);
				}

			} else if (mOldPowerCap > mNewPowerCap) {
				if (StartInterval < 500 && !(PacketRateFixed)) {
					StartInterval += UpdateInterval; // 다음 테스트를위해서 주기를 감소 시킨다.
				} else {
					// 다시 읽어와서 초기화를 한다.
					StartInterval = Integer.parseInt(settings.getString(
							Preferences.PREF_WIFI_START_INTERVAL, "100"));
				}
				mOldPowerCap = mNewPowerCap; // 값을 다시 같게 해준다.(낮아진 값으로)
			} else {
				// 이경우는 NewPowerCap이 Old보다 높은 경우 이다.
				mOldPowerCap = mNewPowerCap; // 값을 맞춰주어서 정확히 동작하도록 만들어준다.

			}

			/*
			 * //load가 너무 크게 걸리므로 이 부분을 제거 한다. // App-Widget에 대한 동작 부분을 설정한다.
			 * try { serviceNM.notify(NOTIFYID, serviceNotify); } catch
			 * (Exception e) { }
			 */

			mHandler.postDelayed(mRefresh, StartInterval * 1);
			// mHandler.postDelayed(mRefresh, 0 * 1);
		}
	};

	// Packet Rate를 계산하는 Handler이다.
	private Handler mHandler_Load = new Handler();
	private Runnable mLoadRunable = new Runnable() {
		@Override
		public void run() {
			long transmitPackets = TrafficStats.getTotalTxPackets();
			long receivePackets = TrafficStats.getTotalRxPackets();

			long lastPackets = receivePackets + transmitPackets
					- lastReceivePackets - lastTransmitPackets;
			// packet을 누적 시킨다.
			totalPackets = totalPackets + lastPackets;

			String maininfo = "WIFI SUITE";
			String extendinfo = "";

			extendinfo = "packet rate per second: " + lastPackets + "("
					+ totalPackets + ")";

			serviceNotify.setLatestEventInfo(serviceContext, maininfo,
					extendinfo, serviceNotify.contentIntent);

			try {
				serviceNM.notify(NOTIFYID2, serviceNotify);
			} catch (Exception e) {
			}

			// Packets 정보를 갱신 한다.
			lastTransmitPackets = transmitPackets;
			lastReceivePackets = receivePackets;

			// 1초 마다 packet Rate를 계산 한다.
			mHandler_Load.postDelayed(mLoadRunable, 1000);
		}
	};

	private void Enable() {
		// load settings
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			UpdateInterval = Integer.parseInt(settings.getString(
					Preferences.PREF_WIFI_INTERVAL, "10"));
			StartInterval = Integer.parseInt(settings.getString(
					Preferences.PREF_WIFI_START_INTERVAL, "100"));
			PacketRateFixed = settings.getBoolean(
					Preferences.PREF_WIFI_PACKET_RATE, false);
		} catch (Exception e) {
		}
		if (TimeUpdate == false) {
			mHandler.postDelayed(mRefresh, StartInterval * 1);
			mHandler_Load.postDelayed(mLoadRunable, 1000);
			TimeUpdate = true;
		}
		// battery 관련 리시버만 등록 시킨다.
		startBatteryMonitor();

		// Wake Lock를 얻어온다.
		mPowermanager = (PowerManager) getSystemService(Context.POWER_SERVICE); // 파워
		// 매니저를
		// 얻어온다.
		// (스크린
		// 상태를
		// 조사한다.)
		mWakeLock = mPowermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"WakeAlways"); // WakeLock을 만들어 낸다.
		mWakeLock.acquire(); // sleep mode에서도 CPU를 활성화 시키기위한 WakeLock이다.

		// wifi의 연결을 유지 시킨다.
		if (wifiLock == null) {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wifiLock = wifiManager.createWifiLock("wifilock");
			wifiLock.setReferenceCounted(true);
			wifiLock.acquire();
		}

		/*
		 * Socket을 생성 한다.스트림을 연결 시킨다.
		 */
		// debug
		try {
			socket = new Socket("168.188.127.139", 4444);
			out = socket.getOutputStream();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("Traffic", "S: Error", e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("Traffic", "S: Error", e);
		}
	}

	public void Disable() {
		if (TimeUpdate) {
			mHandler.removeCallbacks(mRefresh);
			mHandler_Load.removeCallbacks(mLoadRunable);
			TimeUpdate = false;
		}
		serviceNM.cancel(NOTIFYID);
		serviceNM.cancel(NOTIFYID2);
		stopBatteryMonitor();
		mWakeLock.release(); // WakeLock을 제거한다.

		// wifi lock을 해지 시킨다.
		if (wifiLock != null) {
			wifiLock.release();
			wifiLock = null;
		}

		byte[] buf = new byte[1024];
		buf[0] = 9;
		try {
			out.write(buf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("Traffic", "End Out: Error", e);
		}
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

	// battery가 감소 되면 이 핸들러가 실행 된다.
	private static BroadcastReceiver battReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {

			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1);

			voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0); // 베터리의
			// 전압을
			// 조사한다.
			temperature = intent.getIntExtra("temperature", -1);

			if (rawlevel >= 0 && scale > 0) {
				// debug;
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
		serviceNM.notify(NOTIFYID2, serviceNotify);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
