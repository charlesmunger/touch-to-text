package edu.ucsb.cs290.touch.to.chat.crypto;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;


public class ContactsProvider extends ContentProvider {


	private static final String AUTHORITY = "edu.ucsb.cs290.touch.to.chat";
	public static final int CONTACTS = 120;
	public static final int CONTACTS_ID = 130;
	private static final String CONTACTS_BASE_PATH = "contacts";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + CONTACTS_BASE_PATH);
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/contact";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/contacts ";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, CONTACTS_BASE_PATH, CONTACTS);
		sURIMatcher.addURI(AUTHORITY, CONTACTS_BASE_PATH + "/#", CONTACTS_ID);
	}
	public ContactsProvider() {

	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(DatabaseHelper.CONTACTS_TABLE);
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case CONTACTS_ID:
			queryBuilder.appendWhere(DatabaseHelper.CONTACTS_ID + "="
					+ uri.getLastPathSegment());
			break;
		case CONTACTS:
			// no filter
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		Cursor cursor = queryBuilder.query(DatabaseHelper.getInstance(getContext()).getReadableDatabase(MasterPassword.getInstance(null).getPasswordString()),
				projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.item/vnd."+AUTHORITY+".contacts_table";
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SQLiteDatabase database = DatabaseHelper.getInstance(getContext()).getWritableDatabase(MasterPassword.getInstance(null).getPasswordString());
		long value = database.insert(DatabaseHelper.CONTACTS_TABLE, null, initialValues);
		return Uri.withAppendedPath(CONTENT_URI, String.valueOf(value));
	}

	/**
	 * Todo: Implement deletion by id
	 */
	@Override
	public int delete(Uri uri, String s, String[] strings) {
		return 0;
	}

	/**
	 * Must implement
	 */
	@Override
	public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
		return 0;
	}

}