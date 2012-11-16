package edu.ucsb.cs290.touch.to.chat.crypto;

import java.security.PublicKey;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;


public class CryptoContacts {
	public static class Contact {
		private final PublicKey signingKey;
		private final PublicKey encryptingKey;
		private final SignedObject so;
		private final String name;

		public Contact(String name, PublicKey signing, PublicKey encrypting, SignedObject so) {
			this.signingKey = signing;
			this.encryptingKey = encrypting;
			this.name = name;
			this.so = so;
		}
		
		public Contact(String name, SealablePublicKey spk) {
			this(name,spk.sign(),spk.encrypt(),spk.token());
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
	}

	public static void getContacts() {
		DatabaseHelper.getInstance(null).getAllContacts();
	}


	public static List<Contact> ITEMS = new ArrayList<CryptoContacts.Contact>();
	public static Map<String,CryptoContacts.Contact> ITEM_MAP = new HashMap<String, CryptoContacts.Contact>(); 

	public static void addContact(Contact newContact) {
		ITEMS.add(newContact);
		ITEM_MAP.put(newContact.name, newContact);
	}
	
}
