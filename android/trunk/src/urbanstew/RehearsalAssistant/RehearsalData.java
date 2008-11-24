package urbanstew.RehearsalAssistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RehearsalData {

	public RehearsalData(Context context)
	{
        // Read the database.  For now, insert the hardcoded project
        mOpenHelper = new DatabaseHelper(context);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor c = db.query("projects", null, null, null, null, null, null);
        Log.w("RehearsalAssistant", "Read " + c.getCount() + " projects");
        if (c.getCount() == 0)
        {
        	ContentValues values = new ContentValues();
        	values.put("title", "Only Project");
        	values.put("identifier", "only_project");
        	db.insert("projects", "identifier", values);
        }
	}
	
	public long getProjectID()
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor c = db.query("projects", null, null, null, null, null, null);
        c.moveToFirst();
        return c.getLong(0);
	}
	
    private static class DatabaseHelper extends SQLiteOpenHelper
    {

        DatabaseHelper(Context context)
        {
            super(context, "rehearsal_assistant.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE projects ("
                    + "id INTEGER PRIMARY KEY,"
                    + "title TEXT,"
                    + "identifier TEXT"
                    + ");");

            db.execSQL("CREATE TABLE annotations ("
                    + "id INTEGER PRIMARY KEY,"
                    + "project_id INTEGER,"
                    + "start_time INTEGER,"
                    + "file_name TEXT"
                    + ");");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w("RehearsalAssistant", "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS projects");
            db.execSQL("DROP TABLE IF EXISTS annotations");
            onCreate(db);
        }
    }

	public void clearAnnotations(long project_id)
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM annotations WHERE project_id = " + project_id + ";");
	}

	public void insertAnnotation(long project_id, long start_time, String file_name)
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    	ContentValues values = new ContentValues();
    	values.put("project_id", project_id);
    	values.put("start_time", start_time);
    	values.put("file_name", file_name);
    	db.insert("annotations", "project_id", values);
	}

	Cursor getAnnotations(long project_id)
	{
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query("annotations", null, null, null, null, null, null);

		Log.w("RehearsalAssistant", "Read " + c.getCount() + " annotation entries.");
		return c;
		
	}

	private DatabaseHelper mOpenHelper;
}
