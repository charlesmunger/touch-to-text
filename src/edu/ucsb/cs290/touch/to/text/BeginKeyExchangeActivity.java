package edu.ucsb.cs290.touch.to.text;

import java.io.Serializable;
import java.security.interfaces.ECPublicKey;

import javax.crypto.SecretKey;

import android.content.Intent;
import edu.ucsb.cs290.touch.to.text.crypto.KeyExchange;

public class BeginKeyExchangeActivity extends AbstractNFCExchangeActivity {
	private KeyExchange keyExchange;
	private SecretKey encodedKey;

	@Override
	protected void done() {
		Intent i = new Intent(this,EndKeyExchangeActivity.class);
		i.putExtra("AES key", encodedKey);
		startActivityForResult(i, 1);
	}

	@Override
	protected Serializable sendObject() {
		return keyExchange.getPublicKey();
	}
	
	@Override
	protected void parseIntent(Intent i) {
		keyExchange = (KeyExchange) getIntent().getSerializableExtra("keyExchange");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(resultCode) {
			case RESULT_CANCELED: return;
			case RESULT_OK: setResult(RESULT_OK, data);
			finish();
		}
	}

	@Override
	protected void recieveObject(Object o) throws Exception {
		encodedKey = keyExchange.setOtherPublicKey((ECPublicKey) o, "AES");
	}
}
