package edu.ucsb.cs290.touch.to.text.crypto;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.SignedObject;

import android.database.Cursor;
import edu.ucsb.cs290.touch.to.text.remote.Helpers;

public class Contact implements Serializable {
	/**
	 * Three public keys, a name, and a signedObject.
	 */
	private static final long serialVersionUID = 1L;
	private final PublicKey signingKey;
	private final PublicKey encryptingKey;
	private final PublicKey tokenKey;
	private final SignedObject tokenToSend;

	private final String name;
	private final long id;

	public Contact(Cursor c) {
		this(c.getString(c.getColumnIndex(DatabaseHelper.NICKNAME)), 
				(SealablePublicKey) Helpers.deserialize(c.getBlob(c.getColumnIndex(DatabaseHelper.PUBLIC_KEY))),
				((SealablePublicKey) Helpers.deserialize(c.getBlob(c.getColumnIndex(DatabaseHelper.PUBLIC_KEY)))).token(), 
				c.getLong(c.getColumnIndex(DatabaseHelper.CONTACTS_ID)));
	}

	public Contact(String name, PublicKey signing, PublicKey encrypting, PublicKey tokenKey,SignedObject so, long id) {
		this.signingKey = signing;
		this.encryptingKey = encrypting;
		this.tokenKey = tokenKey;
		this.name = name;
		this.tokenToSend = so;
		this.id = id;
	}

	public Contact(Contact c, SignedObject token) {
		this(c.name,c.signingKey,c.encryptingKey,c.tokenKey,token,c.id);
	}

	public Contact(String name, SealablePublicKey key, SignedObject token,
			long newContactId) {
		this.signingKey = key.sign();
		this.encryptingKey = key.encrypt();
		this.tokenKey = key.address();
		this.name = name;
		this.tokenToSend = token;
		this.id = newContactId;	
	}

	public PublicKey getSigningKey() {
		return signingKey;
	}

	public PublicKey getEncryptingKey() {
		return encryptingKey;
	}

	public SignedObject getToken() {
		return tokenToSend;
	}

	public String toString() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public PublicKey getTokenKey() {
		return tokenKey;
	}

	public SealablePublicKey getSealablePublicKey() {
		return new SealablePublicKey(signingKey, encryptingKey,tokenKey, tokenToSend);
	}

	public long getID() {
		return id;
	}

}