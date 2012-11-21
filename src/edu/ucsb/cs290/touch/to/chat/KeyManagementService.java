package edu.ucsb.cs290.touch.to.chat;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import edu.ucsb.cs290.touch.to.chat.crypto.CryptoContacts;
import edu.ucsb.cs290.touch.to.chat.crypto.DatabaseHelper;
import edu.ucsb.cs290.touch.to.chat.crypto.KeyPairsProvider;
import edu.ucsb.cs290.touch.to.chat.crypto.SealablePublicKey;

public class KeyManagementService extends Service {
	private DatabaseHelper dbHelperInstance;
	private volatile KeyPairsProvider kp;
	private Timer timer;
	private static final String TAG = KeyManagementService.class
			.getSimpleName();
	private final IBinder binder = new KeyCachingBinder();

	public KeyPairsProvider getKeys() {
		return kp;
	}

	public DatabaseHelper getInstance() {
		if (dbHelperInstance == null) {
			// Use global context for the app
			dbHelperInstance = new DatabaseHelper(this);
		}
		return dbHelperInstance;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public class KeyCachingBinder extends Binder {
		public KeyManagementService getService() {
			return KeyManagementService.this;
		}
	}

	private TimerTask expireTask = new TimerTask() {
		@Override
		public void run() {
			Log.i(TAG, "Timer task doing work");
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service creating");

		timer = new Timer("KeyExpirationTimer");
		timer.schedule(expireTask, 1000L, 60 * 1000L);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Service destroying");

		timer.cancel();
		timer = null;
	}

	public int onStartCommand(Intent intent) {
		return START_STICKY;
	}

	private void foregroundService() {
		// danny shit here
		stopForeground(true);
		startForeground(SERVICE_RUNNING_ID, notification);
	}
}
