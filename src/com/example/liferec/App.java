package com.example.liferec;

import java.io.File;

import android.app.Application;
import android.util.Log;

public class App extends Application {

	public static final String LOGTAG = "LIFEREC";

	private String _video_file_path;

	@Override
	public void onCreate() {
		super.onCreate();

		File dir = this.getExternalFilesDir(null);
		_video_file_path = dir.getPath() + File.separator;
	}

	public String getVideoFilePath() {
		return _video_file_path;
	}
	
}
