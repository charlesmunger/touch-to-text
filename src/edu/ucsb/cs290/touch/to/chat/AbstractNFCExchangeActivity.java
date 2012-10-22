package edu.ucsb.cs290.touch.to.chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

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

	public void recieve(byte[] b) {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			recieveObject(in.readObject());
		} catch (Exception e) {
			// TODO handle more gracefully
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Error decrypting payload", e);
		} finally {
			try {
				bis.close();
				in.close();
			} catch (Exception e) {
			}
		}
	}

	public abstract void done();
	
	public byte[] send() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] yourBytes = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(sendObject());
			yourBytes = bos.toByteArray();
		} catch (Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Error serializing public key", e);
		} finally {
			try {
				out.close();
				bos.close();
			} catch (Exception e) {
			} // TODO handle elegantly
		}
		return yourBytes;
	}
	
	public abstract Serializable sendObject() throws Exception;
	
	public abstract void recieveObject(Object o) throws Exception;
	
	public abstract void parseIntent(Intent i);
}
