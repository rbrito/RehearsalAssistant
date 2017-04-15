package urbanstew.RehearsalAssistant;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;

class AppDataAccess {
    private static final String[] projectsDataProjection =
            {
                    Projects._ID,
            };
    private final Context mContext;

    AppDataAccess(Context context) {
        mContext = context;
    }

    public long getCurrentProjectId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (preferences.contains("current_project_id"))
            return (preferences.getLong("current_project_id", -1));

        long id = getFirstProjectID();
        preferences.edit().putLong("current_project_id", id).apply();
        return id;
    }

    void setCurrentProjectId(long id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit().putLong("current_project_id", id).apply();
    }

    long getProjectIdNot(long id) {
        Cursor projectsCursor = mContext.getContentResolver().query(Projects.CONTENT_URI, projectsDataProjection, Projects._ID + "<>" + id, null, Projects.DEFAULT_SORT_ORDER);
        if (projectsCursor.getCount() > 0) {
            projectsCursor.moveToFirst();
            setCurrentProjectId(id = projectsCursor.getLong(0));
        }
        projectsCursor.close();
        return id;
    }

    private long getFirstProjectID() {
        Cursor c = mContext.getContentResolver().query(Projects.CONTENT_URI, projectsDataProjection, null, null, Projects.DEFAULT_SORT_ORDER);
        c.moveToFirst();
        long result = c.getLong(0);
        c.close();
        return result;
    }

    private long getRecorderWidgetProjectIdImpl(boolean createIfNeeded) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        long projectId = -1;
        if (preferences.contains("recorder_widget_project_id"))
            projectId = preferences.getLong("recorder_widget_project_id", -1);

        String[] projectsProjection =
                {
                        Projects._ID,
                        Projects.TYPE
                };

        Cursor projectsCursor;

        // if there was a projectId in preferences, make sure it exists and is
        if (projectId != -1) {
            projectsCursor = mContext.getContentResolver().query(ContentUris.withAppendedId(Projects.CONTENT_URI, projectId), projectsProjection, null, null, Projects.DEFAULT_SORT_ORDER);
            if (projectsCursor.getCount() > 0) {
                projectsCursor.moveToFirst();
                if (projectsCursor.getLong(1) == Projects.TYPE_SIMPLE) {
                    projectsCursor.close();
                    return projectId;
                }
            }
        }

        // otherwise, find an existing project or create a new one
        projectsCursor = mContext.getContentResolver().query(Projects.CONTENT_URI, projectsProjection, Projects.TYPE + "=" + Projects.TYPE_SIMPLE, null, Projects.DEFAULT_SORT_ORDER);
        if (projectsCursor.getCount() == 0) {
            if (!createIfNeeded)
                return -1;
            mContext.getContentResolver().insert(Rehearsal.Projects.CONTENT_URI, RehearsalData.valuesForMemoProject(mContext));
            projectsCursor.requery();
        }
        projectsCursor.moveToFirst();
        projectId = projectsCursor.getLong(0);
        projectsCursor.close();

        // save the new project id
        preferences.edit().putLong("recorder_widget_project_id", projectId).apply();

        return projectId;

    }

    long getRecorderWidgetProjectId() {
        return getRecorderWidgetProjectIdImpl(true);
    }

    void setRecorderWidgetProjectId(long id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit().putLong("recorder_widget_project_id", id).apply();
    }

    long getRecorderWidgetProjectIdIfExists() {
        return getRecorderWidgetProjectIdImpl(false);
    }

    float getVisitedVersion() {
        return getVisitedVersion("");
    }

    void setVisitedVersion(float version) {
        setVisitedVersion("", version);
    }

    private float getVisitedVersion(String what) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getFloat("app_visited_version" + what, 0);
    }

    private void setVisitedVersion(String what, float version) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.edit().putFloat("app_visited_version" + what, version).apply();
    }

    boolean lastVisitedVersionOlderThan(String what, float version) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        float oldVersion = preferences.getFloat("app_visited_version" + what, 0);
        preferences.edit().putFloat("app_visited_version" + what, RehearsalAssistant.currentVersion).apply();
        return oldVersion < version;
    }
}
