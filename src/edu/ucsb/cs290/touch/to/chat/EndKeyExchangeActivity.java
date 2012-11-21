package edu.ucsb.cs290.touch.to.chat;

import java.io.Serializable;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import edu.ucsb.cs290.touch.to.chat.crypto.DatabaseHelper;
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
	public void done() {
		Intent i = new Intent();
		i.putExtra("key_package", p);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void parseIntent(Intent i) {
		aesKey = (SecretKey) getIntent().getSerializableExtra("AES key");
	}

	@Override
	public Serializable sendObject() throws Exception {
		SealablePublicKey p = mService.getInstance().getPGPPublicKey();
		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, aesKey);
		// Get Our PGP Public Key
		return new SealedObject(p,c);
	}

	@Override
	public void recieveObject(Object sealed) throws Exception {
		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, aesKey);
		SealedObject o = (SealedObject) sealed;
		
		p = (SealablePublicKey) o.getObject(c);
	}
}
