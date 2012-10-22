package edu.ucsb.cs290.touch.to.chat;

import java.io.Serializable;
import java.security.interfaces.ECPublicKey;

import javax.crypto.SecretKey;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import edu.ucsb.cs290.touch.to.chat.crypto.KeyExchange;

public class BeginKeyExchangeActivity extends AbstractNFCExchangeActivity {
	private KeyExchange keyExchange;
	private SecretKey encodedKey;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_begin_key_exchange);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_begin_key_exchange, menu);
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
		Intent i = new Intent(this,EndKeyExchangeActivity.class);
		i.putExtra("AES key", encodedKey);
		startActivityForResult(i, 1);
	}

	@Override
	public Serializable sendObject() {
		return keyExchange.getPublicKey();
	}
	
	public void parseIntent(Intent i) {
		keyExchange = (KeyExchange) getIntent().getSerializableExtra("keyExchange");
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(resultCode) {
		case RESULT_CANCELED: return;
		case RESULT_OK: setResult(RESULT_OK, data);
		finish();
		}
	}

	@Override
	public void recieveObject(Object o) throws Exception {
		encodedKey = keyExchange.setOtherPublicKey((ECPublicKey) o, "AES");
	}
}
