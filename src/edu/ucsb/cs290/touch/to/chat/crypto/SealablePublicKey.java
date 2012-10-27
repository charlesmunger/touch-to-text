package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.util.encoders.Base64;
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Serializable {
	
	PublicKey publicKey;
	SignedObject signedToken;

	public SealablePublicKey(KeyPair keys, String identity) {
		this.publicKey = keys.getPublic();
		try {
			this.signedToken = new SignedObject(UUID.randomUUID(), keys.getPrivate(), Signature.getInstance("DSA", "SC"));
		} catch (GeneralSecurityException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "Security error creating exchange object", e);
		} catch (IOException i) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "Error serializing object", i);
		}
	}
	
	public String digest() {
		
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
			byte[] digest = sha1.digest(Helpers.serialize(publicKey));
			return new String(Base64.encode(digest));
		} catch (NoSuchAlgorithmException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"SHA1 is missing!", e);
			return "Key Verification Error";
		}
	}

	public SignedObject token() {
		return signedToken;
	}
}
