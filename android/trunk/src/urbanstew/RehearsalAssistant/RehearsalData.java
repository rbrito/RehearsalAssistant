package urbanstew.RehearsalAssistant;

import java.util.HashMap;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Runs;

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

	enum Run { _ID, PROJECT_ID, TITLE, IDENTIFIER, RECORDING_TIME }
	
	enum Annotation { _ID, RUN_ID, START_TIME, FILE_NAME }

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context)
		{
			super(context, "rehearsal_assistant.db", null, 3);
		}

		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE projects ("
					+ "_id INTEGER PRIMARY KEY,"
					+ "title TEXT,"
					+ "identifier TEXT"
					+ ");");

			db.execSQL("CREATE TABLE runs ("
					+ "_id INTEGER PRIMARY KEY,"
					+ "project_id INTEGER,"
					+ "title TEXT,"
					+ "identifier TEXT,"
					+ "recording_time INTEGER"
					+ ");");

			db.execSQL("CREATE TABLE annotations ("
					+ "_id INTEGER PRIMARY KEY,"
					+ "run_id INTEGER,"
					+ "start_time INTEGER,"
					+ "file_name TEXT"
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

    private static final int RUNS = 1;
    private static final int RUN_ID = 2;
    private static final int ANNOTATIONS = 3;
    private static final int ANNOTATION_ID = 4;

    private static final UriMatcher sUriMatcher;
    private static HashMap<String, String> sRunsProjectionMap;
    private static HashMap<String, String> sAnnotationsProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "runs", RUNS);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "runs/#", RUN_ID);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "annotations", ANNOTATIONS);
        sUriMatcher.addURI(Rehearsal.AUTHORITY, "annotations/#", ANNOTATION_ID);

        sRunsProjectionMap = new HashMap<String, String>();
        sRunsProjectionMap.put("_id", "_id");
        sRunsProjectionMap.put("project_id", "project_id");
        sRunsProjectionMap.put("title", "title");
        sRunsProjectionMap.put("identifier", "identifier");
        sRunsProjectionMap.put("recording_time", "recording_time");

        sAnnotationsProjectionMap = new HashMap<String, String>();
        sAnnotationsProjectionMap.put("_id", "_id");
        sAnnotationsProjectionMap.put("run_id", "run_id");
        sAnnotationsProjectionMap.put("start_time", "start_time");
        sAnnotationsProjectionMap.put("file_name", "file_name");
    }

	private DatabaseHelper mOpenHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case RUNS:
            count = db.delete("runs", selection, selectionArgs);
            break;

        case RUN_ID:
            String runId = uri.getPathSegments().get(1);
            count = db.delete("runs", "_id" + "=" + runId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        case ANNOTATIONS:
            count = db.delete("annotations", selection, selectionArgs);
            break;

        case ANNOTATION_ID:
            String annotationId = uri.getPathSegments().get(1);
            count = db.delete("annotations", "_id" + "=" + annotationId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri)
	{
        switch (sUriMatcher.match(uri)) {
        case RUNS:
            return Runs.CONTENT_TYPE;

        case RUN_ID:
            return Runs.CONTENT_ITEM_TYPE;

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
		case RUNS:
		{
	    	values.put("project_id", getProjectID());
	    	if(!values.containsKey("identifier"))
	    		values.put("identifier", values.getAsString("title").toLowerCase().replace(" ", "_"));
	 
	        long rowId = db.insert("runs", "title", values);
	        if (rowId > 0) {
	            Uri noteUri = ContentUris.withAppendedId(Rehearsal.Runs.CONTENT_URI, rowId);
	            getContext().getContentResolver().notifyChange(noteUri, null);
	            return noteUri;
	        }
	        break;
		}
		case ANNOTATIONS:
		{
			long rowId = db.insert("annotations", "file_name", values);
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
        case RUNS:
            qb.setTables("runs");
            qb.setProjectionMap(sRunsProjectionMap);
            break;

        case RUN_ID:
            qb.setTables("runs");
            qb.setProjectionMap(sRunsProjectionMap);
            qb.appendWhere(Runs._ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case ANNOTATIONS:
            qb.setTables("annotations");
            qb.setProjectionMap(sAnnotationsProjectionMap);
            break;

        case ANNOTATION_ID:
            qb.setTables("annotations");
            qb.setProjectionMap(sAnnotationsProjectionMap);
            qb.appendWhere(Annotations._ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Rehearsal.Runs.DEFAULT_SORT_ORDER;
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

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
