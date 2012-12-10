package edu.ucsb.cs290.touch.to.text;

import java.io.Serializable;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import android.content.Intent;
import edu.ucsb.cs290.touch.to.text.crypto.SealablePublicKey;

public class EndKeyExchangeActivity extends AbstractNFCExchangeActivity {
	private SecretKey aesKey;
	private SealablePublicKey p;

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
		SealablePublicKey p = mService.getInstance().getSealablePublicKey();
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
