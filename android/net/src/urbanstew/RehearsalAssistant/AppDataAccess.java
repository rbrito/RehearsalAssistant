package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

public class AppDataAccess
{
	AppDataAccess(Context context)
	{
		mContext = context;
	}
	
	public long getCurrentProjectId()
	{
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    	if(preferences.contains("current_project_id"))
    		return(preferences.getLong("current_project_id", -1));
    	
    	long id = getFirstProjectID();
    	preferences.edit().putLong("current_project_id", id).commit();
        return id;
	}
	
	void setCurrentProjectId(long id)
	{
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    	preferences.edit().putLong("current_project_id", id).commit();
	}

	long getProjectIdNot(long id)
	{		
        Cursor projectsCursor = mContext.getContentResolver().query(Projects.CONTENT_URI, projectsDataProjection, Projects._ID + "<>" + id, null, Projects.DEFAULT_SORT_ORDER);
        if(projectsCursor.getCount()>0)
        {
        	projectsCursor.moveToFirst();
        	setCurrentProjectId(id = projectsCursor.getLong(0));
        }
        projectsCursor.close();
        return id;
	}
	
	long getFirstProjectID() {
        Cursor c = mContext.getContentResolver().query(Projects.CONTENT_URI, projectsDataProjection, null, null, Projects.DEFAULT_SORT_ORDER);
		c.moveToFirst();
		long result = c.getLong(0);
		c.close();
		return result;
	}
	
	float getVisitedVersion()
	{
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    	return preferences.getFloat("app_visited_version", 0);
	}
	
	void setVisitedVersion(float version)
	{
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    	preferences.edit().putFloat("app_visited_version", version).commit();
	}
    
    static String[] projectsDataProjection =
    {
    	Projects._ID,
    };

    Context mContext;
}
