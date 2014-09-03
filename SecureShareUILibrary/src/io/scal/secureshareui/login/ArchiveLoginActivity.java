package io.scal.secureshareui.login;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareuilibrary.R;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArchiveLoginActivity extends Activity {

	private final static String ARCHIVE_CREATE_ACCOUNT_URL = "https://archive.org/account/login.createaccount.php";
	private final static String ARCHIVE_LOGIN_URL = "https://archive.org/account/login.php";
	private final static String ARCHIVE_LOGGED_IN_URL = "https://archive.org/index.php";
	private final static String ARCHIVE_CREDENTIALS_URL = "https://archive.org/account/s3.php";

	private static int mAccessResult = Activity.RESULT_CANCELED;
	private static String mAccessKey = null;
    private static String mSecretKey = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		login();
	}

	@SuppressLint({ "SetJavaScriptEnabled" })
	private void login() {
		final WebView webview = new WebView(this);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.addJavascriptInterface(new JSInterface(), "htmlout");
		setContentView(webview);

		webview.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				//if logged in, hide and redirect to credentials
				if (url.equals(ARCHIVE_LOGGED_IN_URL)) {
					view.setVisibility(View.INVISIBLE);
					view.loadUrl(ARCHIVE_CREDENTIALS_URL);
					
					return true;
				} else if(url.equals(ARCHIVE_CREATE_ACCOUNT_URL)) {
						showAccountCreatedDialog(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
				}
				
				return false;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);		
				//if credentials page, inject JS for scraping
				if (url.equals(ARCHIVE_CREDENTIALS_URL)) {
		            String jsInjected = "javascript:window.htmlout.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');";
		            webview.loadUrl(jsInjected);
				}
			}
		});

		webview.loadUrl(ARCHIVE_LOGIN_URL);
	}
	
	private void parseArchiveCredentials(String rawHtml) {
		//strip code sections
		String startCode = "<code>";
		String endCode = "</code>";	
		int iStartCode = rawHtml.indexOf(startCode);
		int iEndCode = rawHtml.lastIndexOf(endCode);
		
		//user doesn't appear to be logged in
		if(iStartCode < 0 || iEndCode < 0) {
			return;
		}
		String rawCodes = rawHtml.substring(iStartCode, iEndCode);
		rawCodes = rawCodes.replaceAll("\\s", "");
		
		//strip codes
		char colon = ':';
		String brk = "<br";	
		int iFirstColon = rawCodes.indexOf(colon) + 1;
		int iLastColon = rawCodes.lastIndexOf(colon) + 1;
		int iFirstLt = rawCodes.indexOf(brk);
		int iLastLt = rawCodes.lastIndexOf(brk);
		
		mAccessKey = rawCodes.substring(iFirstColon, iFirstLt);
		mSecretKey = rawCodes.substring(iLastColon, iLastLt);
		
		if(null != mAccessKey && null != mSecretKey) {
			mAccessResult = Activity.RESULT_OK;
		}
		
		finish();
	}
	
	private void showAccountCreatedDialog(DialogInterface.OnClickListener positiveBtnClickListener) {
		new AlertDialog.Builder(this)
				.setTitle(getString(R.string.archive_title))
				.setMessage(getString(R.string.archive_message))
				.setPositiveButton(R.string.ok, positiveBtnClickListener).show();
	}
	
	class JSInterface {
	    @SuppressWarnings("null")
		@JavascriptInterface
		public void processHTML(String html) {			
			if(null == html) {
				return;
			}
			parseArchiveCredentials(html);
	    }
	}

	@Override
	public void finish() {
		Intent data = new Intent();
		data.putExtra(SiteController.EXTRAS_KEY_USERNAME, mAccessKey);
		data.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, mSecretKey);

		setResult(mAccessResult, data);
		super.finish();
	}
}
