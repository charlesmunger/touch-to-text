package edu.ucsb.cs290.touch.to.chat;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ucsb.cs290.touch.to.chat.crypto.KeyExchange;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.view.Menu;

public abstract class AbstractNFCExchangeActivity extends Activity {
	private IntentFilter[] intentFiltersArray;
	private PendingIntent pendingIntent;
	private String[][] mTechLists;
	private NfcAdapter mAdapter;

	private NdefMessage message;
	private boolean sent = false;
	private boolean received = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		parseIntent(getIntent());
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		setResult(RESULT_CANCELED);
		message = new NdefMessage(new NdefRecord[] { new NdefRecord(
				NdefRecord.TNF_UNKNOWN, new byte[0], new byte[0],
				send())});

		intentFiltersArray = new IntentFilter[] { ndef, };
		mTechLists = new String[][] { new String[] { Ndef.class.getName() } };
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		mAdapter.setNdefPushMessage(message, this);
		mAdapter.setOnNdefPushCompleteCallback(new NfcAdapter.OnNdefPushCompleteCallback() {
			
			@Override
			public void onNdefPushComplete(NfcEvent event) {
				sent = true;
				checkDone();				
			}
		}, this);
	}

	private void checkDone() {
		if (sent && received) {
			done();
		}
	}

	public void onNewIntent(Intent intent) {
		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		NdefMessage m = Ndef.get(tagFromIntent).getCachedNdefMessage();
		byte[] b = m.getRecords()[0].getPayload();
		try {
			recieve(b);
			received = true;
		} catch (Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Exception in key exchange!", e);
			setResult(RESULT_CANCELED, new Intent());
		}
		checkDone();
	}
	
	public void onPause() {
		super.onPause();
		mAdapter.disableForegroundDispatch(this);
	}

	public void onResume() {
		super.onResume();
		mAdapter.enableForegroundDispatch(this, pendingIntent,
				intentFiltersArray, mTechLists);
	}

	public abstract void recieve(byte[] b);

	public abstract void done();
	
	public abstract byte[] send();
	
	public abstract void parseIntent(Intent i);
}
