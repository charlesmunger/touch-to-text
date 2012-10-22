package edu.ucsb.cs290.touch.to.chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import edu.ucsb.cs290.touch.to.chat.crypto.SealablePublicKey;

public class EndKeyExchangeActivity extends AbstractNFCExchangeActivity {
	private SecretKey aesKey;

	private SealablePublicKey p;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_end_key_exchange);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_end_key_exchange, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void recieve(byte[] b) {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		ObjectInput in = null;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, aesKey);
			in = new ObjectInputStream(bis);
			SealedObject o = (SealedObject) in.readObject();
			p = (SealablePublicKey) o.getObject(c);
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

	@Override
	public void done() {
		Intent i = new Intent();
		i.putExtra("key_package", p);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public byte[] send() {
		// somehow acquire an object...
		SealablePublicKey p = new SealablePublicKey();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] yourBytes = null;
		try {
			out = new ObjectOutputStream(bos);
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, aesKey);
			SealedObject s = new SealedObject(p,c);
			out.writeObject(s);
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

	@Override
	public void parseIntent(Intent i) {
		aesKey = (SecretKey) getIntent().getSerializableExtra("AES key");
	}

}
