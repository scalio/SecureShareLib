
package io.scal.secureshareui.controller;

import io.scal.secureshareui.login.FacebookLoginActivity;
import io.scal.secureshareui.model.Account;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

public class FacebookSiteController extends SiteController {
    public static final String SITE_NAME = "Facebook";
    public static final String SITE_KEY = "facebook";
    private static final String TAG = "FacebookSiteController";

    public FacebookSiteController(Context context, Handler handler, String jobId) {
        super(context, handler, jobId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void startAuthentication(Account account) {
        Intent intent = new Intent(mContext, FacebookLoginActivity.class);
        intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
        ((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE); // FIXME not a safe cast, context might be a service
    }

    @Override
    public void upload(Account account, HashMap<String, String> valueMap) {
		Log.d(TAG, "Upload file: Entering upload");
		
		String title = valueMap.get("title");
		String body = valueMap.get("body");
		String mediaPath = valueMap.get("mediaPath");
		boolean useTor = Boolean.getBoolean(valueMap.get("useTor"));
        Session session = Session.openActiveSessionFromCache(mContext);

        // setup callback
        Request.Callback uploadMediaRequestCallback = new Request.OnProgressCallback() {
            @Override
            public void onCompleted(Response response) {

                // post fail
                if (response.getError() != null) {
                    Log.d(TAG, "media upload problem. Error= " + response.getError());
                    jobFailed(1, response.getError().toString());
                    return;
                }

                Object graphResponse = response.getGraphObject().getProperty("id");

                // upload fail
                if (graphResponse == null || !(graphResponse instanceof String)
                        || TextUtils.isEmpty((String) graphResponse)) {
                    Log.d(TAG, "failed media upload/no response");

                    jobFailed(0, "failed media upload/no response");
                }
                // upload success
                else {
                    jobSucceeded("" + graphResponse);
                    Log.d(TAG, "successful media upload: " + graphResponse);
                }
            }

            @Override
            public void onProgress(long current, long max) {
                float percent = ((float) current) / ((float) max);
                jobProgress(percent, "Facebook uploading...");
            }
        };

        // upload File
        File mediaFile = new File(mediaPath);
        Bundle parameters = null;    
        Request request = null;
        try {
        	if(super.isVideoFile(mediaFile)) {
        		request = Request.newUploadVideoRequest(session, mediaFile, uploadMediaRequestCallback);
        		
        		if (torCheck(useTor, mContext))
        		{
        		    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
                    request.setProxy(proxy);
        		}
        		    
        		
        		parameters = request.getParameters();
        		
        		//video params
        		parameters.putString("title", title);
        		parameters.putString("description", body);
        	}
        	else if(super.isImageFile(mediaFile)){
        		request = Request.newUploadPhotoRequest(session, mediaFile, uploadMediaRequestCallback);
        		
        		if (torCheck(useTor, mContext))
                {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
                    request.setProxy(proxy);
                }
        		
        		parameters = request.getParameters();
        		
        		//image params
        		parameters.putString("name", title);
        	}
        	else {
        		Log.d(TAG, "media type not supported");
        		return;
        	}
        	
            request.setParameters(parameters);
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        request.executeAsync();
    }
}
