package edu.ucsb.cs290.touch.to.chat.crypto;

import edu.ucsb.cs290.touch.to.chat.KeyManagementService;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;


/**
 * Provider to messages table for GUI and update via a GCM.
 * 
 * For security, explicitly don't export this provider. Additionally, 
 * If a provider's application doesn't specify any permissions,
 * then other applications have no access to the provider's data. 
 * However, components in the provider's application always have full
 * read and write access, regardless of the specified permissions.
 *   
 * @author Danny Iland iland@cs.ucsb.edu
 *
 */
public class MessagesProvider extends ContentProvider {

	private static final String AUTHORITY = "edu.ucsb.cs290.touch.to.chat";
	public static final int MESSAGES = 100;
	public static final int MESSAGES_ID = 110;
	private static final String MESSAGES_BASE_PATH = "messages";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + MESSAGES_BASE_PATH);
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/message";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/messages ";

	private static final UriMatcher sURIMatcher = new UriMatcher(
	        UriMatcher.NO_MATCH);
	static {
	    sURIMatcher.addURI(AUTHORITY, MESSAGES_BASE_PATH, MESSAGES);
	    sURIMatcher.addURI(AUTHORITY, MESSAGES_BASE_PATH + "/#", MESSAGES_ID);
	}

	private final KeyManagementService mService;
	public MessagesProvider(KeyManagementService mService) {
		this.mService = mService;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
	        String[] selectionArgs, String sortOrder) {
	    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
	    queryBuilder.setTables(DatabaseHelper.MESSAGES_TABLE);
	    int uriType = sURIMatcher.match(uri);
	    switch (uriType) {
	    case MESSAGES_ID:
	        queryBuilder.appendWhere(DatabaseHelper.MESSAGES_ID + "="
	                + uri.getLastPathSegment());
	        break;
	    case MESSAGES:
	        // no filter
	        break;
	    default:
	        throw new IllegalArgumentException("Unknown URI");
	    }
	    Cursor cursor = queryBuilder.query(mService.getInstance().getReadableDatabase(MasterPassword.getInstance(null).getPasswordString()),
	            projection, selection, selectionArgs, null, null, sortOrder);
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);
	    return cursor;
	}
	
	/**
	 * Pulled from TextSecure
	 * @param threadId
	 * @return
	 */
	public int getMessageCountForThread(long threadId) {
		SQLiteDatabase db = mService.getInstance().getReadableDatabase(MasterPassword.getInstance(null).getPasswordString());
		Cursor cursor = null;

		try {
			cursor = db.query(DatabaseHelper.MESSAGES_TABLE, new String[] {"COUNT(*)"}, DatabaseHelper.THREAD_ID + " = ?", new String[] {threadId+""}, null, null, null);

			if (cursor != null && cursor.moveToFirst())
				return cursor.getInt(0);
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.item/vnd."+AUTHORITY+".messages_table";
	}

	@Override
	 public Uri insert(Uri uri, ContentValues initialValues) {
	  SQLiteDatabase database = mService.getInstance().getWritableDatabase(MasterPassword.getInstance(null).getPasswordString());
	  long value = database.insert(DatabaseHelper.MESSAGES_TABLE, null, initialValues);
	  return Uri.withAppendedPath(CONTENT_URI, String.valueOf(value));
	 }

	/**
	 * Todo: Implement deletion by id?
	 */
	@Override
	public int delete(Uri uri, String s, String[] strings) {
		return 0;
	}

	/**
	 * Should messages be immutable? Verification on entry into DB.
	 */
	@Override
	public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
		return 0;
	}

}