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
		private final PublicKey p;
		private final SignedObject so;
		private final String name;

		public Contact(String name, PublicKey p, SignedObject so) {
			this.p = p;
			this.name = name;
			this.so = so;
		}
		
		public Contact(String name, SealablePublicKey spk) {
			this.name = name;
			this.p = spk.publicKey;
			this.so = spk.token();
		}

		public PublicKey getKey() {
			return p;
		}

		public SignedObject getToken() {
			return so;
		}

		public String toString() {
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
