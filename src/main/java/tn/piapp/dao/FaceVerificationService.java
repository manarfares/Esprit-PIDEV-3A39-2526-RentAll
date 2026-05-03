package org.example.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper Java autour des scripts Python du projet Symfony :
 *   - bin/face_verify.py     → compare 2 images (selfie ↔ CIN)
 *   - bin/face_identify.py   → identifie un user dans une liste de candidats
 *
 * Ce service appelle Python via ProcessBuilder, lit le JSON de stdout,
 * et renvoie un objet structuré.
 *
 * Configuration dans config.properties :
 *   face.symfony.dir       (chemin du projet Symfony)
 *   face.python.exe        (ex : "py")
 *   face.python.arg        (ex : "-3.10", peut être vide)
 *   face.match.threshold   (ex : 0.66)
 *   face.timeout           (en secondes)
 */
public class FaceVerificationService {

    private final String symfonyDir = AppConfig.get("face.symfony.dir");
    private final String pythonExe  = AppConfig.get("face.python.exe", "py");
    private final String pythonArg  = AppConfig.get("face.python.arg", "");
    private final float  threshold  = parseFloat(AppConfig.get("face.match.threshold", "0.66"), 0.66f);
    private final int    timeoutSec = AppConfig.getInt("face.timeout", 25);
    private final String effectiveSymfonyDir = resolveSymfonyProjectDir(symfonyDir);

    // ============================================================
    //                    Résultats structurés
    // ============================================================

    public static class VerifyResult {
        public boolean success;
        public boolean match;
        public Float   distance;
        public String  error;
    }

    public static class IdentifyResult {
        public boolean success;
        public boolean matched;
        public Integer userId;
        public Float   distance;
        public String  error;
    }

    // ============================================================
    //              VERIFY : comparer 2 images (selfie ↔ CIN)
    // ============================================================

    public VerifyResult verifyFaces(File selfie, File idCard) {
        VerifyResult res = new VerifyResult();

        if (!isUsable(effectiveSymfonyDir)) {
            res.error = "face.symfony.dir non configuré dans config.properties";
            return res;
        }
        Path script = Paths.get(effectiveSymfonyDir, "bin", "face_verify.py");
        if (!Files.isRegularFile(script)) {
            res.error = "Script introuvable : " + script;
            return res;
        }
        if (selfie == null || !selfie.isFile()) { res.error = "Selfie introuvable.";  return res; }
        if (idCard == null || !idCard.isFile()) { res.error = "Photo CIN introuvable.";  return res; }

        List<String> cmd = buildPythonCmd(script.toString(),
                selfie.getAbsolutePath(),
                idCard.getAbsolutePath());

        JSONObject json = runScript(cmd);
        if (json == null) {
            res.error = "Aucune réponse du moteur facial.";
            return res;
        }

        res.success  = json.optBoolean("success", false);
        res.match    = json.optBoolean("match",   false);
        res.distance = json.has("distance") && !json.isNull("distance")
                ? (float) json.optDouble("distance", -1.0) : null;
        res.error    = json.has("error") && !json.isNull("error") ? json.optString("error") : null;

        return res;
    }

    // ============================================================
    //   IDENTIFY : trouver le bon user parmi N candidats (login facial)
    // ============================================================

    /**
     * @param probe       photo live (webcam ou upload)
     * @param candidates  liste des candidats (id user + chemin absolu vers son selfie de référence)
     */
    public IdentifyResult identifyUser(File probe, List<FaceCandidate> candidates) {
        IdentifyResult res = new IdentifyResult();

        if (!isUsable(effectiveSymfonyDir)) {
            res.error = "face.symfony.dir non configuré dans config.properties";
            return res;
        }
        Path script = Paths.get(effectiveSymfonyDir, "bin", "face_identify.py");
        if (!Files.isRegularFile(script)) {
            res.error = "Script introuvable : " + script;
            return res;
        }
        if (probe == null || !probe.isFile()) { res.error = "Photo live introuvable."; return res; }
        if (candidates == null || candidates.isEmpty()) {
            res.error = "Aucun compte éligible à la connexion faciale.";
            return res;
        }

        // Filtrer les candidats dont le fichier référence existe vraiment
        List<FaceCandidate> usable = new ArrayList<>();
        for (FaceCandidate c : candidates) {
            if (c.fullPath != null && new File(c.fullPath).isFile()) usable.add(c);
        }
        if (usable.isEmpty()) {
            res.error = "Aucune photo de référence accessible sur le disque.";
            return res;
        }

        // Sérialiser les candidats en JSON pour le script
        Path manifestPath;
        try {
            Path tempDir = Paths.get(effectiveSymfonyDir, "var", "face-login");
            if (!Files.isDirectory(tempDir)) Files.createDirectories(tempDir);
            manifestPath = tempDir.resolve("face-candidates-" + UUID.randomUUID() + ".json");

            JSONArray arr = new JSONArray();
            for (FaceCandidate c : usable) {
                JSONObject o = new JSONObject();
                o.put("id",   c.id);
                o.put("path", c.fullPath);
                arr.put(o);
            }
            Files.writeString(manifestPath, arr.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            res.error = "Impossible d'écrire le manifest : " + e.getMessage();
            return res;
        }

        Path cachePath = Paths.get(effectiveSymfonyDir, "var", "face-login", "face-identify-cache.json");

        List<String> cmd = buildPythonCmd(script.toString(),
                probe.getAbsolutePath(),
                manifestPath.toString(),
                String.valueOf(threshold),
                cachePath.toString());

        JSONObject json = runScript(cmd);
        try { Files.deleteIfExists(manifestPath); } catch (IOException ignored) {}

        if (json == null) {
            res.error = "Aucune réponse du moteur facial.";
            return res;
        }

        res.success  = json.optBoolean("success", false);
        res.matched  = json.optBoolean("matched", false);
        res.userId   = json.has("user_id")  && !json.isNull("user_id")  ? json.optInt("user_id")          : null;
        res.distance = json.has("distance") && !json.isNull("distance") ? (float) json.optDouble("distance", -1.0) : null;
        res.error    = json.has("error")    && !json.isNull("error")    ? json.optString("error") : null;

        return res;
    }

    // ============================================================
    //                       Internals
    // ============================================================

    private List<String> buildPythonCmd(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonExe);
        if (pythonArg != null && !pythonArg.isBlank()) cmd.add(pythonArg);
        for (String a : args) cmd.add(a);
        return cmd;
    }

