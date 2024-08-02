package cordova.plugin.link.checker;

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.Serializable
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit


class appChecker : CordovaPlugin() {
    private var _callbackContext: CallbackContext? = null
    private val screenChangeEvent: MutableLiveData<SingleEvent<Screen>> = MutableLiveData()
    val screenChangeEventLiveData: LiveData<SingleEvent<Screen>> = screenChangeEvent
    @SuppressLint("QueryPermissionsNeeded")

    private fun prepareNspkDeeplinkWithScheme(schema: String, deepLink: String): Uri {
        val raw = Uri.parse(deepLink)
        return Uri.Builder().apply {
            this.scheme(schema)
            this.authority(raw.authority)
            this.path(raw.path)
        }.build()
    }

    @SuppressLint("QueryPermissionsNeeded")
    internal fun getBankAppsNew(deeplink: String, banks: List<NspkC2bResponse.NspkAppInfo>): JsonObject {
        val PackageManager = cordova.getActivity().packageManager
        val sbpIntent = Intent(Intent.ACTION_VIEW)
        val Out = JsonObject()
        var i = 0;
        banks.forEach { appInfo ->
            val deepLink = prepareNspkDeeplinkWithScheme(appInfo.schema, deeplink)
            sbpIntent.setDataAndNormalize(deepLink)
            PackageManager.queryIntentActivities(sbpIntent, 0).forEach {
                            i++;
                Log.e("RESOLVEINVO", it.activityInfo.packageName)
            val obj = JsonObject()
                val deepLink = prepareNspkDeeplinkWithScheme(appInfo.schema, deeplink)
                sbpIntent.setDataAndNormalize(deepLink)
            obj.addProperty("name", appInfo.bankName)
            obj.addProperty("packageName", it.activityInfo.packageName);
            obj.addProperty("icon", appInfo.logoURL);
            obj.addProperty("deeplink", appInfo.schema);
            Out.add(i.toString(), obj);
            }
        }
        return Out
    }



