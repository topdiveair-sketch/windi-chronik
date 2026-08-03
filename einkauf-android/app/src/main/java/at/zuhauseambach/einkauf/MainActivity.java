package at.zuhauseambach.einkauf;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;

import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private static final long REFRESH_INTERVAL_MS = 60L * 60L * 1000L;
    private static final int CAMERA_REQUEST = 4101;
    private static final int GALLERY_REQUEST = 4102;
    private static final int CAMERA_PERMISSION_REQUEST = 5101;

    private WebView webView;
    private boolean pageReady = false;
    private long lastRefreshRequest = 0L;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());

    private final Runnable hourlyRefresh = new Runnable() {
        @Override public void run() {
            requestFreshOffers(true);
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        Button cameraButton = new Button(this);
        cameraButton.setText("📷 Artikel erkennen");
        cameraButton.setTextSize(14);
        cameraButton.setAllCaps(false);
        cameraButton.setOnClickListener(v -> showImageSourceDialog());
        FrameLayout.LayoutParams cameraParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.BOTTOM);
        cameraParams.setMargins(18, 0, 0, 22);
        root.addView(cameraButton, cameraParams);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                requestFreshOffers(true);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html?startup=" + System.currentTimeMillis());
    }

    @Override protected void onResume() {
        super.onResume();
        requestFreshOffers(true);
        refreshHandler.removeCallbacks(hourlyRefresh);
        refreshHandler.postDelayed(hourlyRefresh, REFRESH_INTERVAL_MS);
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(hourlyRefresh);
        super.onPause();
    }

    private void requestFreshOffers(boolean force) {
        if (webView == null || !pageReady) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastRefreshRequest < 5000L) return;
        lastRefreshRequest = now;
        webView.clearCache(false);
        webView.evaluateJavascript(
                "(function(){try{if(typeof load==='function'){load(true);return 'load';}" +
                "location.reload(true);return 'reload';}catch(e){location.reload(true);return 'error-reload';}})()",
                null);
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Artikel erkennen")
                .setItems(new String[]{"Foto aufnehmen", "Bild aus Galerie wählen"}, (dialog, which) -> {
                    if (which == 0) ensureCameraPermissionAndOpen();
                    else openGallery();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void ensureCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        openCamera();
    }

    private void openCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Keine Kamera-App verfügbar", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivityForResult(camera, CAMERA_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Kamera konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        gallery.addCategory(Intent.CATEGORY_OPENABLE);
        gallery.setType("image/*");
        try {
            startActivityForResult(gallery, GALLERY_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Galerie konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION_REQUEST) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Kamerazugriff wurde nicht erlaubt. Bitte in den App-Einstellungen freigeben.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            Bitmap bitmap = null;
            if (requestCode == GALLERY_REQUEST && data != null && data.getData() != null) {
                Uri uri = data.getData();
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                Object raw = data.getExtras().get("data");
                if (raw instanceof Bitmap) bitmap = (Bitmap) raw;
            }
            if (bitmap == null) {
                Toast.makeText(this, "Das Foto wurde nicht von der Kamera zurückgegeben", Toast.LENGTH_LONG).show();
                return;
            }
            recognizeArticle(bitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Foto konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
        }
    }

    private void recognizeArticle(Bitmap bitmap) {
        Toast.makeText(this, "Artikel wird erkannt …", Toast.LENGTH_SHORT).show();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        final String[] barcodeValue = {""};
        final String[] recognizedText = {""};
        AtomicInteger finished = new AtomicInteger(0);
        Runnable done = () -> {
            if (finished.incrementAndGet() == 2) {
                runOnUiThread(() -> showRecognizedArticleDialog(barcodeValue[0], recognizedText[0]));
            }
        };

        BarcodeScanning.getClient().process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        Barcode b = barcodes.get(0);
                        barcodeValue[0] = b.getRawValue() == null ? "" : b.getRawValue();
                    }
                })
                .addOnCompleteListener(task -> done.run());

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener(result -> recognizedText[0] = result.getText())
                .addOnCompleteListener(task -> done.run());
    }

    private void showRecognizedArticleDialog(String barcode, String text) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);

        EditText article = new EditText(this);
        article.setHint("Artikel");
        article.setText(suggestArticleName(text, barcode));
        form.addView(article);

        EditText brand = new EditText(this);
        brand.setHint("Marke / erkannter Text");
        brand.setText(suggestBrand(text, barcode));
        form.addView(brand);

        Spinner shop = new Spinner(this);
        String[] shops = {"HOFER", "PENNY", "BILLA", "SPAR"};
        shop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shops));
        form.addView(shop);

        EditText quantity = new EditText(this);
        quantity.setHint("Menge");
        quantity.setText("1");
        quantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        form.addView(quantity);

        TextView info = new TextView(this);
        info.setPadding(0, pad / 2, 0, 0);
        info.setText(barcode.isEmpty() ? "Kein Barcode erkannt" : "Barcode: " + barcode);
        form.addView(info);

        new AlertDialog.Builder(this)
                .setTitle("Erkannter Artikel")
                .setView(form)
                .setNegativeButton("Abbrechen", null)
                .setNeutralButton("Nur anzeigen", null)
                .setPositiveButton("Zur Einkaufsliste", (d, w) -> addRecognizedItem(
                        article.getText().toString().trim(),
                        brand.getText().toString().trim(),
                        shops[shop.getSelectedItemPosition()],
                        quantity.getText().toString().trim(),
                        barcode))
                .show();
    }

    private String suggestArticleName(String text, String barcode) {
        if (text != null) {
            for (String line : text.split("\\n")) {
                String s = line.trim();
                if (s.length() >= 3 && s.length() <= 45 && !s.matches(".*\\d{5,}.*")) return s;
            }
        }
        return barcode.isEmpty() ? "Neuer Artikel" : "Artikel " + barcode;
    }

    private String suggestBrand(String text, String barcode) {
        if (text == null || text.trim().isEmpty()) return barcode;
        String compact = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return compact.length() > 90 ? compact.substring(0, 90) : compact;
    }

    private void addRecognizedItem(String item, String brand, String shop, String quantity, String barcode) {
        if (item.isEmpty()) item = "Neuer Artikel";
        try {
            JSONObject o = new JSONObject();
            o.put("id", "photo" + System.currentTimeMillis());
            o.put("item", item);
            o.put("category", "Foto-Erkennung");
            o.put("shop", shop);
            o.put("brand", brand);
            o.put("quantity", quantity.isEmpty() ? "1" : quantity);
            o.put("price", 0);
            o.put("verified", false);
            o.put("manual", true);
            o.put("barcode", barcode);
            String json = JSONObject.quote(o.toString());
            String js = "(function(){var a=JSON.parse(localStorage.getItem('customItems')||'[]');" +
                    "a.push(JSON.parse(" + json + "));localStorage.setItem('customItems',JSON.stringify(a));location.reload();})()";
            webView.evaluateJavascript(js, null);
            Toast.makeText(this, "Artikel wurde zur Einkaufsliste hinzugefügt", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Artikel konnte nicht gespeichert werden", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        refreshHandler.removeCallbacks(hourlyRefresh);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class AndroidBridge {
        @JavascriptInterface public void printShoppingList() {
            runOnUiThread(() -> {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (manager == null || webView == null) return;
                String jobName = "Einkauf Zuhause am Bach";
                manager.print(jobName, webView.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                .build());
            });
        }
    }
}
