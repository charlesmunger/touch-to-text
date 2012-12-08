package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Base64;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

import edu.ucsb.cs290.touch.to.chat.R;
import edu.ucsb.cs290.touch.to.chat.https.TorProxy;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.Message;
import edu.ucsb.cs290.touch.to.chat.remote.messages.ProtectedMessage;
import edu.ucsb.cs290.touch.to.chat.remote.messages.SignedMessage;
import edu.ucsb.cs290.touch.to.chat.remote.register.RegisterUser;

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
	public  static final String SENDER_ID = "sender";
	public static final String RECIPIENT_ID = "recipient";
	public static final String DATE_TIME = "dateTime";
	public static final String READ = "read"; // 1 if read, 0 for unread
	public static final String MESSAGE_BODY = "messageBody";

	public static final String[] MESSAGES_CURSOR_COLUMNS = new String[] { MESSAGES_ID, DATE_TIME, MESSAGE_BODY, SENDER_ID, RECIPIENT_ID };

	// Contacts Table
	public static final String CONTACTS_ID = "_id";
	public static final String NICKNAME = "nickname";
	public static final String CONTACT_ID = "contactId";
	public static final String CONTACT_NOTE = "note";
	private static final String VERIFIED_BY = "verifiers";
	public static final String PUBLIC_KEY = "publicKey";
	public static final String PUBLIC_KEY_FINGERPRINT = "publicKeyFingerprint";


	public static final String[] CONTACT_CURSOR_COLUMNS = new String[] {CONTACTS_ID, PUBLIC_KEY, NICKNAME};

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
					+ PUBLIC_KEY_FINGERPRINT + " TEXT, "
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
		Cursor cursor = getReadableDatabase(passwordInstance.getPasswordString())
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

	public void addContact(Contact newContact) {
		AddContactsToDBTask task = new AddContactsToDBTask();
		task.execute(new Contact[] { newContact });
	}


	public void addOutgoingMessage(final SignedMessage signedMessage,
			Contact contact) {
		long time=0;
		try {
			time = signedMessage.getMessage(getSigningKey().getPublic()).getTimeSent();
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
		updateLastContacted(contact.getID(), time);
		UpdateLastContactedTask last = new UpdateLastContactedTask();
		last.execute(new Long[] { contact.getID(), time });
	}

	// { MESSAGES_ID, DATE_TIME, MESSAGE_BODY, SENDER_ID, RECIPIENT_ID };

	
	public void addIncomingMessage(ProtectedMessage message) {
		SecurePreferences getEncryptionKey = new SecurePreferences(
				context, TOUCH_TO_TEXT_PREFERENCES_XML,
				passwordInstance.getPasswordString(), true);
		String encodedKeyPairProvider = getEncryptionKey.getString(PUBLIC_KEY);
		KeyPairsProvider provider = (KeyPairsProvider)Helpers.deserialize(Base64.decode(encodedKeyPairProvider, Base64.DEFAULT));
		try {
			SignedMessage recieved = message.getMessage(provider.getEncryptionKey().getPrivate());
			PublicKey author = recieved.getAuthor();
			Message recievedTextMessage = recieved.getMessage(author);
			long time = recievedTextMessage.getTimeSent();
			AddIncomingMessageToDBTask task = new AddIncomingMessageToDBTask();
			ContentValues newMessage = new ContentValues();
			newMessage.put(MESSAGE_BODY, Helpers.serialize(recieved));
			newMessage.put(DATE_TIME, time );
			newMessage.put(RECIPIENT_ID, MY_CONTACT_ID);
			newMessage.put(READ, 0);
			// Used to retrieve contact in AddIncomingMessageToDBTask
			newMessage.put(PUBLIC_KEY_FINGERPRINT, Helpers.getKeyFingerprint(author));
			task.execute(new ContentValues[] { newMessage });
			// For sorting purposes, update last contacted.
		} catch (GeneralSecurityException e) {
			Log.wtf("Touch-to-text", "You recieved a message that may have been tamperd with!", e);
		} catch (IOException e) {
			Log.d("Touch-to-text", "Error deserializing signed message", e);
		} catch (ClassNotFoundException e) {
			Log.d("Touch-to-text", "Error, class not found in addIncomingMessage", e);
		} catch( Exception e ) {
			Log.wtf("touch-to-text", "Failed to add message! Problem verifying author.", e);
		}
	}

	private void updateLastContacted(long contactID, long dateTime) {
		ContentValues updateDateContacted = new ContentValues();
		updateDateContacted.put(CONTACTS_ID, contactID);
		updateDateContacted.put(DATE_TIME, dateTime);
		getReadableDatabase(passwordInstance.getPasswordString())
		.update(CONTACTS_TABLE, updateDateContacted,
				CONTACTS_ID + "=" + contactID,
				null);
	}
	
	/**
	 * Update last contacted for a given contactID.
	 * @author dannyiland
	 * @param contactID
	 * @param dateTime
	 */
	private class UpdateLastContactedTask extends
	AsyncTask<Long, Void, Void> {
		@Override
		protected Void doInBackground(Long... toAdd) {
			updateLastContacted(toAdd[0], toAdd[1]);
			return null;
		}
	}

	private class UpdateLastContactedWithCVTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... vals) {
			for (ContentValues val: vals) {
				getReadableDatabase(passwordInstance.getPasswordString())
				.update(CONTACTS_TABLE, val,
						CONTACTS_ID + "=" + val.getAsLong(CONTACTS_ID),
						null);
			}
			return null;
		}
	}
	public Cursor getContactsCursor() {
		String sortOrder = DATE_TIME + " DESC";
		Cursor cursor = getReadableDatabase(
				passwordInstance.getPasswordString()).query(CONTACTS_TABLE,
						CONTACT_CURSOR_COLUMNS,
						null, null, null, null, sortOrder);
		return cursor;
	}
	
	public long getContactFromPublicKeySignature(String keySignature) {
		String sortOrder = DATE_TIME + " DESC";
		String query = PUBLIC_KEY_FINGERPRINT + " = ?";
		Cursor cursor = getReadableDatabase(
				passwordInstance.getPasswordString()).query(CONTACTS_TABLE,
						CONTACT_CURSOR_COLUMNS,
						query, new String[] { keySignature }, null, null, sortOrder);
		if( cursor.getCount() < 1) {
			Log.wtf("touch-to-text", "Recieved message from unknown contact");
			return -1;
		} else {
			cursor.moveToFirst();
			return cursor.getLong(cursor.getColumnIndex(CONTACTS_ID));
		}
	}
	
	private class AddIncomingMessageToDBTask extends
	AsyncTask<ContentValues, Void, Void> {
		@Override
		protected Void doInBackground(ContentValues... toAdd) {
			// Need to get get sender ID from PUBLIC_KEY_FINGERPRINT
			for (ContentValues val : toAdd) {
				long contactID = getContactFromPublicKeySignature(val.getAsString(PUBLIC_KEY_FINGERPRINT));
				val.remove(PUBLIC_KEY_FINGERPRINT);
				long dateTime = toAdd[0].getAsLong(DATE_TIME);
				// Add message to messages Table
				val.put(CONTACTS_ID, contactID);
				getReadableDatabase(passwordInstance.getPasswordString())
				.insert(MESSAGES_TABLE, null, val);
				updateLastContacted(contactID, dateTime);
			}
			return null;
		}
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
	AsyncTask<Contact, Void, Void> {
		@Override
		protected Void doInBackground(Contact... toAdd) {
			for (Contact newContact : toAdd) {
				ContentValues newUser = new ContentValues();
				newUser.put(NICKNAME, newContact.toString());
				newUser.put(PUBLIC_KEY,
						Helpers.serialize(newContact.getSealablePublicKey()));
				newUser.put(DATE_TIME, System.currentTimeMillis());
				newUser.put(PUBLIC_KEY_FINGERPRINT, newContact.getSealablePublicKey().signingKeyFingerprint());
				getReadableDatabase(passwordInstance.getPasswordString())
				.insert(CONTACTS_TABLE, null, newUser);
			}
			return null;
		}
	}

	public Cursor getMessagesCursor(long id) {
		Cursor cursor = null;
		String sortOrder = DATE_TIME + " ASC";
		String condition = RECIPIENT_ID + "="+id+" OR " + SENDER_ID + "="+id;
		cursor = getReadableDatabase(passwordInstance.getPasswordString())
				.query(MESSAGES_TABLE,
						MESSAGES_CURSOR_COLUMNS,
						condition, null, null, null, sortOrder);

		return cursor;
	}

	public Cursor getContactCursor() {
		String sortOrder = DATE_TIME + " DESC";
		Cursor cursor = getReadableDatabase(passwordInstance.getPasswordString()).query(
				CONTACTS_TABLE, 
				new String[] {CONTACTS_ID, PUBLIC_KEY, DATE_TIME, NICKNAME}
				,null,null, null, null, sortOrder);
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
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			WakeLock mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Touch To Text Key Generation");
			mWakeLock.acquire();
			KeyPairsProvider kp = null;
			try { 
				kp = new KeyPairsProvider();
			} finally {
				mWakeLock.release();
			}
			byte[] b = Helpers.serialize(kp);
			String publicKeyString = Base64.encodeToString(b, Base64.DEFAULT);
			encryptedPublicKey.put(PUBLIC_KEY, publicKeyString);
			GCMRegistrar.register(context, context.getResources().getString(R.string.GCM_Sender_ID));
			try {
				TorProxy.postThroughTor(context, new RegisterUser(GCMRegistrar.getRegistrationId(context), kp.getTokenKey(), 50));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void evil) {
			// Set "done generating keys" notification
		}
	}

}
