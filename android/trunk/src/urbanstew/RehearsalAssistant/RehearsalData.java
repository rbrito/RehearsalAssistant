package urbanstew.RehearsalAssistant;

import java.io.File;
import java.util.HashMap;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class RehearsalData extends ContentProvider {

	@Override
	public boolean onCreate()
	{
		// Access the database.
		mOpenHelper = new DatabaseHelper(getContext());
		
		// Insert the hardcoded project if it is missing.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor c = db.query("projects", null, null, null, null, null, null);
		Log.w("RehearsalAssistant", "Read " + c.getCount() + " projects");
		if (c.getCount() == 0) {
			ContentValues values = new ContentValues();
			values.put("title", "Only Project");
			values.put("identifier", "only_project");
			db.insert("projects", "identifier", values);
		}
		c.close();
		
		return true;
	}

	// returns the currently active project id
	public long getProjectID() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor c = db.query("projects", null, null, null, null, null, null);
		c.moveToFirst();
		return c.getLong(0);
	}

	enum Project { _ID, TITLE, IDENTIFIER }

	enum Session { _ID, PROJECT_ID, TITLE, IDENTIFIER, START_TIME, END_TIME }
	
	enum Annotation { _ID, RUN_ID, START_TIME, END_TIME, FILE_NAME }

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context)
		{
			super(context, "rehearsal_assistant.db", null, 4);
		}

		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE projects ("
					+ "_id INTEGER PRIMARY KEY,"
					+ "title TEXT,"
					+ "identifier TEXT"
					+ ");");

			db.execSQL("CREATE TABLE " + Sessions.TABLE_NAME + "("
					+ Sessions._ID + " INTEGER PRIMARY KEY,"
					+ Sessions.PROJECT_ID + " INTEGER,"
					+ Sessions.TITLE + " TEXT,"
					+ Sessions.IDENTIFIER + " TEXT,"
					+ Sessions.START_TIME + " INTEGER,"
					+ Sessions.END_TIME + " INTEGER"
					+ ");");

			db.execSQL("CREATE TABLE annotations ("
					+ Annotations._ID + " INTEGER PRIMARY KEY,"
					+ Annotations.SESSION_ID + " INTEGER,"
					+ Annotations.START_TIME + " INTEGER,"
					+ Annotations.END_TIME + " INTEGER,"
					+ Annotations.FILE_NAME + " TEXT"
					+ ");");
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w("RehearsalAssistant", "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old Rehearsal Assistant data");

            db.execSQL("DROP TABLE IF EXISTS projects");
            db.execSQL("DROP TABLE IF EXISTS runs");
            db.execSQL("DROP TABLE IF EXISTS annotations");
            onCreate(db);
        }
	}

    private static final int SESSIONS = 1;
    private static final int SESSION_ID = 2;
    private static final int ANNOTATIONS = 3;
    private static final int ANNOTATION_ID = 4;

    private static final UriMatcher sUriMatcher;
    private static HashMap<String, String> sSessionsProjectionMap;
    private static HashMap<String, String> sAnnotationsProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "sessions", SESSIONS);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "sessions/#", SESSION_ID);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "annotations", ANNOTATIONS);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "annotations/#", ANNOTATION_ID);

        sSessionsProjectionMap = new HashMap<String, String>();
        sSessionsProjectionMap.put(Sessions._ID, Sessions._ID);
        sSessionsProjectionMap.put(Sessions.PROJECT_ID, Sessions.PROJECT_ID);
        sSessionsProjectionMap.put(Sessions.TITLE, Sessions.TITLE);
        sSessionsProjectionMap.put(Sessions.IDENTIFIER, Sessions.IDENTIFIER);
        sSessionsProjectionMap.put(Sessions.START_TIME, Sessions.START_TIME);
        sSessionsProjectionMap.put(Sessions.END_TIME, Sessions.END_TIME);

        sAnnotationsProjectionMap = new HashMap<String, String>();
        sAnnotationsProjectionMap.put(Annotations._ID, Annotations._ID);
        sAnnotationsProjectionMap.put(Annotations.SESSION_ID, Annotations.SESSION_ID);
        sAnnotationsProjectionMap.put(Annotations.START_TIME, Annotations.START_TIME);
        sAnnotationsProjectionMap.put(Annotations.END_TIME, Annotations.END_TIME);
        sAnnotationsProjectionMap.put(Annotations.FILE_NAME, Annotations.FILE_NAME);
    }

	private DatabaseHelper mOpenHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case SESSIONS:
            count = db.delete(Sessions.TABLE_NAME, selection, selectionArgs);
            break;

        case SESSION_ID:
            String sessionId = uri.getPathSegments().get(1);
            count = db.delete(Sessions.TABLE_NAME, Sessions._ID + "=" + sessionId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            if(count>0)
            	deleteAnnotations(db, Annotations.SESSION_ID + "=" + sessionId, null);
            break;

        case ANNOTATIONS:
            count = deleteAnnotations(db, selection, selectionArgs);
            break;

        case ANNOTATION_ID:
            String annotationId = uri.getPathSegments().get(1);
            count = deleteAnnotations(db, Annotations._ID + "=" + annotationId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
	int deleteAnnotations(SQLiteDatabase db, String selection, String[] selectionArgs)
	{
		// query and erase files
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
        qb.setTables(Annotations.TABLE_NAME);
        qb.setProjectionMap(sAnnotationsProjectionMap);

        String[] projection =
        {
        	Annotations.FILE_NAME        	
        };
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, null);

        for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
        {
        	if(c.getString(0)!=null)
        	{
        		Log.w("Rehearsal Assistant erasing", c.getString(0));
        		(new File(c.getString(0))).delete();
        	}
        }
		// delete
		return db.delete(Annotations.TABLE_NAME, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri)
	{
        switch (sUriMatcher.match(uri)) {
        case SESSIONS:
            return Sessions.CONTENT_TYPE;

        case SESSION_ID:
            return Sessions.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
        ContentValues values;
        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            values = new ContentValues();
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
		switch(sUriMatcher.match(uri))
		{
		case SESSIONS:
		{
	    	values.put(Sessions.PROJECT_ID, getProjectID());
	    	if(!values.containsKey(Sessions.IDENTIFIER))
	    		values.put(Sessions.IDENTIFIER, values.getAsString(Sessions.TITLE).toLowerCase().replace(" ", "_"));
	 
	        long rowId = db.insert(Sessions.TABLE_NAME, Sessions.TITLE, values);
	        if (rowId > 0) {
	            Uri noteUri = ContentUris.withAppendedId(Rehearsal.Sessions.CONTENT_URI, rowId);
	            getContext().getContentResolver().notifyChange(noteUri, null);
	            return noteUri;
	        }
	        break;
		}
		case ANNOTATIONS:
		{
			long rowId = db.insert(Annotations.TABLE_NAME, Annotations.FILE_NAME, values);
	        if (rowId > 0) {
	            Uri noteUri = ContentUris.withAppendedId(Rehearsal.Annotations.CONTENT_URI, rowId);
	            getContext().getContentResolver().notifyChange(noteUri, null);
	            return noteUri;
	        }
	        break;
		}
		default:
	        throw new IllegalArgumentException("Unknown URI " + uri);
		}
        throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case SESSIONS:
            qb.setTables(Sessions.TABLE_NAME);
            qb.setProjectionMap(sSessionsProjectionMap);
            break;

        case SESSION_ID:
            qb.setTables(Sessions.TABLE_NAME);
            qb.setProjectionMap(sSessionsProjectionMap);
            qb.appendWhere(Sessions._ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case ANNOTATIONS:
            qb.setTables(Annotations.TABLE_NAME);
            qb.setProjectionMap(sAnnotationsProjectionMap);
            break;

        case ANNOTATION_ID:
            qb.setTables(Annotations.TABLE_NAME);
            qb.setProjectionMap(sAnnotationsProjectionMap);
            qb.appendWhere(Annotations._ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Rehearsal.Sessions.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case SESSION_ID:
            count = db.update(Sessions.TABLE_NAME, values, selection, selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
}
