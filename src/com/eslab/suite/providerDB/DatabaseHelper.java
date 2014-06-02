package com.eslab.suite.providerDB;

// Copyright (c) 2009, Scott Weston <scott@weston.id.au>
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
//     * Redistributions of source code must retain the above copyright notice,
//       this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright notice,
//       this list of conditions and the following disclaimer in the documentation
//       and/or other materials provided with the distribution.
//     * Neither the name of Scott Weston nor the names of contributors may be
//       used to endorse or promote products derived from this software without
//       specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	static final String TAG = "Watts/DatabaseHelper";
	static final String DATABASE_NAME = "Training.db";
	static final int DATABASE_VERSION = 7;
	public static final String TRAINING_TABLE = "cpu";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {                                                                                                               
		Log.d(TAG, "DatabaseHelper onCreate called");
		db.execSQL("CREATE TABLE " + TRAINING_TABLE + " ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "cpu_setting INTEGER,"
				+ "total_cpu_load_avg INTEGER,"
				+ "total_cpu_load_sd INTEGER,"
				+ "total_cpu_load_max INTEGER,"
				+ "total_cpu_load_min INTEGER,"
				+ "user_cpu_load_avg INTEGER,"
				+ "user_cpu_load_sd INTEGER,"
				+ "user_cpu_load_max INTEGER,"
				+ "user_cpu_load_min INTEGER,"
				+ "count INTEGER"
				+ ");");
	}
	@Override
	public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "database upgrade requested.  ignored.");
		db.execSQL("DROP TABLE IF EXISTS " + TRAINING_TABLE);
		onCreate(db);
	}
}
