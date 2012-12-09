package edu.ucsb.cs290.touch.to.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class MessageReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessageReceived");
		intent.setAction(KeyManagementService.MESSAGE_RECEIVED);
		context.startService(intent);
	}

}
