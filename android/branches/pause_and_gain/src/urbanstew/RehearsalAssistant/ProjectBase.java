package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ProjectBase extends RehearsalActivity
{	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      	
        mAppData = new AppDataAccess(this);

       	mProjectId = Long.valueOf(getIntent().getData().getPathSegments().get(1));
        
        mAppData.setCurrentProjectId(mProjectId);

        String[] projectsProjection =
        {
        	Projects._ID,
        	Projects.TITLE
        };

        Cursor projectCursor =
        	getContentResolver().query
        	(
        			ContentUris.withAppendedId(Projects.CONTENT_URI, mProjectId),
        			projectsProjection, null, null, Projects.DEFAULT_SORT_ORDER
        	);
        if(projectCursor.getCount() == 0)
        {
        	Toast.makeText(this, R.string.error_project_does_not_exist, Toast.LENGTH_LONG).show();
            projectCursor.close();
        	finish();
        	return;
        }

        projectCursor.moveToFirst();
        setTitle("Rehearsal Assistant - " + projectCursor.getString(1));
        projectCursor.close();
        
        // Display license if this is the first time running this version.
        float visitedVersion = mAppData.getVisitedVersion();
        if (visitedVersion < 0.5f)
        {
    		Request.notification
    		(
				this,
				"Warning",
				getString(R.string.beta_warning)
			);
    		Request.confirmation
    		(
				this,
				"License",
				getString(R.string.license),
				new DialogInterface.OnClickListener()
	    		{
	    		    public void onClick(DialogInterface dialog, int whichButton)
	    		    {
	    		    	mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
	    		    }
	    		}
	    	);
        }
        else if (visitedVersion < RehearsalAssistant.currentVersion)
        {
    		//Request.contribution(this);
//    		Request.recordWidget(this);
    		Request.notification(this, "Uncompressed Recording", "This version introduces experimental support for uncompressed recording (higher quality but much higher file size).  You can switch to uncompressed recording in the Settings menu. It will affect new recordings made in all projects.\n\nPlease let us know your feedback, and THANK YOU for all the feedback so far!");
    		mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
        }
    }
        
    protected void onNewIntent(Intent intent)
    {
    	super.onNewIntent(intent);
    	finish();
    	startActivity(intent);
    }

	protected void setSimpleProject(boolean simpleMode)
	{
		mSimpleMode = simpleMode;
	}

    public boolean onCreateOptionsMenu(Menu menu)
    {
        mHelpMenuItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_menu_help);
        String switchText;
        switchText = "Project Manager";
        mSwitchMenuItem = menu.add(switchText).setIcon(android.R.drawable.ic_menu_more);
        super.onCreateOptionsMenu(menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(!super.onOptionsItemSelected(item))
    	{
			if(item == mSwitchMenuItem)
			{		        
				startActivity
		        (
		        	new Intent
		        	(
		        		Intent.ACTION_VIEW,
		        		Projects.CONTENT_URI
		        	)
		        );
		        
		        finish();

				return true;
			}
			return false;
    	}
    	return true;
    }
        
    protected long projectId()
    {
    	return mProjectId;
    }
    
    long mProjectId;
    
    protected MenuItem mHelpMenuItem, mSwitchMenuItem; 
    
    protected static final int SESSIONS_ID = 0;
    protected static final int SESSIONS_TITLE = 1;
    protected static final int SESSIONS_START_TIME = 2;
    protected static final int SESSIONS_END_TIME = 3;
    
    protected static final String[] sessionsProjection = new String[]
	{
	      Sessions._ID, // 0
	      Sessions.TITLE, // 1
	      Sessions.START_TIME,
	      Sessions.END_TIME
	};
    
    AppDataAccess mAppData;
    
    boolean mSimpleMode;
}
