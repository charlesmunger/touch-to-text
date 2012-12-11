package edu.ucsb.cs290.touch.to.text;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gcm.GCMRegistrar;

import edu.ucsb.cs290.touch.to.text.crypto.DatabaseHelper;
import edu.ucsb.cs290.touch.to.text.crypto.IntentDatabaseHelper;
import edu.ucsb.cs290.touch.to.text.crypto.KeyPairsProvider;
import edu.ucsb.cs290.touch.to.text.https.TorProxy;
import edu.ucsb.cs290.touch.to.text.remote.Helpers;
import edu.ucsb.cs290.touch.to.text.remote.messages.ProtectedMessage;
import edu.ucsb.cs290.touch.to.text.remote.register.RegisterUser;

public class KeyManagementService extends Service {
	private DatabaseHelper dbHelperInstance;
	private volatile KeyPairsProvider kp;
	private Timer timer;
	private static final String TAG = KeyManagementService.class.getSimpleName();
	private final IBinder binder = new KeyCachingBinder();
	private static final int SERVICE_RUNNING_ID = 155296813;
	private static final String CLEAR_MEMORY = "edu.ucsb.cs290.touch.to.text.ClearMemory";
	static final String EXIT = "edu.ucsb.cs290.touch.to.text.Exit";
	static final String UPDATE_REG = "edu.ucsb.cs290.touch.to.text.reg";
	public static final String MESSAGE_RECEIVED = "edu.ucsb.cs290.touch.to.text.MESSAGE_RECEIVED";
	public static final String REFRESH_VIEWS = "edu.ucsb.cs290.touch.to.text.REFRESH_VIEWS";


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
		// timer.schedule(expireTask, 1000L, 60 * 1000L);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Service destroying");
		((NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE))
				.cancel(SERVICE_RUNNING_ID);
		timer.cancel();
		timer = null;
	}

	@Override
	public int onStartCommand(Intent intent, int i, int j) {
		
		Log.i("kmg", "On start command called");
		if (intent != null && CLEAR_MEMORY.equals(intent.getAction())) {
			clearKey();
			LocalBroadcastManager.getInstance(this).sendBroadcastSync(
					new Intent(EXIT));
		}
		if ((intent != null && UPDATE_REG.equals(intent.getAction()))
				|| this.getSharedPreferences("touchToTextPreferences.xml",
						MODE_PRIVATE).getBoolean("GCM ready", false)) {
			return handleUpdateReg();
		}
		if (intent != null && intent.getAction() != null && MESSAGE_RECEIVED.equals(intent.getAction())) {
			return handleMessageReceived(intent);
		} else if(intent!= null &&  intent.getAction() != null && REFRESH_VIEWS.equals(intent.getAction())) {
			setCustomNotification();
		}

		return START_STICKY;
	}

	private int handleUpdateReg() {
		if (getInstance().initialized()) {
			Log.d("touch-to-text", "Sending server reg key");
			this.getSharedPreferences("touchToTextPreferences.xml",
					MODE_PRIVATE).edit().remove("GCM ready").commit();
			new AsyncTask<String, Void, Void>() {

				@Override
				protected Void doInBackground(String... params) {
					try {
						TorProxy.postThroughTor(getApplicationContext(),
								new RegisterUser(params[0], getInstance()
										.getTokenKeyPair(), 1000));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (GeneralSecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

			}.execute(GCMRegistrar
					.getRegistrationId(getApplicationContext()));
			return START_REDELIVER_INTENT;
		} else {
			this.getSharedPreferences("touchToTextPreferences.xml",
					MODE_PRIVATE).edit().putBoolean("GCM ready", true)
					.commit();
			return START_NOT_STICKY;
		}
	}

	private int handleMessageReceived(Intent intent) {
		setCustomNotification();
		if (dbHelperInstance != null && dbHelperInstance.initialized()) {
			try {
				final String stringExtra = intent.getStringExtra("message");
				final byte[] decode = Base64.decode(
						stringExtra,
						Base64.DEFAULT);
				final ProtectedMessage deserialize = (ProtectedMessage) Helpers
						.deserialize(decode);
				dbHelperInstance
						.addIncomingMessage(deserialize);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(REFRESH_VIEWS));
			} catch (GeneralSecurityException e) {
				Log.wtf(TAG, "Security problem!",e);
			} 
		} else {
			IntentDatabaseHelper.getInstance(getApplicationContext()).addIntentToDB(intent);
			stopSelf();
		}
		return START_REDELIVER_INTENT;
	}

	private void clearKey() {
		dbHelperInstance.forgetPassword();
		kp = null;
		this.stopSelf();
	}

	@TargetApi(16)
	public void startNotification() {
		RemoteViews remoteView = new RemoteViews(getPackageName(),
				R.layout.notification_message);
		Intent clearMemory = new Intent(this, KeyManagementService.class);
		clearMemory.setAction(CLEAR_MEMORY);
		PendingIntent clearMemoryIntent = PendingIntent.getService(
				getApplicationContext(), 0, clearMemory, 0);
		remoteView.setOnClickPendingIntent(R.id.lock_cache_icon,
				clearMemoryIntent);
		Builder builder = new Notification.Builder(this);
		builder.setSmallIcon(android.R.drawable.ic_lock_lock)
				.setContentTitle("Touch to Text is Running")
				.setContentText("Touch Lock to Clear Memory")
				.setWhen(System.currentTimeMillis()).setContent(remoteView)
				.setOngoing(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			builder.setPriority(Notification.PRIORITY_LOW);
		}
		Notification statusNotification = builder.build();
		stopForeground(true);
		startForeground(SERVICE_RUNNING_ID, statusNotification);
	}
	
	@TargetApi(16)
	public void setCustomNotification() {
		Intent showList = new Intent(this, ConversationListActivity.class);
		PendingIntent showActivityIntent = PendingIntent.getService(
				getApplicationContext(), 0, showList, 0);
		Builder builder = new Notification.Builder(this);
		builder.setSmallIcon(android.R.drawable.ic_lock_lock)
				.setContentTitle("Touch to Text Secure Messaging")
				.setContentText("New Message(s)")
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setDefaults(Notification.DEFAULT_ALL)
				.setOnlyAlertOnce(true)
				.setContentIntent(showActivityIntent)
				.setDeleteIntent(showActivityIntent);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			builder.setPriority(Notification.PRIORITY_HIGH);
		}
		Notification statusNotification = builder.build();
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(SERVICE_RUNNING_ID+1,statusNotification);
	}

	public void loggedIn() {
		Cursor c = IntentDatabaseHelper
				.getInstance(getApplicationContext())
				.getIntentsCursor();
		try {
			for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
				final byte[] blob = c.getBlob(c.getColumnIndex(IntentDatabaseHelper.INTENT_BODY));
				final Parcel obtain = Parcel.obtain();
				obtain.unmarshall(blob, 0,blob.length);
				obtain.setDataPosition(0);
				sendBroadcast((Intent) obtain.readValue(Intent.class.getClassLoader()));
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			c.close();
		}
	}
}
