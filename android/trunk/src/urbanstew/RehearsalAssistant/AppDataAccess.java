package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.AppData;
import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public class AppDataAccess
{
	AppDataAccess(ContentResolver content)
	{
		mContent = content;
	}
	
	public long getCurrentProjectId()
	{
		long result;
        Cursor appDataCursor = mContent.query(AppData.CONTENT_URI, appDataProjection, AppData.KEY + "=" + "'current_project_id'", null, AppData.DEFAULT_SORT_ORDER);
        if(appDataCursor.getCount()>0)
        {
        	appDataCursor.moveToFirst();
        	result = appDataCursor.getLong(2);
        }
        else
        {
        	long id = getFirstProjectID();
        	addCurrentProjectId(id);
        	result = id;
        }
        appDataCursor.close();
        return result;
	}
	
	void setCurrentProjectId(long id)
	{
		ContentValues values = new ContentValues();
    	values.put(AppData.VALUE, id);
    	mContent.update(AppData.CONTENT_URI, values, AppData.KEY + "=" + "'current_project_id'", null);
	}

	void addCurrentProjectId(long id)
	{
		ContentValues values = new ContentValues();
    	values.put(AppData.KEY, "current_project_id");
    	values.put(AppData.VALUE, id);
    	mContent.insert(AppData.CONTENT_URI, values);
	}

	long switchCurrentProject()
	{
		long current = getCurrentProjectId();
		
        Cursor projectsCursor = mContent.query(Projects.CONTENT_URI, projectsDataProjection, Projects._ID + "<>" + current, null, Projects.DEFAULT_SORT_ORDER);
        if(projectsCursor.getCount()>0)
        {
        	projectsCursor.moveToFirst();
        	setCurrentProjectId(current = projectsCursor.getLong(0));
        }
        projectsCursor.close();
        return current;
	}
	// returns the currently active project id
	long getFirstProjectID() {
        Cursor c = mContent.query(Projects.CONTENT_URI, projectsDataProjection, null, null, Projects.DEFAULT_SORT_ORDER);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();
		return result;
	}
	
	String getVisitedVersion()
	{
        Cursor appDataCursor = mContent.query(AppData.CONTENT_URI, appDataProjection, AppData.KEY + "=" + "'app_visited_version'", null, AppData.DEFAULT_SORT_ORDER);
        if(appDataCursor.getCount()==0)
        	return null;
    	appDataCursor.moveToFirst();
    	String value = appDataCursor.getString(2);
        appDataCursor.close();
    	return value;
	}
	
	void addVisitedVersion(String version)
	{
  		ContentValues values = new ContentValues();
       	values.put(AppData.KEY, "app_visited_version");
       	values.put(AppData.VALUE, version);
       	mContent.insert(AppData.CONTENT_URI, values);
	}
    // Display license if this is the first time running this version.
    static String[] appDataProjection =
    {
    	AppData._ID,
        AppData.KEY,
    	AppData.VALUE
    };
    
    static String[] projectsDataProjection =
    {
    	Projects._ID,
    };

    ContentResolver mContent;
}
