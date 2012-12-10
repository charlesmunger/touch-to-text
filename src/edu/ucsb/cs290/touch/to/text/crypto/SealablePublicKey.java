package edu.ucsb.cs290.touch.to.text.crypto;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.UUID;

import org.spongycastle.util.encoders.Base64;

import android.util.Log;
import edu.ucsb.cs290.touch.to.text.remote.Helpers;

public class SealablePublicKey implements Serializable {
	
	/**
	 * Version 1L has an ElGamal publicKey, and two DSA public keys. It also has a SignedObject containing a UUID.
	 */
	private static final long serialVersionUID = 1L;
	private final PublicKey signingKey;
	private final PublicKey encryptionKey;
	private final PublicKey tokenKey;
	private final SignedObject signedToken;

	public SealablePublicKey(PublicKey signingKey, PublicKey encryptingKey, KeyPair tokenKey) {
		this.signingKey = signingKey;
		this.encryptionKey = encryptingKey;
		this.tokenKey = tokenKey.getPublic();
		SignedObject st = null;
		try {
			st = new SignedObject(UUID.randomUUID(), tokenKey.getPrivate(), Signature.getInstance("DSA", "SC"));
		} catch (GeneralSecurityException e) {
			Log.w("touch-to-text","Security error creating exchange object", e);
		} catch (IOException i) {
			Log.w("touch-to-text", "Error serializing object", i);
		}
		this.signedToken = st;
	}
	
	public SealablePublicKey(PublicKey signingKey, PublicKey encryptingKey, PublicKey tokenKey, SignedObject signedToken) {
		this.signingKey = signingKey;
		this.encryptionKey = encryptingKey;
		this.signedToken = signedToken;
		this.tokenKey = tokenKey;
	}

	public String digest() {
		
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1","SC");
			byte[] a = Helpers.serialize(signingKey);
			byte[] b = Helpers.serialize(encryptionKey);
			byte[] c = new byte[a.length + b.length];
			System.arraycopy(a, 0, c, 0, a.length);
			System.arraycopy(b, 0, c, a.length, b.length);
			byte[] digest = sha1.digest(c);
			return new String(Base64.encode(digest));
		} catch (GeneralSecurityException e) {
			Log.wtf("touch-to-text","SHA1 is missing!", e);
			return "Key Verification Error";
		}
	}

	public SignedObject token() {
		return signedToken;
	}
	
	public PublicKey encrypt() {
		return encryptionKey;
	}
	
	public PublicKey sign() {
		return signingKey;
	}
	
	public PublicKey address() {
		return tokenKey;
	}
	
	public String signingKeyFingerprint() {
		return Helpers.getKeyFingerprint(signingKey);
	}
}
