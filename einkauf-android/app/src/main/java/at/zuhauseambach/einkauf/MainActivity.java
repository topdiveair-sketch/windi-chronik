package at.zuhauseambach.einkauf;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final long REFRESH_INTERVAL_MS = 60L * 60L * 1000L;

    private WebView webView;
    private boolean pageReady = false;
    private long lastRefreshRequest = 0L;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable hourlyRefresh = new Runnable() {
        @Override
        public void run() {
            requestFreshOffers(true);
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                requestFreshOffers(true);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestFreshOffers(false);
        refreshHandler.removeCallbacks(hourlyRefresh);
        refreshHandler.postDelayed(hourlyRefresh, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(hourlyRefresh);
        super.onPause();
    }

    private void requestFreshOffers(boolean force) {
        if (webView == null || !pageReady) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastRefreshRequest < 5000L) return;
        lastRefreshRequest = now;
        webView.evaluateJavascript(
            "if (typeof load === 'function') { load(false); }",
            null
        );
    }

    @Override
    protected void onDestroy() {
        refreshHandler.removeCallbacks(hourlyRefresh);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void printShoppingList() {
            runOnUiThread(() -> {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (manager == null || webView == null) return;
                String jobName = "Einkauf Zuhause am Bach";
                manager.print(jobName, webView.createPrintDocumentAdapter(jobName), new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build());
            });
        }
    }
}
