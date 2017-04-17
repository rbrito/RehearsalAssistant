package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

class Request {
    public static void cancellable_confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation) {
        new Dialog(context, title, content, confirmation, true);
    }

    public static void confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation) {
        new Dialog(context, title, content, confirmation, false);
    }

    public static void notification(Context context, String title, String content) {
        new Dialog(context, title, content, null, false);
    }

}

class Dialog {
    public Dialog(Context context, String title, String content, DialogInterface.OnClickListener confirmation, boolean cancellable) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton
                        (
                                context.getString(R.string.ok),
                                confirmation
                        );
        if (cancellable)
            dialog.setNegativeButton(context.getString(R.string.cancel), null);
        dialog.show();
    }
}
