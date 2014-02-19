package com.example.liferec;

import java.io.File;
import java.util.List;

import android.app.Application;
import android.util.Log;

public class App extends Application {

	public static final String LOGTAG = "LIFEREC";

	private String _video_file_path;

	boolean server_alive;
	boolean upload_requested = false;

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
