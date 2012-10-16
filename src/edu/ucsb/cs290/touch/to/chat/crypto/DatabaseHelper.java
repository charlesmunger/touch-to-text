package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.File;
import java.security.MessageDigest;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 *
 * Instantiate and provide access to the DB, which contains
 * Messages, Contacts, and secure private key storage.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	// DB Strings
	public static final String MESSAGES_TABLE = "Messages";
	private static final String ID = "_id";
	public static final String MESSAGES_ID = "messages_id";

	public static final String THREAD_ID = "threadId";
	private static final String NICKNAME = "nickname";
	private static final String CONTACT_ID = "contactId";
	private static final String DATE_TIME = "dateTime";
	private static final String SUBJECT = "subject";
	private static final String MESSAGE_BODY = "messageBody";
	private static final String HASH_MATCHES = "hashVerifed";
	private static final String SIGNATURE_MATCHES = "signatureVerifed";
	private static final String ATTACHMENT = "attachmentBlob";
	private static final String READ = "read"; // 1 if read,  0 for unread

	public static final String CONTACTS_TABLE = "Contacts";
	// NICKNAME
	private static final String TOKEN = "token";
	private static final String CONTACT_NOTE = "note";
	private static final String VERIFIED_BY = "verifiers";
	public static final String CONTACTS_ID = "_id";


	public static final String LOCAL_STORAGE = "LocalStorage";
	private static final String PRIVATE_KEY = "privateKey";
	private static final String PUBLIC_KEY = "publicKey";
	private static final String KEYPAIR_NAME = "keyName";


	// Messages: _id, thread_id, nickname, CONTACT_ID, timestamp, hash_matches, read, signature_matches, subject, body, attachment
	private static final String CREATE_MESSAGES_COMMAND = 
			"CREATE TABLE " + MESSAGES_TABLE + " (" + MESSAGES_ID + " integer PRIMARY KEY autoincrement, " +
					THREAD_ID + " INTEGER, " + NICKNAME + " TEXT, " + CONTACT_ID + " INTEGER, " + DATE_TIME  + " INTEGER, " +
					HASH_MATCHES + " INTEGER DEFAULT 0, " + READ + " INTEGER DEFAULT 0, " +
					SIGNATURE_MATCHES + " INTEGER DEFAULT 0, " + SUBJECT + " TEXT, " + MESSAGE_BODY + " TEXT, " + ATTACHMENT + " BLOB);";

	// Contacts: _id, name, CONTACT_ID, timestamp (added), verified_by (_ids), token, note
	private static final String CREATE_CONTACTS_COMMAND = "CREATE TABLE " + CONTACTS_TABLE + " (" + CONTACTS_ID + " integer PRIMARY KEY autoincrement, " +
			NICKNAME + " TEXT, " + PUBLIC_KEY + " TEXT, " + DATE_TIME  + " INTEGER, " +
			VERIFIED_BY + " TEXT, " + TOKEN + " TEXT, " + CONTACT_NOTE + " TEXT);";

	// LocalStorage: _id, private key, public key, timestamp (added), name
	private static final String CREATE_LOCAL_STORAGE_COMMAND = "CREATE TABLE " + LOCAL_STORAGE + " (" + ID + " integer PRIMARY KEY, " +
			PRIVATE_KEY + " TEXT, " + CONTACT_ID + " TEXT, " + DATE_TIME  + " INTEGER, " + KEYPAIR_NAME + " TEXT);";

	private static final String DATABASE_NAME = "touchToText.db";
	private static final int DATABASE_VERSION = 1;

	// Databases and Context
	private  File dbFile=null;
	private SQLiteDatabase db;
	private Context context;
	// The singleton instance
	private static DatabaseHelper dbHelperInstance = null;

	public static DatabaseHelper getInstance(Context ctx) {
		if (dbHelperInstance == null) {
			// Use global context for the app
			dbHelperInstance = new DatabaseHelper(ctx.getApplicationContext());
		}
		return dbHelperInstance;
	}

	/** Will only work if the db is already unlocked with the current password
	 * Otherwise I think it will fail silently? Should test and see what 'e' is.
	 */
	public boolean changePassword(String newPassword) {
		try {
			MasterPassword.setPassword(newPassword);
			db.rawExecSQL(String.format("PRAGMA key = '%s'", MasterPassword.getPassword()));
			return true;
		} catch( Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Should use a parameterized query to provide this functionality... 
	 * this would allow raw SQL injection if misused
	 */
	private boolean tableExists(String table_name, Context context) {

		Cursor cursor = getDatabase(context).rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+table_name+"'", null);
		if(cursor!=null) {
			if(cursor.getCount()>0) {
				cursor.close();
				return true;
			}
			cursor.close();
		}
		return false;
	}

	/**
	 * Erase the entire database file.
	 * 
	 * @return true if DB was deleted, false otherwise.
	 */
	public boolean wipeDB() {
		if(db != null) {
			db.close();
			db = null;
		}
		return dbFile.delete();
	}

	/**
	 * Returns a reference to the database object, and creates
	 * it if it has not yet been used. Context required for initial
	 * creations, after that it doesn't matter.
	 * @param context The application context, required on first use.
	 * @return
	 */
	public SQLiteDatabase getDatabase(Context context) {
		if (db == null) {
			SQLiteDatabase.loadLibs(context);
			dbFile = context.getDatabasePath(DATABASE_NAME);
			dbFile.mkdirs();
			try {
				db = SQLiteDatabase.openOrCreateDatabase(dbFile, MasterPassword.getPassword(), null);
			} catch(Exception e) {
				e.printStackTrace();
			}
			// Check if this is a new or existing db file. If there are no messages
			if(!tableExists(MESSAGES_TABLE, context) || !tableExists(CONTACTS_TABLE, context)) {
				createTables();
			}
		}
		return db;

	}

	private void createTables() {
		db.execSQL(CREATE_MESSAGES_COMMAND);
		db.execSQL(CREATE_CONTACTS_COMMAND);
		db.execSQL(CREATE_LOCAL_STORAGE_COMMAND);
	}

	DatabaseHelper(Context context) {
		// calls the super constructor, requesting the default cursor factory.
		super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		System.out.println("OnCreate called for db" + db.getPath().toString());
	}


	// Don't do anything on upgrade! But must implement to work with schema changes.
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}