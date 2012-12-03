package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.SignedObject;

import android.database.Cursor;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;

public class Contact implements Serializable {
	private final PublicKey signingKey;
	private final PublicKey encryptingKey;
	private final SignedObject so;
	final String name;
	private long id;

	public Contact(Cursor c) {
		this(c.getString(c.getColumnIndex(DatabaseHelper.NICKNAME)), 
			(SealablePublicKey) Helpers.deserialize(c.getBlob(c.getColumnIndex(DatabaseHelper.PUBLIC_KEY))), 
			c.getLong(c.getColumnIndex(DatabaseHelper.CONTACTS_ID)));
	}
	
	public Contact(String name, PublicKey signing, PublicKey encrypting, SignedObject so, long id) {
		this.signingKey = signing;
		this.encryptingKey = encrypting;
		this.name = name;
		this.so = so;
		this.id = id;
	}
	
	public Contact(String name, SealablePublicKey spk, long id) {
		this(name,spk.sign(),spk.encrypt(),spk.token(), id);
	}

	public PublicKey getSigningKey() {
		return signingKey;
	}

	public PublicKey getEncryptingKey() {
		return encryptingKey;
	}
	
	public SignedObject getToken() {
		return so;
	}

	public String toString() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public SealablePublicKey getSealablePublicKey() {
		return new SealablePublicKey(signingKey, encryptingKey, so);
	}

	public long getID() {
		return id;
	}
	
	public void setID(long newID) {
		id = newID;
	}
}