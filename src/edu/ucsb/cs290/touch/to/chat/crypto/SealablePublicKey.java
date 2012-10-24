package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.util.encoders.Base64;
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Serializable {
	
	byte[] publicKey;
	String identity;

	public SealablePublicKey(byte[] publicKey, String identity) {
		this.identity = identity;
		this.publicKey = publicKey;
	}
	
	public String digest() {
		
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
			byte[] digest = sha1.digest(publicKey);
			return new String(Base64.encode(digest));
		} catch (NoSuchAlgorithmException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"SHA1 is missing!", e);
			return "Key Verification Error";
		}
	}
}
