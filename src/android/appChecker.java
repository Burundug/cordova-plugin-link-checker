package cordova.plugin.link.checker;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.util.Base64;


import com.google.gson.JsonObject;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class appChecker extends CordovaPlugin {


    private static final String TAG = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("checkUrl")) {
            String url = args.getString(0);
            this.checkUrl(url, callbackContext);
            return true;
        } else if (action.equals("openApp")) {
            String id = args.getString(0);
            this.openApp(id, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void checkUrl(String message, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Uri uri = Uri.parse(message);
                PackageManager pkManager = cordova.getActivity().getPackageManager();
                final JsonObject Out = new JsonObject();
                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resInfo = pkManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
                int i = 0;
//                ПРОВЕРКА INTENT != NULL https://stackoverflow.com/questions/3872063/how-to-launch-an-activity-from-another-application-in-android
                if (!resInfo.isEmpty()) {
                    for (ResolveInfo resolveInfo : resInfo) {
                        try {
                            ApplicationInfo appInfo = pkManager.getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
                            if ((appInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) == 0) {
                                Uri uriTest = Uri.parse("https://1234567890.ru");
                                final String packageName = appInfo.packageName;
                                final Intent intentFinal = new Intent(Intent.ACTION_VIEW);
                                final JsonObject obj = new JsonObject();
                                final Intent intentBrowser = new Intent(Intent.ACTION_VIEW);
                                intentFinal.setData(uri);
                                intentFinal.setPackage(packageName);
                                intentBrowser.setData(uriTest);
                                intentBrowser.setPackage(packageName);
                                if (intentFinal.resolveActivity(pkManager) != null && intentBrowser.resolveActivity(pkManager) == null) {
                                    Drawable icon = pkManager.getApplicationIcon(appInfo);
                                    String base64Icon = appChecker.encodeIcon(icon);
                                    String title = (String) pkManager.getApplicationLabel(appInfo);
                                    obj.addProperty("name", title);
                                    obj.addProperty("packageName", packageName);
                                    obj.addProperty("icon", base64Icon);
                                    Out.add(String.valueOf(i), obj);
                                    i++;
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
                callbackContext.success(Out.toString());
            }
        });
    }

    public static String encodeIcon(Drawable icon) {
        if (icon != null) {
            BitmapDrawable drawable = ((BitmapDrawable) icon);
            Bitmap bitmap = drawable.getBitmap();
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS);
            return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT).replace("\n", "").replace("\r", "");
        }
        return null;
    }

    public void openApp(String message, CallbackContext callbackContext) {
        Uri uri = Uri.parse("https://qr.nspk.ru/AS10003P3RH0LJ2A9ROO038L6NT5RU1M?type=01&bank=000000000001&sum=10000&cur=RUB&crc=F3D0");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(message);
        intent.setData(uri);
        cordova.getActivity().startActivity(intent);
        }
}
