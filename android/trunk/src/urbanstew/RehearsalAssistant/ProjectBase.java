package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ProjectBase extends RehearsalActivity
{	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      	      	
        String projectId = getIntent().getData().getPathSegments().get(1);
        mProjectId = Long.valueOf(projectId);

        setTitle(getResources().getString(R.string.about));
        
        mAppData = new AppDataAccess(getContentResolver());

        // Display license if this is the first time running this version.
        mVisitedVersion = mAppData.getVisitedVersion();
        if (mVisitedVersion == null || !mVisitedVersion.equals("0.5"))
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
	    		    	if(mVisitedVersion == null)
	    		    		mAppData.addVisitedVersion("0.5");
	    		    	else
	    		    		mAppData.setVisitedVersion("0.5");
	    		    }
	    		}
	    	);
        }
    }
    
	protected void setSimpleProject(boolean simpleMode)
	{
		mSimpleMode = simpleMode;
	}

    public boolean onCreateOptionsMenu(Menu menu)
    {
        mHelpMenuItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_menu_help);
        String switchText;
        switchText = mSimpleMode ? "Switch to Session Mode" : "Switch to Simple Mode";
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
				long project_id = new AppDataAccess(getContentResolver()).switchCurrentProject();
		        
				startActivity
		        (
		        	new Intent
		        	(
		        		Intent.ACTION_VIEW,
		        		ContentUris.withAppendedId(Projects.CONTENT_URI, project_id)
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
    
    protected static final String[] sessionsProjection = new String[]
	{
	      Sessions._ID, // 0
	      Sessions.TITLE // 1
	};
    
    String mVisitedVersion;
    AppDataAccess mAppData;
    
    boolean mSimpleMode;
}