    private JSONObject runScript(List<String> cmd) {
        try {
            System.out.println("🐍 face cmd : " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(effectiveSymfonyDir));
            pb.redirectErrorStream(false);

            // Variables d'environnement pour le script
            pb.environment().put("FACE_MATCH_THRESHOLD",  String.valueOf(threshold));
            pb.environment().put("FACE_LOGIN_THRESHOLD",  String.valueOf(threshold));

            Process proc = pb.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            // Lecture asynchrone de stderr pour ne pas bloquer
            Thread errReader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) stderr.append(line).append('\n');
                } catch (IOException ignored) {}
            });
            errReader.setDaemon(true);
            errReader.start();

            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) stdout.append(line).append('\n');
            }

            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                System.err.println("⏱ Timeout script facial après " + timeoutSec + "s");
                return null;
            }

            errReader.join(500);
            String out = stdout.toString().trim();

            if (out.isEmpty()) {
                System.err.println("⚠️ Script facial : stdout vide. stderr=\n" + stderr);
                return null;
            }

            try {
                return new JSONObject(out);
            } catch (Exception parseEx) {
                System.err.println("⚠️ Réponse non-JSON : " + out);
                System.err.println("⚠️ stderr : " + stderr);
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur exécution script facial : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static boolean isUsable(String s) { return s != null && !s.isBlank(); }

    private static float parseFloat(String s, float defaultVal) {
        try { return Float.parseFloat(s); } catch (Exception e) { return defaultVal; }
    }

    /**
     * Conversion d'un chemin selfieImage stocké en DB (ex : "/uploads/verification/selfies/x.jpg")
     * en chemin absolu sur disque, en utilisant face.symfony.dir/public/...
     */
    public String resolveAbsolutePath(String selfieImageColumn) {
        if (selfieImageColumn == null || selfieImageColumn.isBlank()) return null;
        String rel = selfieImageColumn.replace('\\', '/').trim();
        if (!rel.startsWith("/")) rel = "/" + rel;
        if (!isUsable(effectiveSymfonyDir)) return null;
        return Paths.get(effectiveSymfonyDir, "public" + rel).toString();
    }

    /** Utilitaire DTO pour passer un candidat à identifyUser(). */
    public static class FaceCandidate {
        public final int    id;
        public final String fullPath;
        public FaceCandidate(int id, String fullPath) { this.id = id; this.fullPath = fullPath; }
    }

    /**
     * Accepte soit la vraie racine Symfony, soit un dossier parent qui contient un unique sous-dossier projet.
     */
    private static String resolveSymfonyProjectDir(String configuredPath) {
        if (!isUsable(configuredPath)) return configuredPath;
        Path base = Paths.get(configuredPath);
        if (isSymfonyRoot(base)) return base.toString();

        // Recherche peu profonde: utile pour "...\pidev-user (2)\pidev-user\pidev-user"
        try {
            for (int depth = 0; depth < 2; depth++) {
                File dir = depth == 0 ? base.toFile() : null;
                if (depth == 1) {
                    // scan one level deeper from each child
                    File[] children = base.toFile().listFiles(File::isDirectory);
                    if (children == null) break;
                    for (File child : children) {
                        if (isSymfonyRoot(child.toPath())) return child.getAbsolutePath();
                        File[] grandChildren = child.listFiles(File::isDirectory);
                        if (grandChildren == null) continue;
                        for (File gc : grandChildren) {
                            if (isSymfonyRoot(gc.toPath())) return gc.getAbsolutePath();
                        }
                    }
                } else if (dir != null) {
                    File[] children = dir.listFiles(File::isDirectory);
                    if (children == null) break;
                    for (File child : children) {
                        if (isSymfonyRoot(child.toPath())) return child.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {}

        return configuredPath;
    }

    private static boolean isSymfonyRoot(Path root) {
        return root != null
                && Files.isDirectory(root)
                && Files.isRegularFile(root.resolve("bin").resolve("face_verify.py"))
                && Files.isRegularFile(root.resolve("bin").resolve("face_identify.py"))
                && Files.isDirectory(root.resolve("public"));
    }
}
