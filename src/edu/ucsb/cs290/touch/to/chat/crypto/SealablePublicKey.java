package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.util.encoders.Base64;

import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Serializable {
	
	private final PublicKey signingKey;
	private final PublicKey encryptionKey;
	private final SignedObject signedToken;

	public SealablePublicKey(KeyPair signingKeys, PublicKey encryptingKey) {
		this.signingKey = signingKeys.getPublic();
		this.encryptionKey = encryptingKey;
		SignedObject st = null;
		try {
			st = new SignedObject(UUID.randomUUID(), signingKeys.getPrivate(), Signature.getInstance("DSA", "SC"));
		} catch (GeneralSecurityException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "Security error creating exchange object", e);
		} catch (IOException i) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "Error serializing object", i);
		}
		this.signedToken = st;
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
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"SHA1 is missing!", e);
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
}
