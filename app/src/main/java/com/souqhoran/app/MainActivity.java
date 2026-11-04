package com.souqhoran.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_RESULT_CODE = 1001;
    private static final String HOME_URL = "https://souq-horan.rf.gd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.parseColor("#060b16"));

        webView = new WebView(this);
        layout.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(layout);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                if (url == null) return false;

                // 1. التعامل مع روابط الواتساب بجميع صيغها (intent أو whatsapp أو wa.me)
                if (url.startsWith("whatsapp:") || url.contains("scheme=whatsapp") || url.contains("wa.me/") || url.contains("api.whatsapp.com")) {
                    openWhatsAppDirectly(url);
                    return true;
                }

                // 2. التعامل مع الاتصال الهاتفي والروابط الخارجية
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("intent:")) {
                    try {
                        Intent intent;
                        if (url.startsWith("intent:")) {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            intent.setPackage(null);
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        }
                        startActivity(intent);
                    } catch (Exception ignored) {
                    }
                    return true;
                }

                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(HOME_URL);
    }

    private void openWhatsAppDirectly(String url) {
        try {
            String finalUri = url;

            if (url.startsWith("intent://")) {
                int endIdx = url.indexOf("#Intent");
                String body = (endIdx != -1) ? url.substring("intent://".length(), endIdx) : url.substring("intent://".length());
                finalUri = "whatsapp://" + body;
            } else if (url.contains("wa.me/")) {
                String clean = url.replace("https://wa.me/", "whatsapp://send?phone=")
                                  .replace("http://wa.me/", "whatsapp://send?phone=")
                                  .replace("?text=", "&text=");
                finalUri = clean;
            }

            Uri uri = Uri.parse(finalUri);

            // المحاولة الأولى: فتح واتساب الأعمال مباشرة إذا كان مثبتاً
            try {
                Intent w4bIntent = new Intent(Intent.ACTION_VIEW, uri);
                w4bIntent.setPackage("com.whatsapp.w4b");
                startActivity(w4bIntent);
                return;
            } catch (Exception ignored) {
            }

            // المحاولة الثانية: فتح الواتساب العادي أو أي تطبيق واتساب مثبت على الهاتف
            Intent generalIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(generalIntent);
        } catch (Exception ignored) {
            // لا نحول المستخدم إلى متجر جوجل بلاي نهائياً
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
