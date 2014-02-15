package com.example.liferec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements Runnable {

	public static final String LOGTAG = "LIFEREC";

	
	private ServerSocket mServer;
    private Socket mSocket;
    private int port = 1337;
    volatile Thread runner = null;
    Handler mHandler = new Handler();	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button button1=(Button)findViewById(R.id.button1);
	      button1.setOnClickListener(new OnClickListener(){
	       @Override
	         public void onClick(View v) {
	             Intent intent = new Intent(MainActivity.this, VideoCapture.class);
	             startActivity(intent);
	        }
	      });

	      
	      
			if(runner == null){
	            runner = new Thread(this);
	            runner.start();
	        }
	      
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

/*		
	public void onClick(View v) {
		Intent intent = new Intent(MainActivity.this, VideoCapture.class);
		setActivity(intent);
		}
*/

	
	
	@Override
	public void run() {

		Log.v(LOGTAG, "thread start");

		
		try {
			mServer = new ServerSocket(port);
			mSocket = mServer.accept();
			BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			String message;
			final StringBuilder messageBuilder = new StringBuilder();
			while ((message = in.readLine()) != null) {
				messageBuilder.append(message);
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {

					Log.v(LOGTAG, "RCVD:" + messageBuilder.toString());
					
				}
			});
			runner.start();
			
		} catch (IOException e) {
			Log.v(LOGTAG, "FAILED to listen");

			e.printStackTrace();
		}
		
	}
	
	
}

