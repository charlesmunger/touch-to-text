package edu.ucsb.cs290.touch.to.chat.crypto;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Parcel;
import android.util.Log;

/**
 * 
 * Instantiate and provide access to the DB, which contains Messages, Contacts,
 * and secure private key storage.
 */
public class IntentDatabaseHelper extends SQLiteOpenHelper {

	// Encrypted Preferences File
	private static final String TOUCH_TO_TEXT_PREFERENCES_XML = "touchToTextPreferences.xml";

	// DB Strings
	public static final String INTENTS_TABLE="Intents";

	// Messages Table
	public static final String INTENT_ID = "_id";
	public static final String DATE_TIME = "dateTime";
	public static final String INTENT_BODY = "intentBody";

	public static final String[] INTENTS_CURSOR_COLUMNS = new String[] { INTENT_ID, DATE_TIME, INTENT_BODY};
	private static final String CREATE_INTENT_COMMAND = 
			"CREATE TABLE " + INTENTS_TABLE + " (  " 
					+ INTENT_ID + " INTEGER PRIMARY KEY autoincrement, "
					+ DATE_TIME + " INTEGER, " 
					+ INTENT_BODY + " BLOB);";


	private static final String DATABASE_NAME = "delayedIntents.db";
	private static final int DATABASE_VERSION = 1;

	// Databases and Context
	private SQLiteDatabase db;
	private Context context;

	public IntentDatabaseHelper(Context ctx) {
		// calls the super constructor, requesting the default cursor factory.
		super(ctx.getApplicationContext(), DATABASE_NAME, null,
				DATABASE_VERSION);
		context = ctx;
	}

	/**
	 * Create the tables and set a password
	 * 
	 * @param password
	 */
	public void initalizeInstance() {
		Log.i("db", "Intializing database");
		db = this.getWritableDatabase();
	}

	/**
	 * Erase the entire database file.
	 * 
	 * @return true if DB was deleted, false otherwise.
	 */
	public boolean wipeDB() {
		if (db != null) {
			if(tableExists(INTENTS_TABLE)) {
				db.execSQL("DROP TABLE " + INTENTS_TABLE);
			}
			createTables(db);
			return true;
		}
		return false;
	}

	private void createTables(SQLiteDatabase db) {
		db.execSQL(CREATE_INTENT_COMMAND);
	}

	private boolean tableExists(String table_name) {

		String condition = "tbl_name = ?";
		Cursor cursor = getReadableDatabase()
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
	}

	// Don't do anything on upgrade! But must implement to work with schema
	// changes.
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
	
	private class AddIntentToDBTask extends
	AsyncTask<Intent, Void, Void> {
		@Override
		protected Void doInBackground(Intent... toAdd) {
			for (Intent newIntent : toAdd) {
				ContentValues intentValues = new ContentValues();
				Parcel parcel = null;
				newIntent.writeToParcel(parcel, 0);
				ContentValues values = new ContentValues();  
				values.put(INTENT_BODY, parcel.createByteArray());  
				intentValues.put(DATE_TIME, System.currentTimeMillis());
				getReadableDatabase()
				.insert(INTENTS_TABLE, null, intentValues);
			}
			return null;
		}
	}
	
	public Cursor getIntentsCursor() {
		String sortOrder = DATE_TIME + " ASC";
		Cursor cursor = getReadableDatabase().query(
				INTENTS_TABLE, 
				new String[] {INTENT_ID, INTENT_BODY, DATE_TIME}
				,null,null, null, null, sortOrder);
		return cursor;
	}

}