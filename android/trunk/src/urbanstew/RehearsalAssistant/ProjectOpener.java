package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;


public class ProjectOpener extends Activity
{    
    /** Called when the activity is first created.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String project_id = getIntent().getData().getPathSegments().get(1);

        // Find out what kind of project this is
        String[] projectsProjection =
        {
        	Projects._ID,
            Projects.TYPE
        };
        
        Cursor projectsCursor = getContentResolver().query(Projects.CONTENT_URI, projectsProjection, Projects._ID + "=" + project_id, null, Projects.DEFAULT_SORT_ORDER);
        projectsCursor.moveToFirst();
        
        // start appropriate activity
        if(projectsCursor.getLong(1) == Projects.TYPE_SESSION)
            startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData(), getApplication(), SessionProject.class));
        else
            startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData(), getApplication(), SimpleProject.class));
        
        projectsCursor.close();
        // finish
        finish();
    }
}