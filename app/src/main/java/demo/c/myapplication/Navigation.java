package demo.c.myapplication;

import android.content.Context;
import android.content.Intent;

/**
 * Navigation is a helper class for common links throughout the app.
 */
public class Navigation {
    

    public static void goToStyleTransfer(Context context) {
        Intent styleActivity = new Intent(context, demo.c.myapplication.filming.StyleTransferActivity.class);
        context.startActivity(styleActivity);
    }
    public static void goToMain(Context context) {
        Intent storage = new Intent(context, demo.c.myapplication.MainActivity.class);
        context.startActivity(storage);
   }
    public static void goToBuildCamera(Context context) {
        Intent storage = new Intent(context, demo.c.myapplication.building.BuildCamera.class);
        context.startActivity(storage);
    }
    public static void goToexplain(Context context) {
        Intent storage = new Intent(context, demo.c.myapplication.ExplainPage.class);
        context.startActivity(storage);
    }

}
