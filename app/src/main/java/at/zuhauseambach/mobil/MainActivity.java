package at.zuhauseambach.mobil;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int PORT = 8765;
    private static final int TIMEOUT_MS = 350;
    private static final long RETRY_MS = 8000L;

    private WebView webView;
    private final ExecutorService executor = Executors.newFixedThreadPool(32);
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private volatile boolean serverLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.clearCache(true);
        showSearchingPage("Hotel-PC wird automatisch gesucht …", "Die Verbindung wird selbstständig hergestellt. V91.7 prüft das lokale WLAN fortlaufend.");
        discoverServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!serverLoaded) discoverServer();
    }

    private void showSearchingPage(String title, String detail) {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{font-family:system-ui;background:#f4f7fa;color:#18314a;margin:0;padding:28px}" +
                ".box{max-width:600px;margin:40px auto;background:white;border-radius:18px;padding:26px;box-shadow:0 4px 20px #0002}" +
                "h1{color:#0b3d70;margin-top:0}.dot{font-size:42px;color:#4f8f25}.small{color:#607286;line-height:1.5}.ver{color:#789;font-size:14px;margin-top:22px}</style></head>" +
                "<body><div class='box'><div class='dot'>●</div><h1>Zuhause am Bach Mobil</h1>" +
                "<p><b>" + title + "</b></p>" +
                "<p class='small'>" + detail + "</p>" +
                "<p class='ver'>Version 91.7 · automatische Verbindung</p>" +
                "</div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void discoverServer() {
        if (!scanning.compareAndSet(false, true)) return;
        serverLoaded = false;
        executor.execute(() -> {
            try {
                List<String> subnets = localSubnets();
                AtomicBoolean found = new AtomicBoolean(false);
                List<Runnable> probes = new ArrayList<>();
                for (String subnet : subnets) {
                    for (int i = 1; i <= 254; i++) {
                        final String host = subnet + i;
                        probes.add(() -> {
                            if (found.get()) return;
                            if (isZabServer(host) && found.compareAndSet(false, true)) {
                                serverLoaded = true;
                                runOnUiThread(() -> webView.loadUrl("http://" + host + ":" + PORT + "/index.html?v=917"));
                            }
                        });
                    }
                }
                for (Runnable probe : probes) {
                    if (found.get()) break;
                    executor.execute(probe);
                }
                long waitUntil = System.currentTimeMillis() + 9000;
                while (!found.get() && System.currentTimeMillis() < waitUntil) {
                    try { Thread.sleep(120); } catch (InterruptedException ignored) { break; }
                }
                if (!found.get()) {
                    runOnUiThread(() -> {
                        showSearchingPage("Hotel-PC noch nicht gefunden – Suche läuft weiter …", "Die App versucht es automatisch erneut. Nach Installation der PC-Version V91.7 ist keine IP-Eingabe nötig.");
                        webView.postDelayed(this::discoverServer, RETRY_MS);
                    });
                }
            } finally {
                scanning.set(false);
            }
        });
    }

    private List<String> localSubnets() {
        Set<String> result = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) continue;
                    String ip = address.getHostAddress();
                    if (ip == null) continue;
                    String[] p = ip.split("\\.");
                    if (p.length == 4 && isPrivate(ip)) result.add(p[0] + "." + p[1] + "." + p[2] + ".");
                }
            }
        } catch (Exception ignored) { }
        if (result.isEmpty()) {
            result.add("192.168.1.");
            result.add("192.168.0.");
            result.add("10.0.0.");
        }
        return new ArrayList<>(result);
    }

    private boolean isPrivate(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
                ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    private boolean isZabServer(String host) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + host + ":" + PORT + "/api/status");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setUseCaches(false);
            if (conn.getResponseCode() != 200) return false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            String text = body.toString();
            return (text.contains("\"ok\": true") || text.contains("\"ok\":true")) && text.contains("server_version") && text.contains("data_file");
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
