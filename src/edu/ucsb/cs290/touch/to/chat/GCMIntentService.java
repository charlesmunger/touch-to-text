package edu.ucsb.cs290.touch.to.chat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import edu.ucsb.cs290.touch.to.chat.KeyManagementService.KeyCachingBinder;
import edu.ucsb.cs290.touch.to.chat.crypto.DatabaseHelper;
import edu.ucsb.cs290.touch.to.chat.crypto.IntentDatabaseHelper;
import edu.ucsb.cs290.touch.to.chat.https.TorProxy;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.ProtectedMessage;
import edu.ucsb.cs290.touch.to.chat.remote.register.RegisterUser;

public class GCMIntentService extends GCMBaseIntentService {

	private void handleMessage(Intent intent) {
		addMessageToDb((ProtectedMessage) Helpers.deserialize(Base64.decode(
				intent.getStringExtra("message"), Base64.DEFAULT)));
	}

	private void handleRegistration(String regID) {
		if (KeyManagementService.getStatic() == null) {
			this.getSharedPreferences("touchToTextPreferences.xml",
					MODE_PRIVATE).edit().putBoolean("GCM ready", true).commit();
		} else {
			Intent i = new Intent(getApplicationContext(),
					KeyManagementService.class);
			i.setAction(KeyManagementService.UPDATE_REG);
			LocalBroadcastManager.getInstance(getApplicationContext())
					.sendBroadcastSync(i);
		}
	}

	private void addMessageToDb(ProtectedMessage pm) {
		try {
			KeyManagementService.getStatic().addIncomingMessage(pm);
		} catch (GeneralSecurityException g) {
			// TODO notify user of attack
		}
		Intent i = new Intent("edu.ucsb.cs290.touch.to.chat.MESSAGE_RECIEVED");
		LocalBroadcastManager.getInstance(getApplicationContext())
				.sendBroadcastSync(i);
	}

	private void addIntentToDb(Intent intent) {
		IntentDatabaseHelper.getInstance(getApplicationContext())
				.addIntentToDB(intent);
	}

	@Override
	protected void onError(Context arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onMessage(Context arg0, Intent arg1) {
		DatabaseHelper ref = KeyManagementService.getStatic();
		if (ref == null) {
			addIntentToDb(arg1);
		} else {
			handleMessage(arg1);
		}
	}

	@Override
	protected void onRegistered(Context arg0, String arg1) {
		// TODO Auto-generated method stub
		handleRegistration(arg1);
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		// TODO Auto-generated method stub

	}
}
