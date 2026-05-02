package org.example.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Google reCAPTCHA v3 - verification cote serveur.
 */
public class CaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double DEFAULT_SCORE = 0.5;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_ACTION = "register";
    private static final String DEFAULT_VERSION = "v3";
    private static final String GOOGLE_TEST_SITE_KEY_V2 = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
    private static final String GOOGLE_TEST_SITE_KEY_V3 = "6LeIxAcTAAAAANo4n7hR6U7sQX6P5gG2w2i6R4Y";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build();

    private final String siteKey;
    private final String secretKey;
    private final double minScore;
    private final int timeoutSeconds;
    private final String expectedAction;
    private final String expectedHostname;
    private final String recaptchaVersion;
    private final boolean allowGoogleTestHostname;

    public CaptchaService() {
        this.siteKey = trim(AppConfig.get("recaptcha.site.key"));
        this.secretKey = trim(AppConfig.get("recaptcha.secret.key"));
        this.minScore = readDouble("recaptcha.min.score", DEFAULT_SCORE);
        this.timeoutSeconds = Math.max(3, (int) readDouble("recaptcha.timeout.seconds", DEFAULT_TIMEOUT_SECONDS));

        String configuredAction = trim(AppConfig.get("recaptcha.expected.action"));
        this.expectedAction = (configuredAction == null || configuredAction.isBlank()) ? DEFAULT_ACTION : configuredAction;

        String configuredHost = trim(AppConfig.get("recaptcha.expected.hostname"));
        this.expectedHostname = (configuredHost == null || configuredHost.isBlank()) ? "localhost" : configuredHost;
        String configuredVersion = trim(AppConfig.get("recaptcha.version"));
        this.recaptchaVersion = (configuredVersion == null || configuredVersion.isBlank())
                ? DEFAULT_VERSION
                : configuredVersion.toLowerCase(Locale.ROOT);
        this.allowGoogleTestHostname = isGoogleTestSiteKey(siteKey);
    }

    public String getSiteKey() {
        return siteKey;
    }

    public double getMinScore() {
        return minScore;
    }

    /**
     * Verifie le token reCAPTCHA v3 cote serveur.
     */
    public VerifyResult verify(String token) {
        if (token == null || token.isBlank()) {
            return VerifyResult.fail("Token vide ou null.");
        }

        if (secretKey == null || secretKey.isBlank()) {
            return VerifyResult.fail("recaptcha.secret.key non configure dans config.properties.");
        }

        try {
            StringBuilder body = new StringBuilder();
            body.append("secret=").append(URLEncoder.encode(secretKey, StandardCharsets.UTF_8));
            body.append("&response=").append(URLEncoder.encode(token.trim(), StandardCharsets.UTF_8));

            String remoteIp = resolveLocalIp();
            if (remoteIp != null) {
                body.append("&remoteip=").append(URLEncoder.encode(remoteIp, StandardCharsets.UTF_8));
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return VerifyResult.fail("Erreur API reCAPTCHA (HTTP " + res.statusCode() + ").");
            }

            JSONObject json = new JSONObject(res.body());
            boolean success = json.optBoolean("success", false);
            if (!success) {
                List<String> errorCodes = parseErrorCodes(json.optJSONArray("error-codes"));
                String reason = resolveErrorCodes(errorCodes);
                System.err.println("reCAPTCHA refuse: " + reason + " | raw=" + res.body());
                return VerifyResult.fail(reason);
            }

            String hostname = json.optString("hostname", "");
            boolean hostnameOk = isHostnameAllowed(hostname, expectedHostname)
                    || (allowGoogleTestHostname && "testkey.google.com".equalsIgnoreCase(hostname));
            if (!hostnameOk) {
                return VerifyResult.fail("Hostname reCAPTCHA non autorise: " + hostname);
            }

            if ("v2".equals(recaptchaVersion)) {
                return VerifyResult.ok(1.0);
            }

            double score = json.has("score") ? json.optDouble("score", -1.0) : 1.0;
            String action = json.optString("action", "");
            if (!expectedAction.equalsIgnoreCase(action)) {
                return VerifyResult.fail("Action invalide: recu='" + action + "', attendu='" + expectedAction + "'.");
            }

            if (score < minScore) {
                String msg = String.format(Locale.US,
                        "Score trop bas (%.2f < %.2f). Activite suspecte detectee.", score, minScore);
                return VerifyResult.lowScore(score, msg);
            }

            return VerifyResult.ok(score);
        } catch (Exception e) {
            System.err.println("Erreur verification reCAPTCHA: " + e.getMessage());
            return VerifyResult.fail("Erreur reseau/API: " + e.getMessage());
        }
    }

    private static List<String> parseErrorCodes(JSONArray array) {
        List<String> codes = new ArrayList<>();
        if (array == null) {
            return codes;
        }
        for (int i = 0; i < array.length(); i++) {
            String code = array.optString(i, "");
            if (!code.isBlank()) {
                codes.add(code);
            }
        }
        return codes;
    }

    private static String resolveErrorCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "Erreur inconnue";
        }
        if (codes.contains("timeout-or-duplicate")) {
            return "Token expire (>2 min) ou deja utilise. Reessayez.";
        }
        if (codes.contains("invalid-input-secret")) {
            return "Cle secrete invalide dans config.properties.";
        }
        if (codes.contains("invalid-input-response")) {
            return "Token reCAPTCHA invalide ou mal forme.";
        }
        if (codes.contains("bad-request")) {
            return "Requete mal formee envoyee a Google.";
        }
        if (codes.contains("missing-input-secret")) {
            return "Cle secrete manquante.";
        }
        if (codes.contains("missing-input-response")) {
            return "Token manquant dans la requete.";
        }
        return "Erreur Google reCAPTCHA : " + String.join(",", codes);
    }

    private static boolean isHostnameAllowed(String actualHostname, String configuredHostname) {
        if (actualHostname == null || actualHostname.isBlank()) {
            return false;
        }
        String actual = actualHostname.trim().toLowerCase(Locale.ROOT);
        String expected = configuredHostname == null ? "" : configuredHostname.trim().toLowerCase(Locale.ROOT);
        if (expected.isBlank()) {
            return true;
        }
        return actual.equals(expected)
                || ("localhost".equals(expected) && (actual.equals("127.0.0.1") || actual.equals("localhost")));
    }

    private static boolean isGoogleTestSiteKey(String key) {
        if (key == null) return false;
        String k = key.trim();
        return GOOGLE_TEST_SITE_KEY_V2.equals(k) || GOOGLE_TEST_SITE_KEY_V3.equals(k);
    }

    private static String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }

    private static double readDouble(String key, double fallback) {
        try {
            String raw = AppConfig.get(key);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return Double.parseDouble(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static final class VerifyResult {
        public final boolean success;
        public final double score;
        public final String errorMessage;

        private VerifyResult(boolean success, double score, String errorMessage) {
            this.success = success;
            this.score = score;
            this.errorMessage = errorMessage;
        }

        static VerifyResult ok(double score) {
            return new VerifyResult(true, score, null);
        }

        static VerifyResult lowScore(double score, String msg) {
            return new VerifyResult(false, score, msg);
        }

        static VerifyResult fail(String msg) {
            return new VerifyResult(false, -1.0, msg);
        }

        @Override
        public String toString() {
            return "VerifyResult{success=" + success + ", score=" + score + ", error=" + errorMessage + "}";
        }
    }
}