    @SuppressLint("QueryPermissionsNeeded")
    private fun getBankApps(link: String, banks: List<Any>): JsonObject {
        // get sbp packages
        val sbpIntent = Intent(Intent.ACTION_VIEW)
        val PackageManager = cordova.getActivity().packageManager
        sbpIntent.setDataAndNormalize(Uri.parse(link))
        val sbpPackages = PackageManager.queryIntentActivities(sbpIntent, 0)
            .map { it.activityInfo.packageName }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val browserPackages = PackageManager.queryIntentActivities(browserIntent, 0)
            .map { it.activityInfo.packageName }
        // filter out browsers
        val nonBrowserSbpPackages = sbpPackages.filter { it !in browserPackages }
        // get bank packages
        val bankPackages = PackageManager.getInstalledApplications(0)
            .map {it.packageName}.filter { it in banks }
        val Out = JsonObject()
        var i = 0;
        val mutable = mutableListOf<String>().apply {
            addAll(nonBrowserSbpPackages)
            addAll(bankPackages)
        }.distinct()
        mutable.map {
            i++;
            val obj = JsonObject()
            val title = PackageManager.getApplicationLabel(PackageManager.getApplicationInfo( it, 0))
            obj.addProperty("name", title.toString())
            obj.addProperty("packageName", it);
            obj.addProperty("icon", drawableToBitmap(PackageManager.getApplicationIcon(it)).toString());
            Out.add(i.toString(), obj);
        }
        return Out
    }
    fun drawableToBitmap(drawable: Drawable): String? {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
                return Base64.encodeToString(imageBytes, Base64.DEFAULT)
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)

    }


        fun openLink(url: String) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            cordova.activity.startActivity(intent);
        }
      fun openSbpDeepLinkInBank(packageName: String, url: String, schema: String) {
//          var intent: Intent? = cordova.activity.packageManager.getLaunchIntentForPackage(packageName)
//          if (intent != null) {
//              intent.action = Intent.ACTION_VIEW;
//              intent.putExtra("data",Uri.parse(url))
//          } else {
//              intent = Intent(Intent.ACTION_VIEW)
//              intent.data = Uri.parse(url)
//              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//          }
//          var intent: Intent =  Intent(Intent.ACTION_VIEW).setPackage(packageName);

          val deepLink = prepareNspkDeeplinkWithScheme(schema, url)
          val intent = Intent(Intent.ACTION_VIEW)
          intent.setDataAndNormalize(deepLink)
          try {
              cordova.activity.runOnUiThread {
                  cordova.startActivityForResult(this, intent, 112)
              }
          } catch (e: Exception) {
              Log.e("ERROR",e.toString())

          }


    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        val obj = JsonObject()
        obj.addProperty("status", true)
        when (resultCode) {
             Activity.RESULT_OK -> {
                 this.context.success(obj.toString())
             }
             Activity.RESULT_CANCELED ->  {
                 this.context.success(obj.toString())
             }
             AutoResolveHelper.RESULT_ERROR -> {
                 this.context.success(obj.toString())
             }
         }
     }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        when (requestCode) {
//            REQUEST_CODE -> {
//                if (resultCode == RESULT_OK) {
//                   Log.e(TAG,"RESULT_OK")
//                    // используем bitmap
//                } else {
//                    // не удалось получить фото
//                }
//            }
//            else -> super.onActivityResult(requestCode, resultCode, data)
//        }
//    }


    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(40000, TimeUnit.MILLISECONDS)
        .readTimeout(40000, TimeUnit.MILLISECONDS)
        .build()
    private val gson: Gson = GsonBuilder().create()
    val urlconn = "https://qr.nspk.ru/proxyapp/c2bmembers.json"
    lateinit var context: CallbackContext
    private fun connectToUrl(url: String): String {
        val okHttpRequest = okhttp3.Request.Builder().url(urlconn).get()
            .header("User-Agent", System.getProperty("http.agent")!!)
            .header("Accept", "application/json")
            .build()
        val call = okHttpClient.newCall(okHttpRequest)
        val okHttpResponse = call.execute()
        val responseCode = okHttpResponse.code
        val response: String = checkNotNull(okHttpResponse.body?.string())
        try {
            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val info = serializeData(response)
                val info = serializeData(response)
//            val banks: Set<Any?> = (gson.fromJson(response, List::class.java) as List).map {
//                ((it as Map<*, *>)["target"] as Map<*, *>)["package_name"]
//            }.toSet()
                val a = getBankAppsNew(url,info.dictionary)
            return if(a.size() > 0) {
                a.toString();
            } else {
                "";
            }
            } else {
                return "";
            }

        } catch (e: IOException) {

            return "";
        } catch (e: JsonParseException) {
            return "";

        }



    }
    @Throws(JSONException::class)
    override fun execute(action: String, data: JSONArray, callbackContext: CallbackContext): Boolean {
        context = callbackContext
        var result = true
        try {
            if (action == "checkUrl") {
                val url = "https://qr.nspk.ru/";
                connectToUrl(url)
                callbackContext.success(connectToUrl(url))
            } else if (action == "openCustom") {
                val obj: JSONObject = data.getJSONObject(0)
                openLink(obj.get("url").toString())
            } else if (action == "coolMethod") {
                val obj: JSONObject = data.getJSONObject(0)
                openSbpDeepLinkInBank(obj.get("name").toString(), obj.get("url").toString(), obj.get("deepLink").toString())
            } else {
                handleError("Invalid action")
                result = false
            }
        } catch (e: Exception) {
            handleException(e)
            result = false
        }

        return result
    }

    private fun serializeData(response: String): NspkC2bResponse {
        return gson.fromJson(response, NspkC2bResponse::class.java)
    }

    private fun handleError(errorMsg: String) {
        try {
            Log.e(TAG, errorMsg)
            context.error(errorMsg)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun handleException(exception: Exception) {
        handleError(exception.toString())
    }

    companion object {

        protected val TAG = "appChecker"
    }
}

public class SingleEvent<out T>(val value: T) {

    private var hasBeenHandled = false

    fun getValueIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            value
        }
    }
}

sealed class ScreenState

sealed class Screen : ScreenState()


class StartActivity : AppCompatActivity() {

    val activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
  
        }

    }
    fun start(intent: Intent) {
        activityLauncher.launch(intent)
    }

    companion object {

        protected val TAG = "appChecker"
    }
}



class NspkC2bResponse(
    @SerializedName("version")
    val version: String,
    @SerializedName("dictionary")
    val dictionary: List<NspkAppInfo>,
) : Serializable {
    class NspkAppInfo(
        @SerializedName("bankName")
        val bankName: String,
        @SerializedName("logoURL")
        val logoURL: String,
        @SerializedName("schema")
        val schema: String,
        @SerializedName("package_name")
        val packageName: String?,
    ) : Serializable
}

