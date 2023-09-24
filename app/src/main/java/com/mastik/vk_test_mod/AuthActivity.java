package com.mastik.vk_test_mod;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AuthActivity extends AppCompatActivity {
    public static final int APP_ID = 6121396;//7681504;
    public static final String API_AUTHENTICATION = "api_token", ACCESS_TOKEN = "access_token", USER_ID = "user_id",
            VK_APP_PACKAGE_ID = "com.vkontakte.android",
            VK_APP_AUTH_ACTION = "com.vkontakte.android.action.SDK_AUTH",
            VK_EXTRA_CLIENT_ID = "client_id",
            VK_EXTRA_SCOPE = "scope",
            TOKEN_SCOPE = "offline,messages,docs,status,friends,photos";
    private WebView webView;

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuth();
        startWebViewAuth();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        checkUserAuth();
        startWebViewAuth();
    }

    @Override
    public void onBackPressed() {
//        if(webView.canGoBack())
//            webView.goBack();
//        else
        finish();
    }

    private void checkUserAuth(){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(API_AUTHENTICATION, Context.MODE_PRIVATE);
        if(prefs.contains("access_token")){
            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    private void startWebViewAuth() {
        String uri = "https://oauth.vk.com/authorize?client_id=" + APP_ID + "&display=mobile&redirect_uri=https://oauth.vk.com/blank.html&scope=" + TOKEN_SCOPE + "&response_type=token&v=5.131&state=123456";
        webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Pattern tokenPattern = Pattern.compile("access_token=(\\w|\\.|-|_)+&");
                Pattern userIdPattern = Pattern.compile("user_id=\\d+&");
                Matcher tokenMatcher = tokenPattern.matcher(req.getUrl().toString());
                Matcher userIdMatcher = userIdPattern.matcher(req.getUrl().toString());
                System.out.println(req.getUrl());
                if (tokenMatcher.find()) {
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(API_AUTHENTICATION, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString(ACCESS_TOKEN, req.getUrl().toString().substring(tokenMatcher.start() + "access_token=".length(), tokenMatcher.end() - 1));
                    if (userIdMatcher.find())
                        edit.putInt(USER_ID, Integer.parseInt(req.getUrl().toString().substring(userIdMatcher.start() + "user_id=".length(), userIdMatcher.end() - 1)));
                    edit.apply();
                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                String message = "SSL Certificate error.";
                switch (error.getPrimaryError()) {
                    case SslError.SSL_UNTRUSTED:
                        message = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        message = "The certificate has expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        message = "The certificate Hostname mismatch.";
                        break;
                    case SslError.SSL_NOTYETVALID:
                        message = "The certificate is not yet valid.";
                        break;
                }
                message += " Do you want to continue anyway?";

                builder.setTitle("SSL Certificate Error");
                builder.setMessage(message);
                builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
                builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        webView.loadUrl(uri);
    }

    private void startVkAppAuth() {
        //Requires that app being approved by VK, so not used
        try {
            ActivityResultLauncher<String> mGetContent =
                    registerForActivityResult(
                            new ActivityResultContract<String, String>() {
                                @Override
                                public String parseResult(int i, Intent intent) {
                                    System.out.println(intent);
                                    intent.getExtras().getString("access_token");
                                    intent.getExtras().getInt("user_id");
                                    return intent.getData().toString();
                                }

                                @Override
                                public Intent createIntent(Context context, String input) {
                                    Intent vkAppInt = new Intent(VK_APP_AUTH_ACTION, null);
                                    vkAppInt.setPackage(VK_APP_PACKAGE_ID);
                                    vkAppInt.putExtra(VK_EXTRA_CLIENT_ID, APP_ID);
                                    vkAppInt.putExtra(VK_EXTRA_SCOPE, TOKEN_SCOPE);
                                    vkAppInt.putExtra("redirect_url", "https://oauth.vk.com/blank.html");
                                    vkAppInt.putExtra("revoke", true);
                                    return vkAppInt;
                                }
                            },
                            new ActivityResultCallback<String>() {
                                @Override
                                public void onActivityResult(String uri) {
                                    System.out.println(uri);
                                }
                            });
            mGetContent.launch(TOKEN_SCOPE, null);

        } catch (ActivityNotFoundException e) {
            Log.e("Auth", "Official VK app not found");
            startWebViewAuth();
        }
    }
}