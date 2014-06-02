package com.cnu.eslab.suite;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.eslab.suite.providerDB.DatabaseHelper;

public class SaveInformationActivity extends Activity {

	final int _ID = 0;
	final int CPUSETTING = 1;
	final int TOTALCPULOADAVG = 2;
	final int TOTALCPULOADSD = 3;
	final int TOTALCPULOADMAX = 4;
	final int TOTALCPULOADMIN = 5;
	final int USERCPULOADAVG = 6;
	final int USERCPULOADSD = 7;
	final int USERCPULOADMAX = 8;
	final int USERCPULOADMIN = 9;
	final int COUNT = 10;
	
	private ExpandableListView mMyList;	
	private SQLiteDatabase mDb;	// SQLite DB에 접근하기위한 변수
	private Cursor mCursor;
	private DatabaseHelper mSaveDatabases;
	// 커스터 마이징 한 어뎁터이다.
	MyExpandableListAdapter mAdapter;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loadsave);

		mMyList = (ExpandableListView)findViewById(R.id.gamesavelistview);
		mSaveDatabases = new DatabaseHelper(this);
		mDb = mSaveDatabases.getWritableDatabase();

		mCursor = mDb.rawQuery("select _id, cpu_setting, total_cpu_load_avg, total_cpu_load_sd," +
				"total_cpu_load_max, total_cpu_load_min, user_cpu_load_avg, user_cpu_load_sd," +
				"user_cpu_load_max, user_cpu_load_min, count from cpu", null);
		
		mAdapter = new MyExpandableListAdapter(this, mCursor, 
				android.R.layout.simple_expandable_list_item_2, 
				R.layout.chunk_list_item);
		
		mMyList.setAdapter(mAdapter);
	}
	OnClickListener pOnClickListener = new OnClickListener() {

		public void onClick(View v) {
		}
	};

    public class MyExpandableListAdapter extends ResourceCursorTreeAdapter {

    	// 생성자로 기본적인 자료를 넘겨 주어야 한다. 
		public MyExpandableListAdapter(Context context, Cursor cursor,
				int groupLayout, int childLayout) {
			super(context, cursor, groupLayout, childLayout);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			// TODO Auto-generated method stub
			TextView mInfoView = (TextView)view.findViewById(R.id.chunk_detail_info);
/*			
			mInfoView.setPadding(3, 3, 3, 3); 
			mInfoView.setTextSize(8);*/
			mInfoView.setText("TOTAL LOAD : "+cursor.getString(TOTALCPULOADAVG)+"%\t\t\tSD: "+cursor.getString(TOTALCPULOADSD)+"%\n");
			mInfoView.append("MAX: "+cursor.getString(TOTALCPULOADMAX)+"%\t\t\tMIN: "+cursor.getString(TOTALCPULOADMIN)+"%\n");
			mInfoView.append("USER LOAD : "+cursor.getString(USERCPULOADAVG)+"%\t\t\tSD: "+cursor.getString(USERCPULOADSD)+"%\n");
			mInfoView.append("MAX: "+cursor.getString(USERCPULOADMAX)+"\t\t\tMIN: "+cursor.getString(USERCPULOADMIN)+"\n");
			mInfoView.append("COUNT : "+cursor.getString(COUNT)+"sec\n");
		}
/*		
		@Override
		public View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			// TODO Auto-generated method stub
			return super.newChildView(context, cursor, isLastChild, parent);
		}*/

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			// TODO Auto-generated method stub	
			TwoLineListItem mTwoLineList = (TwoLineListItem)view;
			TextView mInfoView = mTwoLineList.getText1();
			TextView mInfoView2 = mTwoLineList.getText2();
			
			mInfoView.setTextSize(15);
			mInfoView.setTextColor(Color.GREEN);
			mInfoView2.setTextSize(15);
			mInfoView2.setTextColor(Color.BLUE);
			mInfoView.setText(cursor.getString(_ID)+" Training Interval: "+cursor.getString(CPUSETTING)+"msec");
			mInfoView2.setText("");
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// TODO Auto-generated method stub
			Cursor childCurosr = mDb.rawQuery("select _id, cpu_setting, total_cpu_load_avg, total_cpu_load_sd," +
				"total_cpu_load_max, total_cpu_load_min, user_cpu_load_avg, user_cpu_load_sd," +
				"user_cpu_load_max, user_cpu_load_min, count from cpu where _id = "+groupCursor.getString(_ID), null);
			return childCurosr;
		}
    }
/*
	//커서 어뎁터의 커스터 마이징 원하는 값을 출력 해주기 위해서 한다.
	private final class ContactListItemAdapter extends ResourceCursorAdapter {
		public ContactListItemAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final ContactListItemCache cache = (ContactListItemCache) view.getTag();
			//캐쉬에서 꺼내는 작업
			TextView bitemId = cache.itemId;
			TextView bitemPower = cache.itemPower;
			TextView bitemCpuLoadAvg = cache.itemCpuLoadAvg;
			TextView bitemCpuFreqAvg = cache.itemCpuFreqAvg;
			TextView bitemLedTimeAvg = cache.itemLedTimeAvg;
			TextView bitemLedBrightAvg = cache.itemLedBrightAvg;
			TextView bitemCount = cache.itemCount;
			TextView bitemRange = cache.itemRange;
			TextView bitemVoltageAvg = cache.itemVoltageAvg;
			
			bitemId.setText(cursor.getString(_ID));
			bitemPower.setText(cursor.getString(POWER));
			bitemCpuLoadAvg.setText(cursor.getString(CPULOADAVG));
			bitemCpuFreqAvg.setText(cursor.getString(CPUFREQAVG));
			bitemLedTimeAvg.setText(cursor.getString(LEDTIMEAVG));
			bitemLedBrightAvg.setText(cursor.getString(LEDBRIGHTAVG));
			bitemCount.setText(cursor.getString(COUNT));
			bitemRange.setText(cursor.getString(RANGE));
			bitemVoltageAvg.setText(cursor.getString(VOLTAGE));
		}
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = super.newView(context, cursor, parent);
			ContactListItemCache cache = new ContactListItemCache();
			cache.itemId = (TextView) view.findViewById(R.id.chunk_id);
			cache.itemPower = (TextView) view.findViewById(R.id.chunk_power);
			cache.itemCpuLoadAvg = (TextView) view.findViewById(R.id.chunk_cpuloadavg);
			cache.itemCpuFreqAvg = (TextView) view.findViewById(R.id.chunk_cpufreqavg);
			cache.itemLedTimeAvg = (TextView) view.findViewById(R.id.chunk_ledtimeavg);
			cache.itemLedBrightAvg = (TextView) view.findViewById(R.id.chunk_ledbrightavg);
			cache.itemCount = (TextView) view.findViewById(R.id.chunk_count);
			cache.itemRange = (TextView) view.findViewById(R.id.chunk_range);
			cache.itemVoltageAvg = (TextView) view.findViewById(R.id.chunk_voltage);
			
			view.setTag(cache);
			return view;
		}
	}
	*/
    
	final static class ContactListItemCache {
		public TextView itemId;
		public TextView itemPower;
		public TextView itemCpuLoadAvg;
		public TextView itemCpuFreqAvg;
		public TextView itemLedTimeAvg;
		public TextView itemLedBrightAvg;
		public TextView itemCount;
		public TextView itemRange;
		public TextView itemVoltageAvg;
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//DB를 쓰고난 다음에 닫아 주어야 한다.
		mDb.close();
		mCursor.close();
	}
	
}
