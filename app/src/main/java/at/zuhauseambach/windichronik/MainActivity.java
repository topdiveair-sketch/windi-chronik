package at.zuhauseambach.windichronik;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri data = result.getData().getData();
                if (data != null) results = new Uri[]{data};
                else if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = result.getData().getClipData().getItemAt(i).getUri();
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackNew,
                    FileChooserParams fileChooserParams) {
                filePathCallback = filePathCallbackNew;
                Intent intent = fileChooserParams.createIntent();
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileChooserLauncher.launch(intent);
                return true;
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSafeBrowsingEnabled(true);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    public final class AndroidBridge {
        @JavascriptInterface
        public void shareCheckin(String json, String requestedFilename) {
            runOnUiThread(() -> {
                try {
                    String filename = sanitizeFilename(requestedFilename);
                    File shareDir = new File(getCacheDir(), "shared");
                    if (!shareDir.exists() && !shareDir.mkdirs()) {
                        throw new IllegalStateException("Freigabeordner konnte nicht erstellt werden.");
                    }

                    File file = new File(shareDir, filename);
                    try (FileOutputStream output = new FileOutputStream(file, false)) {
                        output.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".fileprovider",
                        file
                    );

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/json");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.putExtra(Intent.EXTRA_SUBJECT, "Gäste-Check-in Zuhause am Bach");
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Check-in-Datei teilen"));
                } catch (Exception error) {
                    notifyShareError(error.getMessage());
                }
            });
        }
    }

    private String sanitizeFilename(String value) {
        String safe = value == null ? "ZAB_CHECKIN.json" : value;
        safe = safe.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!safe.toLowerCase().endsWith(".json")) safe += ".json";
        if (safe.length() > 100) safe = safe.substring(0, 95) + ".json";
        return safe;
    }

    private void notifyShareError(String message) {
        if (webView == null) return;
        String safeMessage = message == null ? "Unbekannter Fehler" :
            message.replace("\\", "\\\\").replace("'", "\\'");
        webView.evaluateJavascript(
            "window.zabShareError && window.zabShareError('" + safeMessage + "');",
            null
        );
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
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
}
