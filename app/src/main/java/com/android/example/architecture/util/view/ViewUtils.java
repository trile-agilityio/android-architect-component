package com.android.example.architecture.util.view;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;

public class ViewUtils {

    /**
     * dismissKeyboard
     *
     * @param activity The {@link Activity}
     * @param windowToken The {@link IBinder}
     */
    public static void dismissKeyboard(Activity activity, IBinder windowToken) {
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }
}
