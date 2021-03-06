package urbanstew.RehearsalAssistant;

import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

public class ProjectBase extends RehearsalActivity {
    static final int SESSIONS_ID = 0;
    protected static final int SESSIONS_TITLE = 1;
    static final int SESSIONS_START_TIME = 2;
    static final int SESSIONS_END_TIME = 3;
    static final String[] sessionsProjection = new String[]
            {
                    Sessions._ID, // 0
                    Sessions.TITLE, // 1
                    Sessions.START_TIME,
                    Sessions.END_TIME
            };
    MenuItem mHelpMenuItem;
    private MenuItem mSwitchMenuItem;
    private long mProjectId;
    private AppDataAccess mAppData;

    public void onCreate(Bundle savedInstanceState) {
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
        if (projectCursor.getCount() == 0) {
            Toast.makeText(this, R.string.error_project_does_not_exist, Toast.LENGTH_LONG).show();
            projectCursor.close();
            finish();
            return;
        }

        projectCursor.moveToFirst();
        setTitle(projectCursor.getString(1));
        projectCursor.close();

        // Display license if this is the first time running this version.
        float visitedVersion = mAppData.getVisitedVersion();
        if (visitedVersion < 0.5f) {
            Request.notification
                    (
                            this,
                            this.getResources().getString(R.string.warning),
                            getString(R.string.beta_warning)
                    );
            Request.confirmation
                    (
                            this,
                            getString(R.string.license2),
                            getString(R.string.license),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
                                }
                            }
                    );
        } else if (visitedVersion < RehearsalAssistant.currentVersion) {
            Request.notification(this, this.getString(R.string.uncompressed_recording), this.getString(R.string.uncompressed_recording2));
            mAppData.setVisitedVersion(RehearsalAssistant.currentVersion);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        mHelpMenuItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_menu_help);
        String switchText;
        switchText = this.getString(R.string.project_manager);
        mSwitchMenuItem = menu.add(switchText).setIcon(android.R.drawable.ic_menu_more);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            if (item == mSwitchMenuItem) {
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

    long projectId() {
        return mProjectId;
    }
}
