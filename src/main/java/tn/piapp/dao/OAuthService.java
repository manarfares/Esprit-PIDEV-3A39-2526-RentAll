package org.example.services;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gestion du flow OAuth 2.0 (Google + Facebook) pour app desktop.
 *
 * Stratégie : "loopback IP redirect"
 *  1. Génération d'une URL d'autorisation
 *  2. Ouverture dans le navigateur système
 *  3. Petit serveur HTTP local (port 8765) qui capture le callback
 *  4. Échange code → access_token → user_info
 *  5. Retour d'un objet OAuthUserInfo au LoginController
 *
 * Avantages : compatible avec les exigences modernes de Google
 * (qui bloque les WebView embarquées pour cause d'insécurité).
 */
public class OAuthService {

    private static final int    LOCAL_PORT          = 8765;
    private static final String GOOGLE_AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private static final String FACEBOOK_AUTH_URL     = "https://www.facebook.com/v18.0/dialog/oauth";
    private static final String FACEBOOK_TOKEN_URL    = "https://graph.facebook.com/v18.0/oauth/access_token";
    private static final String FACEBOOK_USERINFO_URL = "https://graph.facebook.com/me?fields=id,name,email,picture";

    private final String googleClientId     = AppConfig.get("oauth.google.client.id");
    private final String googleClientSecret = AppConfig.get("oauth.google.client.secret");
    private final String googleRedirectUri  = AppConfig.get("oauth.google.redirect.uri");

    private final String facebookClientId     = AppConfig.get("oauth.facebook.client.id");
    private final String facebookClientSecret = AppConfig.get("oauth.facebook.client.secret");
    private final String facebookRedirectUri  = AppConfig.get("oauth.facebook.redirect.uri");

    /** Conteneur pour les infos retournées par le provider OAuth. */
    public static class OAuthUserInfo {
        public String provider;    // "google" ou "facebook"
        public String providerId;  // ID unique chez le provider
        public String email;
        public String name;
        public String picture;
    }

    // ============================================================
    //                          GOOGLE
    // ============================================================

    /** Lance le flow OAuth Google complet. Bloque jusqu'à la fin (timeout 2 min). */
    public OAuthUserInfo loginWithGoogle() throws Exception {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("oauth.google.client.id non configuré dans config.properties");
        }

        String state = UUID.randomUUID().toString();
        String scope = "openid email profile";

        String authUrl = GOOGLE_AUTH_URL
                + "?client_id="     + URLEncoder.encode(googleClientId,    StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode(scope,             StandardCharsets.UTF_8)
                + "&state="         + state
                + "&access_type=offline"
                + "&prompt=select_account";

        // Capture du code via serveur local
        String code = waitForCallback("/callback/google", state, authUrl);

        // Échange code → access_token
        String tokenBody = "code="          + URLEncoder.encode(code,                StandardCharsets.UTF_8)
                + "&client_id="     + URLEncoder.encode(googleClientId,      StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(googleClientSecret,  StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(googleRedirectUri,   StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpRequest tokenReq = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

        HttpResponse<String> tokenRes = HttpClient.newHttpClient()
                .send(tokenReq, HttpResponse.BodyHandlers.ofString());

        JSONObject tokenJson  = new JSONObject(tokenRes.body());
        String     accessToken = tokenJson.optString("access_token", "");

        if (accessToken.isBlank()) {
            throw new RuntimeException("Échec échange token Google : " + tokenRes.body());
        }

        // Récupération info utilisateur
        HttpRequest userReq = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userRes = HttpClient.newHttpClient()
                .send(userReq, HttpResponse.BodyHandlers.ofString());

        JSONObject userJson = new JSONObject(userRes.body());

        OAuthUserInfo info = new OAuthUserInfo();
        info.provider   = "google";
        info.providerId = userJson.optString("sub",     "");
        info.email      = userJson.optString("email",   "");
        info.name       = userJson.optString("name",    "");
        info.picture    = userJson.optString("picture", "");

        if (info.email.isBlank()) {
            throw new RuntimeException("Email non récupéré depuis Google : " + userRes.body());
        }

        System.out.println("✅ Google OAuth → " + info.email + " (" + info.name + ")");
        return info;
    }

    // ============================================================
    //                         FACEBOOK
    // ============================================================

    /** Lance le flow OAuth Facebook complet. Bloque jusqu'à la fin (timeout 2 min). */
    public OAuthUserInfo loginWithFacebook() throws Exception {
        if (facebookClientId == null || facebookClientId.isBlank()) {
            throw new IllegalStateException("oauth.facebook.client.id non configuré dans config.properties");
        }

        String state = UUID.randomUUID().toString();
        String scope = "email,public_profile";

        String authUrl = FACEBOOK_AUTH_URL
                + "?client_id="     + URLEncoder.encode(facebookClientId,    StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode(scope,               StandardCharsets.UTF_8)
                + "&state="         + state;

        String code = waitForCallback("/callback/facebook", state, authUrl);

        // Échange code → access_token (Facebook : GET avec query params)
        String tokenUrl = FACEBOOK_TOKEN_URL
                + "?client_id="     + URLEncoder.encode(facebookClientId,     StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(facebookClientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri="  + URLEncoder.encode(facebookRedirectUri,  StandardCharsets.UTF_8)
                + "&code="          + URLEncoder.encode(code,                 StandardCharsets.UTF_8);

        HttpRequest tokenReq = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .GET()
                .build();

        HttpResponse<String> tokenRes = HttpClient.newHttpClient()
                .send(tokenReq, HttpResponse.BodyHandlers.ofString());

        JSONObject tokenJson  = new JSONObject(tokenRes.body());
        String     accessToken = tokenJson.optString("access_token", "");

        if (accessToken.isBlank()) {
            throw new RuntimeException("Échec échange token Facebook : " + tokenRes.body());
        }

        // Récupération info utilisateur
        String userUrl = FACEBOOK_USERINFO_URL + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        HttpRequest userReq = HttpRequest.newBuilder()
                .uri(URI.create(userUrl))
                .GET()
                .build();

        HttpResponse<String> userRes = HttpClient.newHttpClient()
                .send(userReq, HttpResponse.BodyHandlers.ofString());

        JSONObject userJson = new JSONObject(userRes.body());

        OAuthUserInfo info = new OAuthUserInfo();
        info.provider   = "facebook";
        info.providerId = userJson.optString("id",    "");
        info.email      = userJson.optString("email", "");
        info.name       = userJson.optString("name",  "");

        // Picture Facebook : objet imbriqué picture.data.url
        JSONObject picObj = userJson.optJSONObject("picture");
        if (picObj != null) {
            JSONObject picData = picObj.optJSONObject("data");
            if (picData != null) info.picture = picData.optString("url", "");
        }

        if (info.email.isBlank()) {
            throw new RuntimeException(
                    "Email non récupéré depuis Facebook (l'utilisateur n'a peut-être pas autorisé l'email) : "
                            + userRes.body());
        }

        System.out.println("✅ Facebook OAuth → " + info.email + " (" + info.name + ")");
        return info;
    }

    // ============================================================
    //                    SERVEUR LOCAL (CALLBACK)
    // ============================================================

    /**
     * Démarre un serveur HTTP local sur localhost:8765, ouvre l'URL d'autorisation
     * dans le navigateur système, et attend que le provider redirige vers le callback.
     * Retourne le code d'autorisation reçu.
     */
    private String waitForCallback(String contextPath, String expectedState, String authUrl) throws Exception {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(LOCAL_PORT), 0);
        server.createContext(contextPath, exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code  = parseQueryParam(query, "code");
            String state = parseQueryParam(query, "state");
            String error = parseQueryParam(query, "error");

            String response;
            int    httpCode;

            if (error != null) {
                httpCode = 200;
                response = htmlPage("❌ Erreur OAuth", "#e74c3c",
                        "Le provider a refusé : " + error
                                + "<br><br>Vous pouvez fermer cet onglet.");
                codeFuture.completeExceptionally(new RuntimeException("OAuth error: " + error));
            } else if (code == null || code.isBlank()) {
                httpCode = 400;
                response = htmlPage("❌ Code manquant", "#e74c3c",
                        "Le code d'autorisation n'a pas été reçu.<br>Vous pouvez fermer cet onglet.");
                codeFuture.completeExceptionally(new RuntimeException("Code manquant"));
            } else if (!expectedState.equals(state)) {
                httpCode = 400;
                response = htmlPage("❌ State invalide", "#e74c3c",
                        "Possible attaque CSRF détectée.<br>Vous pouvez fermer cet onglet.");
                codeFuture.completeExceptionally(new RuntimeException("State mismatch"));
            } else {
                httpCode = 200;
                response = htmlPage("✅ Connexion réussie", "#27ae60",
                        "Vous pouvez fermer cet onglet et retourner dans l'application RentAll.");
                codeFuture.complete(code);
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(httpCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        System.out.println("🌐 Serveur callback OAuth démarré sur http://localhost:" + LOCAL_PORT + contextPath);

        try {
            // Ouvrir le navigateur
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(authUrl));
                System.out.println("🌍 Navigateur ouvert sur l'URL d'autorisation");
            } else {
                System.out.println("⚠️ Desktop.browse() non supporté.");
                System.out.println("➡️  Ouvrez manuellement cette URL dans votre navigateur :");
                System.out.println(authUrl);
            }

            // Attente du callback (timeout 2 min)
            return codeFuture.get(120, TimeUnit.SECONDS);

        } finally {
            // Petit délai pour que la page de succès s'affiche avant de couper
            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                server.stop(0);
                System.out.println("🛑 Serveur callback OAuth arrêté");
            }).start();
        }
    }

    private static String parseQueryParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String htmlPage(String title, String color, String message) {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='utf-8'>"
                + "<title>" + title + "</title>"
                + "<style>"
                + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "background:#f5f7fa;margin:0;display:flex;align-items:center;justify-content:center;height:100vh;}"
                + ".card{background:white;border-radius:12px;padding:40px 60px;text-align:center;"
                + "box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:480px;}"
                + "h1{color:" + color + ";margin:0 0 16px;font-size:28px;}"
                + "p{color:#566573;font-size:15px;line-height:1.5;margin:0;}"
                + ".brand{margin-top:24px;color:#95a5a6;font-size:12px;}"
                + "</style></head><body>"
                + "<div class='card'><h1>" + title + "</h1><p>" + message + "</p>"
                + "<div class='brand'>RentAll · OAuth</div></div>"
                + "</body></html>";
    }
}
