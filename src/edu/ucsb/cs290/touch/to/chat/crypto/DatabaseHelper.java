package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.Message;
import edu.ucsb.cs290.touch.to.chat.remote.messages.SignedMessage;

/**
 * 
 * Instantiate and provide access to the DB, which contains Messages, Contacts,
 * and secure private key storage.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TOUCH_TO_TEXT_PREFERENCES_XML = "touchToTextPreferences.xml";
	// DB Strings
	public static final String MESSAGES_TABLE = "Messages";
	public static final String MESSAGES_ID = "messages_id";
	public static final String THREAD_ID = "threadId";
	public static final String CONTACTS_TABLE = "Contacts";
	public static final String CONTACTS_ID = "_id";
	public static final String LOCAL_STORAGE = "LocalStorage";

	private static final String ID = "_id";
	private static final String NICKNAME = "nickname";
	private static final String CONTACT_ID = "contactId";
	private static final String DATE_TIME = "dateTime";
	private static final String SUBJECT = "subject";
	private static final String MESSAGE_BODY = "messageBody";
	private static final String HASH_MATCHES = "hashVerifed";
	private static final String SIGNATURE_MATCHES = "signatureVerifed";
	private static final String ATTACHMENT = "attachmentBlob";
	private static final String READ = "read"; // 1 if read, 0 for unread

	// NICKNAME
	private static final String TOKEN = "token";
	private static final String CONTACT_NOTE = "note";
	private static final String VERIFIED_BY = "verifiers";
	private static final String PRIVATE_KEY = "privateKey";
	private static final String PUBLIC_KEY = "publicKey";
	private static final String KEYPAIR_NAME = "keyName";

	// Messages: _id, thread_id, nickname, CONTACT_ID, timestamp, hash_matches,
	// read, signature_matches, subject, body, attachment
	private static final String CREATE_MESSAGES_COMMAND = "CREATE TABLE "
			+ MESSAGES_TABLE + " (" + MESSAGES_ID
			+ " integer PRIMARY KEY autoincrement, " + THREAD_ID + " INTEGER, "
			+ NICKNAME + " TEXT, " + CONTACT_ID + " INTEGER, " + DATE_TIME
			+ " INTEGER, " + HASH_MATCHES + " INTEGER DEFAULT 0, " + READ
			+ " INTEGER DEFAULT 0, " + SIGNATURE_MATCHES
			+ " INTEGER DEFAULT 0, " + SUBJECT + " TEXT, " + MESSAGE_BODY
			+ " BLOB, " + ATTACHMENT + " BLOB);";

	// Contacts: _id, name, CONTACT_ID, timestamp (added), verified_by (_ids),
	// note
	private static final String CREATE_CONTACTS_COMMAND = "CREATE TABLE "
			+ CONTACTS_TABLE + " (" + CONTACTS_ID
			+ " integer PRIMARY KEY autoincrement, " + NICKNAME + " TEXT, "
			+ PUBLIC_KEY + " BLOB, " + DATE_TIME + " INTEGER, " + VERIFIED_BY
			+ " TEXT, " + TOKEN + " BLOB, " + CONTACT_NOTE + " TEXT);";


	private static final String DATABASE_NAME = "touchToText.db";
	private static final int DATABASE_VERSION = 1;

	// Databases and Context
	private File dbFile = null;
	private SQLiteDatabase db;
	private MasterPassword passwordInstance = null;
	private Context context;
	private SealablePublicKey publicKey;
	// The singleton instance

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
		Log.i("db","Intializing database");
		if (passwordInstance == null) {
			setPassword(password);
			SQLiteDatabase.loadLibs(context);
			db = this.getWritableDatabase(password);
		}
	}

	// Insert a message you just wrote into the database.
	void addSentMessage(int threadID, int contactID, String nickname,
			String body) {
		ContentValues cv = new ContentValues();
		cv.put(THREAD_ID, threadID);
		cv.put(NICKNAME, nickname);
		cv.put(CONTACT_ID, contactID);
		cv.put(MESSAGE_BODY, body);
		cv.put(DATE_TIME, System.currentTimeMillis());
		db.insert(MESSAGES_TABLE, null, cv);
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
			db.close();
			db = null;
		}
		return dbFile.delete();
	}

	private void createTables(SQLiteDatabase db) {
		db.execSQL(CREATE_MESSAGES_COMMAND);
		db.execSQL(CREATE_CONTACTS_COMMAND);
	}

	private boolean tableExists(String table_name) {

		Cursor cursor = getReadableDatabase("password").rawQuery(
				"select DISTINCT tbl_name from sqlite_master where tbl_name = '"
						+ table_name + "'", null);
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
				TOUCH_TO_TEXT_PREFERENCES_XML, passwordInstance.getPasswordString()
				, true);

		String publicKeyString = encryptedPublicKey.getString(PUBLIC_KEY);
		KeyPairsProvider kp = (KeyPairsProvider) Helpers.deserialize(Base64.decode(
				publicKeyString, Base64.DEFAULT));
		publicKey = kp.getExternalKey();
		return publicKey;
	}
	
	public KeyPair getSigningKey() {
		SecurePreferences encryptedPublicKey = new SecurePreferences(context,
				TOUCH_TO_TEXT_PREFERENCES_XML, passwordInstance.getPasswordString()
				, true);

		String publicKeyString = encryptedPublicKey.getString(PUBLIC_KEY);
		KeyPairsProvider kp = (KeyPairsProvider) Helpers.deserialize(Base64.decode(
				publicKeyString, Base64.DEFAULT));
		return kp.getSigningKey();
	}

	public void addContact(CryptoContacts.Contact newContact) {
		AddContactsToDBTask task = new AddContactsToDBTask();
		task.execute(new CryptoContacts.Contact[] { newContact });
	}

	// Messages: _id, thread_id, nickname, CONTACT_ID, timestamp, hash_matches,
	// read, signature_matches, subject, body, attachment
	public void addOutgoingMessage(final SignedMessage signedMessage, long timeSent,
			CryptoContacts.Contact contact) {
		AddMessageToDBTask task = new AddMessageToDBTask();
		ContentValues newMessage = new ContentValues();
		newMessage.put(MESSAGE_BODY, Helpers.serialize(signedMessage));
		newMessage.put(DATE_TIME, timeSent);
		newMessage.put(CONTACT_ID, contact.getID());
		newMessage.put(NICKNAME, contact.toString());
		// Need to get CONTACT_ID from contact and add that, nickname is not
		// guaranteed unique.
		task.execute(new ContentValues[] { newMessage });
		// Update
		UpdateLastContactedTask last = new UpdateLastContactedTask();
		ContentValues updateTime = new ContentValues();
		updateTime.put(CONTACTS_ID, contact.getID());
		updateTime.put(DATE_TIME, timeSent);
		last.execute(new ContentValues[] { updateTime });
	}



	// Messages: _id, thread_id, nickname, CONTACT_ID, timestamp, hash_matches,
	// read, signature_matches, subject, body, attachment
	private class UpdateLastContactedTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... toAdd) {
			for (ContentValues val : toAdd) {
				getReadableDatabase(passwordInstance.getPasswordString()).update(CONTACTS_TABLE, val, CONTACTS_ID + "=" + val.getAsLong(CONTACTS_ID), null);
			}
			return null;
		}
	}

	private class AddMessageToDBTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... toAdd) {
			for (ContentValues val : toAdd) {
				getReadableDatabase(passwordInstance.getPasswordString()).insert(MESSAGES_TABLE, null, val);
			}
			return null;
		}
	}

	public void getAllContacts() {
		GetContactsFromDBTask task = new GetContactsFromDBTask();
		task.execute(new String[] { null });
	}

	private class AddContactsToDBTask extends
	AsyncTask<CryptoContacts.Contact, Void, Void> {
		@Override
		protected Void doInBackground(CryptoContacts.Contact... toAdd) {
			for (CryptoContacts.Contact newContact : toAdd) {
				ContentValues newUser = new ContentValues();
				newUser.put(NICKNAME, newContact.toString());
				newUser.put(PUBLIC_KEY, Helpers.serialize(newContact.getSealablePublicKey()));
				newUser.put(DATE_TIME, System.currentTimeMillis());
				getReadableDatabase(passwordInstance.getPasswordString()).insert(CONTACTS_TABLE, null, newUser);
			}
			return null;
		}
	}

	private class GetMessagesFromDBTask extends AsyncTask<Long, Void, Cursor> {
		@Override
		protected Cursor doInBackground(Long... ids) {
			Cursor cursor = null;
			for (Long id : ids) {
				String sortOrder = DATE_TIME + " ASC";
				String condition = CONTACT_ID + "=" + ids;
				cursor = getReadableDatabase(passwordInstance.getPasswordString()).query(
						MESSAGES_TABLE, 
						new String[] {DATE_TIME, MESSAGE_BODY, CONTACT_ID}
						, condition, null, null, null, sortOrder);
			}
			
			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			List<TimestampedMessage> messages = new ArrayList<TimestampedMessage>();
			long id = -1;
			result.moveToFirst();
			// {DATE_TIME, MESSAGE_BODY, CONTACT_ID}
			while (!result.isAfterLast()) {
				if (id == -1) {
					id = result.getLong(2);
				}
				long dateTime = result.getLong(0);
				byte[] messageBody = result.getBlob(1);
				SignedMessage message = (SignedMessage)Helpers.deserialize(messageBody);
				messages.add(new TimestampedMessage(message, dateTime));
				result.moveToNext();
			}
			CryptoContacts.addMessages(id, messages);
			result.close();
		}
	}

	private class GetContactsFromDBTask extends AsyncTask<String, Void, Cursor> {
		@Override
		protected Cursor doInBackground(String... names) {
			// TODO: Need to update DATE_TIME when message is sent or received for this contact.
			String sortOrder = DATE_TIME + " DESC";
			Cursor cursor = getReadableDatabase(passwordInstance.getPasswordString()).query(
					CONTACTS_TABLE, 
					new String[] {	CONTACTS_ID, PUBLIC_KEY, NICKNAME, DATE_TIME}
					, null, null, null, null, sortOrder);
			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			CryptoContacts.clearContacts();
			result.moveToFirst();
			// {ID, SEALABLE_PUBLIC_KEY, NICKNAME };
			while (!result.isAfterLast()) {
				long id = result.getLong(0);
				SealablePublicKey key = (SealablePublicKey) Helpers.deserialize(result.getBlob(1));
				String nickname = result.getString(2);
				CryptoContacts.Contact newContact = new CryptoContacts.Contact(
						nickname, key, id);
				CryptoContacts.addContact(newContact);
				result.moveToNext();
			}
			result.close();
		}
	}

	private class GenerateKeysTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... names) {
			SecurePreferences encryptedPublicKey = new SecurePreferences(context,
					TOUCH_TO_TEXT_PREFERENCES_XML, passwordInstance.getPasswordString()
					, true);
			KeyPairsProvider kp = new KeyPairsProvider();
			byte[] b = Helpers.serialize(kp);
			String publicKeyString = Base64.encodeToString(b, Base64.DEFAULT);
			encryptedPublicKey.put(PUBLIC_KEY, publicKeyString);
			return null;
		}
	}
}