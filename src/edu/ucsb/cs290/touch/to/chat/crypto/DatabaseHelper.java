package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import edu.ucsb.cs290.touch.to.chat.crypto.CryptoContacts.Contact;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.SignedMessage;

/**
 * 
 * Instantiate and provide access to the DB, which contains Messages, Contacts,
 * and secure private key storage.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
	
	// Encrypted Preferences File
	private static final String TOUCH_TO_TEXT_PREFERENCES_XML = "touchToTextPreferences.xml";
	
	// DB Strings
	public static final String MESSAGES_TABLE = "Messages";
	public static final String CONTACTS_TABLE = "Contacts";

	// Messages Table
	public static final String MESSAGES_ID = "_id";
	private static final String SENDER_ID = "sender";
	private static final String RECIPIENT_ID = "recipient";
	private static final String DATE_TIME = "dateTime";
	private static final String READ = "read"; // 1 if read, 0 for unread
	private static final String MESSAGE_BODY = "messageBody";
	
	// Contacts Table
	public static final String CONTACTS_ID = "_id";
	private static final String NICKNAME = "nickname";
	private static final String CONTACT_ID = "contactId";
	private static final String CONTACT_NOTE = "note";
	private static final String VERIFIED_BY = "verifiers";
	private static final String PUBLIC_KEY = "publicKey";
	
	// My contact ID
	private static final long MY_CONTACT_ID = -1;


	private static final String CREATE_MESSAGES_COMMAND = 
			"CREATE TABLE " + MESSAGES_TABLE + " (  " 
					+ MESSAGES_ID + " INTEGER PRIMARY KEY autoincrement, "
					+ SENDER_ID + " INTEGER, "
					+ RECIPIENT_ID + " INTEGER, "
					+ DATE_TIME + " INTEGER, " 
					+ READ + " INTEGER DEFAULT 0, " 
					+ MESSAGE_BODY + " BLOB);";

	private static final String CREATE_CONTACTS_COMMAND =
			"CREATE TABLE " + CONTACTS_TABLE + " ( " 
					+ CONTACTS_ID + " integer PRIMARY KEY autoincrement, " 
					+ NICKNAME + " TEXT, "
					+ PUBLIC_KEY + " BLOB, "
					+ DATE_TIME + " INTEGER, " 
					+ VERIFIED_BY + " TEXT, "
					+ CONTACT_NOTE + " TEXT);";

	private static final String DATABASE_NAME = "touchToText.db";
	private static final int DATABASE_VERSION = 1;

	// Databases and Context
	private SQLiteDatabase db;
	private MasterPassword passwordInstance = null;
	private Context context;
	private SealablePublicKey publicKey;

	public static final String[] CONTACTS_QUERY = new String[] { /*
	 * CONTACTS_ID,
	 * PUBLIC_KEY,
	 */NICKNAME /*
	 * ,
	 * DATE_TIME
	 */};

	public static final String[] MESSAGES_QUERY = new String[] { /*
	 * MESSAGE_BODY,
	 * PUBLIC_KEY,
	 */ MESSAGE_BODY/*
	 * ,
	 * DATE_TIME
	 */};


	public DatabaseHelper(Context ctx) {
		// calls the super constructor, requesting the default cursor factory.
		super(ctx.getApplicationContext(), DATABASE_NAME, null,
				DATABASE_VERSION);
		context = ctx;
	}

	public boolean initialized() {
		return passwordInstance != null;
	}

	/**
	 * Create the tables and set a password
	 * 
	 * @param password
	 */
	public void initalizeInstance(String password) {
		Log.i("db", "Intializing database");
		if (passwordInstance == null) {
			setPassword(password);
			SQLiteDatabase.loadLibs(context);
			db = this.getWritableDatabase(password);
		}
	}

	public void setPassword(String password) {
		if (passwordInstance != null) {
			passwordInstance.forgetPassword();
		}
		passwordInstance = MasterPassword.getInstance(password);
	}

	public void forgetPassword() {
		passwordInstance.forgetPassword();
	}

	/**
	 * Erase the entire database file.
	 * 
	 * @return true if DB was deleted, false otherwise.
	 */
	public boolean wipeDB() {
		if (db != null) {
			if(tableExists(MESSAGES_TABLE)) {
				db.rawExecSQL("DROP TABLE " + MESSAGES_TABLE);
			}
			if(tableExists(CONTACTS_TABLE)) {
				db.rawExecSQL("DROP TABLE " + CONTACTS_TABLE);
			}
			createTables(db);
			return true;
		}
		return false;
	}

	private void createTables(SQLiteDatabase db) {
		db.execSQL(CREATE_MESSAGES_COMMAND);
		db.execSQL(CREATE_CONTACTS_COMMAND);
	}

	private boolean tableExists(String table_name) {

		String condition = "tbl_name = ?";
		Cursor cursor = getReadableDatabase("password")
				.query("sqlite_master", new String[] { "tbl_name" },
						condition, new String[] { table_name }, null,null,null);

		if (cursor != null) {
			if (cursor.getCount() > 0) {
				cursor.close();
				return true;
			}
			cursor.close();
		}
		return false;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
		GenerateKeysTask task = new GenerateKeysTask();
		task.execute(new String[] { null });
	}

	// Don't do anything on upgrade! But must implement to work with schema
	// changes.
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public SealablePublicKey getPGPPublicKey() {
		SecurePreferences encryptedPublicKey = new SecurePreferences(context,
				TOUCH_TO_TEXT_PREFERENCES_XML,
				passwordInstance.getPasswordString(), true);

		String publicKeyString = encryptedPublicKey.getString(PUBLIC_KEY);
		KeyPairsProvider kp = (KeyPairsProvider) Helpers.deserialize(Base64
				.decode(publicKeyString, Base64.DEFAULT));
		publicKey = kp.getExternalKey();
		return publicKey;
	}

	public KeyPair getSigningKey() {
		SecurePreferences encryptedPublicKey = new SecurePreferences(context,
				TOUCH_TO_TEXT_PREFERENCES_XML,
				passwordInstance.getPasswordString(), true);

		String publicKeyString = encryptedPublicKey.getString(PUBLIC_KEY);
		KeyPairsProvider kp = (KeyPairsProvider) Helpers.deserialize(Base64
				.decode(publicKeyString, Base64.DEFAULT));
		return kp.getSigningKey();
	}

	public void addContact(CryptoContacts.Contact newContact) {
		AddContactsToDBTask task = new AddContactsToDBTask();
		task.execute(new CryptoContacts.Contact[] { newContact });
	}


	public void addOutgoingMessage(final SignedMessage signedMessage,
		CryptoContacts.Contact contact) {
		long time=0;
		try {
			time = signedMessage.getMessage(getSigningKey().getPublic()).getTimeSent().getTime();
		} catch (GeneralSecurityException e) {
			Log.wtf("Touch-to-text", "Your keys may have been tampered with!?!?", e);
		} catch (IOException e) {
			Log.d("Touch-to-text", "Error deserializing signed message", e);
			time = System.currentTimeMillis();
		} catch (ClassNotFoundException e) {
			Log.d("Touch-to-text", "Error, class not found in addOutgoingMessage", e);
			time = System.currentTimeMillis();
		}
		
		AddMessageToDBTask task = new AddMessageToDBTask();
		ContentValues newMessage = new ContentValues();
		newMessage.put(MESSAGE_BODY, Helpers.serialize(signedMessage));
		newMessage.put(DATE_TIME, time );
		newMessage.put(RECIPIENT_ID, contact.getID());
		newMessage.put(SENDER_ID, MY_CONTACT_ID);
		newMessage.put(READ, 1);
		task.execute(new ContentValues[] { newMessage });
		// For sorting purposes, update last contacted.
		UpdateLastContactedTask last = new UpdateLastContactedTask();
		ContentValues updateTime = new ContentValues();
		updateTime.put(CONTACTS_ID, contact.getID());
		updateTime.put(DATE_TIME, time);
		last.execute(new ContentValues[] { updateTime });
	}

	private class UpdateLastContactedTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... toAdd) {
			for (ContentValues val : toAdd) {
				getReadableDatabase(passwordInstance.getPasswordString())
				.update(CONTACTS_TABLE, val,
						CONTACTS_ID + "=" + val.getAsLong(CONTACTS_ID),
						null);
			}
			return null;
		}
	}

	public Cursor getContactsCursor() {
		// TODO: Need to update DATE_TIME when message is sent or received for
		// this contact.
		String sortOrder = DATE_TIME + " DESC";
		Cursor cursor = getReadableDatabase(
				passwordInstance.getPasswordString()).query(CONTACTS_TABLE,
						new String[] { CONTACTS_ID, PUBLIC_KEY, NICKNAME, DATE_TIME },
						null, null, null, null, sortOrder);
		return cursor;
	}

	private class AddMessageToDBTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... toAdd) {
			for (ContentValues val : toAdd) {
				getReadableDatabase(passwordInstance.getPasswordString())
				.insert(MESSAGES_TABLE, null, val);
			}
			return null;
		}
	}

	private class AddContactsToDBTask extends
	AsyncTask<CryptoContacts.Contact, Void, Void> {
		@Override
		protected Void doInBackground(CryptoContacts.Contact... toAdd) {
			for (CryptoContacts.Contact newContact : toAdd) {
				ContentValues newUser = new ContentValues();
				newUser.put(NICKNAME, newContact.toString());
				newUser.put(PUBLIC_KEY,
						Helpers.serialize(newContact.getSealablePublicKey()));
				newUser.put(DATE_TIME, System.currentTimeMillis());
				getReadableDatabase(passwordInstance.getPasswordString())
				.insert(CONTACTS_TABLE, null, newUser);
			}
			return null;
		}
	}

	public Cursor getMessagesCursor(String id) {
		Cursor cursor = null;
		String sortOrder = DATE_TIME + " ASC";
		String condition = RECIPIENT_ID + "=? OR " + SENDER_ID + "=?";
		cursor = getReadableDatabase(passwordInstance.getPasswordString())
				.query(MESSAGES_TABLE,
						new String[] { MESSAGES_ID, DATE_TIME, MESSAGE_BODY, SENDER_ID, RECIPIENT_ID },
						condition, new String[] {id, id}, null, null, sortOrder);

		return cursor;
	}

	public Cursor getContactCursor(String name) {
		String sortOrder = DATE_TIME + " DESC";
		String condition = NICKNAME + "=?";
		Cursor cursor = getReadableDatabase(passwordInstance.getPasswordString()).query(
				CONTACTS_TABLE, 
				new String[] {CONTACTS_ID,	PUBLIC_KEY, DATE_TIME}
				, condition, new String[] { name }, null, null, sortOrder);
		return cursor;
	}

	private class GenerateKeysTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			// Add "generating keys" notification
		}

		@Override
		protected Void doInBackground(String... names) {
			SecurePreferences encryptedPublicKey = new SecurePreferences(
					context, TOUCH_TO_TEXT_PREFERENCES_XML,
					passwordInstance.getPasswordString(), true);
			KeyPairsProvider kp = new KeyPairsProvider();
			byte[] b = Helpers.serialize(kp);
			String publicKeyString = Base64.encodeToString(b, Base64.DEFAULT);
			encryptedPublicKey.put(PUBLIC_KEY, publicKeyString);
			return null;
		}

		@Override
		protected void onPostExecute(Void evil) {
			// Set "done generating keys" notification
		}
	}

}
