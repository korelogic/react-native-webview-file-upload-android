package com.oblongmana.webviewfileuploadandroid;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.os.Build;


import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.views.webview.ReactWebViewManager;
import com.oblongmana.webviewfileuploadandroid.AndroidWebViewModule;

public class AndroidWebViewManager extends ReactWebViewManager {

    private Activity mActivity = null;
    private AndroidWebViewPackage aPackage;
    public String getName() {
        return "AndroidWebView";
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        WebView view = super.createViewInstance(reactContext);
        //Now do our own setWebChromeClient, patching in file chooser support
        final AndroidWebViewModule module = this.aPackage.getModule();
        mActivity = reactContext.getCurrentActivity();
        view.setWebChromeClient(new VideoWebChromeClient(mActivity, view){

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                module.setUploadMessage(uploadMsg);
                openFileChooserView();

            }

            public boolean onJsConfirm (WebView view, String url, String message, JsResult result){
                return true;
            }

            public boolean onJsPrompt (WebView view, String url, String message, String defaultValue, JsPromptResult result){
                return true;
            }

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                module.setUploadMessage(uploadMsg);
                openFileChooserView();
            }

            // For Android  > 4.1.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                module.setUploadMessage(uploadMsg);
                openFileChooserView();
            }

            // For Android > 5.0
            public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                Log.d("customwebview", "onShowFileChooser");

                module.setmUploadCallbackAboveL(filePathCallback);
                openFileChooserView();
                return true;
            }

            private void openFileChooserView(){
                try {
                    final Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                    galleryIntent.setType("image/*");
                    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose File");
                    module.getActivity().startActivityForResult(chooserIntent, 1);
                } catch (Exception e) {
                    Log.d("customwebview", e.toString());
                }
            }
        });
        view.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimetype,
                    long contentLength) {

                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    return;
                }

                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                String downloadMessage = "Downloading " + fileName;

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setTitle(fileName);
                request.setDescription(downloadMessage);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager dm = (DownloadManager) module.getActivity().getBaseContext().getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);

                Toast.makeText(module.getActivity().getApplicationContext(), downloadMessage, Toast.LENGTH_LONG).show();
            }
        });

        //jhwang edit start
        view.setFocusable(true);
        view.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus())
                        {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });

        // 设置4.2以后版本支持autoPlay，非用户手势促发
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            view.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        //jhwang edit end



        return view;
    }

    public void setPackage(AndroidWebViewPackage aPackage){
        this.aPackage = aPackage;
    }

    public AndroidWebViewPackage getPackage(){
        return this.aPackage;
    }
}
