package com.example.liferec;

/*
 * this file is based on: 
 * https://github.com/vanevery/Custom-Video-Capture-with-Preview
 */

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class VideoCapture extends Activity implements SurfaceHolder.Callback {

	public static final String LOGTAG = "LIFEREC";

	private MediaRecorder recorder = null;
	private SurfaceHolder holder;
	private CamcorderProfile camcorderProfile;
	private Camera camera;	
	
	boolean recording = false;
	boolean usecamera = true;
	boolean previewRunning = false;

	private Timer timer = null;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);

		setContentView(R.layout.capture);

		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//*
		cameraView.setClickable(true);
		cameraView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(LOGTAG, "clicked");
				finish();
			}
		});
//*/
	}

	private String getVideoFilename(String extension) {
		App app = (App)getApplication();
		String path = app.getVideoFilePath() + "video_";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		path += sdf.format(new Date());
		path += ".";
		path += extension;
		return path;
	}

	private void prepareRecorder() {
		if (recorder == null) {
			recorder = new MediaRecorder();
		} else {
			recorder.reset();
		}
		recorder.setPreviewDisplay(holder.getSurface());

		if (usecamera) {
			camera.unlock();
			recorder.setCamera(camera);
		}
		
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		recorder.setProfile(camcorderProfile);
	}

	public void startRecording() {
		if (recording) {
			return;
		}

		prepareRecorder();
		
		if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
    		recorder.setOutputFile(getVideoFilename("3gp"));
		} else {
    		recorder.setOutputFile(getVideoFilename("mp4"));
		}

		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}

		recording = true;
		recorder.start();
		Log.v(LOGTAG, "Recording Started");

		if (timer == null) {
			handler = new Handler();
			timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					 handler.post( new Runnable() {					
						 public void run() {
							Log.v(LOGTAG, "timer");
							if (recording) {
								stopRecording();
								startRecording();
							}
						 }
					 });
				}
			}, 60000, 60000);
		}
	}

	public void stopRecording() {
		if (!recording) {
			return;
		}

		recorder.stop();

		if (usecamera) {
			try {
				camera.reconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		recording = false;
		Log.v(LOGTAG, "Recording Stopped");
	}

	public void toggleRecording() {
		if (recording) {
			stopRecording();
		} else {
			startRecording();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceCreated");
		
		if (usecamera) {
			camera = Camera.open();
			
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			} catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	
		}		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(LOGTAG, "surfaceChanged");

		if (!recording && usecamera) {
			if (previewRunning){
				camera.stopPreview();
			}

			try {
				Camera.Parameters p = camera.getParameters();

				 p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
			     p.setPreviewFrameRate(camcorderProfile.videoFrameRate);
				
				camera.setParameters(p);
				
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			} catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	

			startRecording();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceDestroyed");

		// delete timer
		if (timer != null) {
			timer.cancel();
			timer = null;
		}

		// delete MediaRecorder
		if (recording) {
			recorder.reset();
			recording = false;
		}
		recorder.release();

		if (usecamera) {
			previewRunning = false;
			camera.release();
		}
		finish();
	}
}
