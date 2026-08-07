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
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final int PORT = 8765;
    private static final int TIMEOUT_MS = 350;
    private static final long RETRY_MS = 7000L;

    private WebView webView;
    private final ExecutorService executor = Executors.newFixedThreadPool(36);
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
        showStatus("Hotel-PC wird gesucht …", "Netzwerk wird geprüft.", networkSummary(), 0, 0);
        discoverServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!serverLoaded) discoverServer();
    }

    private void showStatus(String title, String detail, String network, int checked, int total) {
        String progress = total > 0 ? ("Geprüft: " + checked + " von " + total + " Adressen") : "Scan wird vorbereitet";
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{font-family:system-ui;background:#f4f7fa;color:#18314a;margin:0;padding:22px}" +
                ".box{max-width:600px;margin:28px auto;background:white;border-radius:18px;padding:24px;box-shadow:0 4px 20px #0002}" +
                "h1{color:#0b3d70;margin-top:0}.dot{font-size:40px;color:#4f8f25}.small{color:#607286;line-height:1.5}" +
                ".diag{margin-top:18px;background:#eef4f8;border-radius:12px;padding:14px;font-size:14px;line-height:1.55;color:#345}" +
                ".ver{color:#789;font-size:13px;margin-top:20px}</style></head>" +
                "<body><div class='box'><div class='dot'>●</div><h1>Zuhause am Bach Mobil</h1>" +
                "<p><b>" + esc(title) + "</b></p><p class='small'>" + esc(detail) + "</p>" +
                "<div class='diag'><b>Diagnose</b><br>" + esc(network) + "<br>" + esc(progress) + "<br>Gesuchter Dienst: TCP " + PORT + " /api/status</div>" +
                "<p class='ver'>Version 91.8 · automatische Verbindung mit Diagnose</p></div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String networkSummary() {
        List<String> ips = localIps();
        if (ips.isEmpty()) return "Keine private IPv4-Adresse erkannt. WLAN prüfen.";
        return "Handy-IP: " + String.join(", ", ips) + " | Subnetz(e): " + String.join(", ", localSubnets());
    }

    private void discoverServer() {
        if (!scanning.compareAndSet(false, true)) return;
        serverLoaded = false;
        executor.execute(() -> {
            try {
                List<String> subnets = localSubnets();
                int total = subnets.size() * 254;
                AtomicBoolean found = new AtomicBoolean(false);
                AtomicInteger checked = new AtomicInteger(0);
                if (subnets.isEmpty()) {
                    runOnUiThread(() -> showStatus("Kein WLAN-Netz erkannt", "Das Handy hat keine nutzbare private IPv4-Adresse. Bitte WLAN-Verbindung prüfen.", networkSummary(), 0, 0));
                    return;
                }

                for (String subnet : subnets) {
                    for (int i = 1; i <= 254; i++) {
                        final String host = subnet + i;
                        executor.execute(() -> {
                            if (found.get()) return;
                            boolean ok = isZabServer(host);
                            int n = checked.incrementAndGet();
                            if (ok && found.compareAndSet(false, true)) {
                                serverLoaded = true;
                                runOnUiThread(() -> webView.loadUrl("http://" + host + ":" + PORT + "/index.html?v=918"));
                                return;
                            }
                            if (n % 40 == 0 && !found.get()) {
                                runOnUiThread(() -> showStatus("Hotel-PC wird gesucht …", "Das WLAN ist vorhanden, der Mobilserver wurde bisher aber noch nicht gefunden.", networkSummary(), n, total));
                            }
                        });
                    }
                }

                long waitUntil = System.currentTimeMillis() + 12000;
                while (!found.get() && System.currentTimeMillis() < waitUntil) {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) { break; }
                }
                if (!found.get()) {
                    int n = checked.get();
                    runOnUiThread(() -> {
                        showStatus("Hotel-PC nicht erreichbar", "Wenn PC und Handy im selben WLAN sind, ist sehr wahrscheinlich Port 8765 am PC oder die Gerätekommunikation im Router blockiert. Die Suche startet automatisch erneut.", networkSummary(), n, total);
                        webView.postDelayed(this::discoverServer, RETRY_MS);
                    });
                }
            } finally {
                scanning.set(false);
            }
        });
    }

    private List<String> localIps() {
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
                    if (ip != null && isPrivate(ip)) result.add(ip);
                }
            }
        } catch (Exception ignored) { }
        return new ArrayList<>(result);
    }

    private List<String> localSubnets() {
        Set<String> result = new LinkedHashSet<>();
        for (String ip : localIps()) {
            String[] p = ip.split("\\.");
            if (p.length == 4) result.add(p[0] + "." + p[1] + "." + p[2] + ".");
        }
        if (result.isEmpty()) {
            result.add("192.168.1.");
            result.add("192.168.0.");
            result.add("10.0.0.");
        }
        return new ArrayList<>(result);
    }

    private boolean isPrivate(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") || ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
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
