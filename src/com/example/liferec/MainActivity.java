package com.example.liferec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {

	public static final String LOGTAG = "LIFEREC";

    final static private String APP_KEY = "w9yk4p1soc99gdu";
    final static private String APP_SECRET = "cqdr823srmv6x5b";

	private ServerSocket mServer;
    private Socket mSocket;
    private int port = 1337;
    volatile Thread runner = null;
    Handler mHandler = new Handler();	

	private Timer timer = null;
    
    
    // You don't need to change these, leave them alone.
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static final boolean USE_OAUTH1 = false;

    DropboxAPI<AndroidAuthSession> mApi;    

    private boolean mLoggedIn;
    private Button mSubmit;
    
    private final String DROPBOX_PATH = "/mov/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(LOGTAG, "MainActivity::onCreate");

		super.onCreate(savedInstanceState);

        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

		setContentView(R.layout.activity_main);

		checkAppKeySetup();
		
		mSubmit = (Button)findViewById(R.id.auth_button);
		
		Button auth_button = (Button)findViewById(R.id.auth_button);
		auth_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        if (!mLoggedIn) {
			        // Start the remote authentication
			        if (USE_OAUTH1) {
			            mApi.getSession().startAuthentication(MainActivity.this);
			        } else {
			            mApi.getSession().startOAuth2Authentication(MainActivity.this);
			        }
		        }
			}
		});
		Button start_button = (Button)findViewById(R.id.start_button);
		start_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, VideoCapture.class);
				startActivity(intent);
			}
		});

		App app = (App)getApplication();
		if (!app.server_alive && runner == null) {
			app.server_alive = true;
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

    @Override
    protected void onResume() {
		Log.v(LOGTAG, "MainActivity::onResume");

    	super.onResume();

        AndroidAuthSession session = mApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                setLoggedIn(true);

/*                
                String path = "/sdcard/Download/Frog.jpg";
                File file = new File(path);
                UploadPicture upload = new UploadPicture(MainActivity.this, mApi, DROPBOX_PATH, file);
        	    upload.execute();
*/                

            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(LOGTAG, "Error authenticating", e);
            }
        }

        if (mLoggedIn) {
   			timer = new Timer(true);
   			timer.schedule(new TimerTask() {
   				@Override
   				public void run() {
   					 mHandler.post( new Runnable() {
   						 public void run() {
   							uploadMovies();
   						 }
   					 });
   				}
   			}, 2000);
        }
    }	

	private void uploadMovies() {
		Log.v(LOGTAG, "uploadMovies");

		App app = (App)getApplication();
		if (!app.upload_requested) {
			return;
		}
		app.upload_requested = false;

    	File dir = new File(app.getVideoFilePath());
    	final File[] files = dir.listFiles();
//    	final String[] str_items;
//    	str_items = new String[files.length];
    	Date now = new Date();
    	long e = now.getTime();
    	Log.i(LOGTAG, "Now:" + Long.toString(e));
    	for (int i = 0; i < files.length ; i++) {
    	    File file = files[i];
    	    long t = file.lastModified();
    	    if ((e-t) <= (5*60*1000)) {
    	    	// in 5min
        	    String path = app.getVideoFilePath() + file.getName();
        	    Log.i(LOGTAG, "Filename: " + path);
        	    File f = new File(path);
        	    UploadPicture upload = new UploadPicture( MainActivity.this, mApi, DROPBOX_PATH, f);
        	    upload.execute();
    	    } else {
//        	    Log.i(LOGTAG, "Filename: " + file.getName() + "," + Long.toString(t));
    	    }
      	    if ((e-t) > (60*60*1000)) {
      	    	file.delete();
      	    }
    	}
	}

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
		Log.v(LOGTAG, "Logged in");
    	mLoggedIn = loggedIn;
    	if (loggedIn) {
    		mSubmit.setText("Unlink");
    	} else {
    		mSubmit.setText("Auth");
    	}
    }    
    
	@Override
	public void run() {
		Log.v(LOGTAG, "thread start");
		
		try {
			mServer = new ServerSocket(port);
			while (acceptClient()) {
				;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				//@@
				Log.v(LOGTAG, "Thread end");
			}
		});
		
	}

	public boolean acceptClient() {
		try {
			mSocket = mServer.accept();
			BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			String message;
			while ((message = in.readLine()) != null) {
				final String s = message;

				//@@
				if (s.length() <= 4) {
					continue;
				}
				
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Log.v(LOGTAG, "RCVD:" + s);

						App app = (App)getApplication();
						app.upload_requested = true;
						
						Intent intent = new Intent(MainActivity.this, MainActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(intent);
					}
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.v(LOGTAG, "DISCONNECTED");
			}
		});

		return true;
	}

	
    private void checkAppKeySetup() {
        // Check to make sure that we have a valid app key
        if (APP_KEY.startsWith("CHANGE") ||
                APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            finish();
            return;
        }

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
            finish();
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);

		Log.v(LOGTAG, "DropBox::buildSession");
        
        return session;
    }
 
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
		Log.v(LOGTAG, "DropBox::loadAuth");

    	SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        
        if (key.equals("oauth2:")) {
    		Log.v(LOGTAG, "auth2 is valid:" + secret);
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
    		Log.v(LOGTAG, "auth1 is valid:" + secret);

        	// Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
        
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {

    	// Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
    		Log.v(LOGTAG, "DropBox::storeAuth2");

        	SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
    		Log.v(LOGTAG, "DropBox::storeAuth1");

        	SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }
	
}

