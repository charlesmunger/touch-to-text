package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;

import android.util.Log;

public class KeyExchange implements Serializable {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private KeyPair keypair;
	public KeyExchange() {
		this(256);
	}

	public KeyExchange(int keyBits) {
		try {
			ECNamedCurveParameterSpec dhSpec = ECNamedCurveTable.getParameterSpec("prime192v1");

			// Use the values to generate a key pair
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH","SC");
			keyGen.initialize(dhSpec);
			keypair = keyGen.generateKeyPair();
		} catch (Exception e) {
			Log.d("touch-to-text","Exception in key exchange", e);
		}
	}
	
	public  PublicKey getPublicKey() {
		return keypair.getPublic();
	}
	
	public SecretKey setOtherPublicKey(ECPublicKey keySpec,String algorithm) throws GeneralSecurityException {
		if(keypair == null) {
			throw new UnsupportedOperationException("Do not reuse key exchange objects.");
		}
		KeyAgreement ka = KeyAgreement.getInstance("ECDH", "SC");
	    ka.init(keypair.getPrivate());
	    ka.doPhase(keySpec, true);
	    keypair = null; //Ensure not reused
	    return ka.generateSecret(algorithm);
	}
}
