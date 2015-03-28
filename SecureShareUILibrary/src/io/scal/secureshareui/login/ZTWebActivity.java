package io.scal.secureshareui.login;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.scribe.model.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.ResponseHandler;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicNameValuePair;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;
import io.scal.secureshareui.lib.Util;

/**
 * Created by mnbogner on 3/26/15.
 */
public class ZTWebActivity extends Activity {

    private static final String TAG = "ZTWebActivity";

    private WebView mWebview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        String authorizationUrl = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            authorizationUrl = extras.getString("authorizationUrl");
        }

        if ((authorizationUrl == null) || (authorizationUrl.length() == 0)) {
            Log.e("ZT OAUTH", "NO AUTHORIZATION URL FOUND");
            finish();
            return;
        }

        // check for tor settings and set proxy
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean useTor = settings.getBoolean("pusetor", false);

        if (useTor) {
            Log.d(TAG, "user selected \"use tor\"");

            OrbotHelper orbotHelper = new OrbotHelper(getApplicationContext());
            if ((!orbotHelper.isOrbotInstalled()) || (!orbotHelper.isOrbotRunning())) {
                Log.e(TAG, "user selected \"use tor\" but orbot is not installed or not running");
                finish();
                return;
            } else {
                try {
                    WebkitProxy.setProxy("android.app.Application", getApplicationContext(), Util.ORBOT_HOST, Util.ORBOT_HTTP_PORT);
                } catch (Exception e) {
                    Log.e(TAG, "user selected \"use tor\" but an exception was thrown while setting the proxy: " + e.getLocalizedMessage());
                    finish();
                    return;
                }
            }
        } else {
            Log.d(TAG, "user selected \"don't use tor\"");
        }

        mWebview = new WebView(this);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setVisibility(View.VISIBLE);

        setContentView(mWebview);

        mWebview.setWebViewClient(new WebViewClient() {
            Intent resultIntent = new Intent();

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("ZT OAUTH", "LOADING URL: " + url);

                if (url.contains("oauth_token=") && url.contains("oauth_verifier=")) {

                    Log.d("ZT OAUTH", "GOT TOKEN/VERIFIER");

                    Uri uri = Uri.parse(url);
                    String token = uri.getQueryParameter("oauth_token");
                    String verifier = uri.getQueryParameter("oauth_verifier");
                    resultIntent.putExtra("token", token);
                    resultIntent.putExtra("verifier", verifier);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    return true;
                } else {

                    Log.d("ZT OAUTH", "NO TOKEN/VERIFIER");

                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept-Language", "en");

        mWebview.loadUrl(authorizationUrl, headers);
    }

    @Override
    public void finish() {
        super.finish();
        Util.clearWebviewAndCookies(mWebview, this);
    }
}
