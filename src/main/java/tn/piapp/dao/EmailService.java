package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

/**
 * Envoi d'emails via Gmail SMTP.
 * Configurez d'abord smtp.username / smtp.password dans config.properties
 * (utilisez un App Password Google, pas votre mot de passe principal).
 */
public class EmailService {

    private final String host     = AppConfig.get("smtp.host", "smtp.gmail.com");
    private final String port     = AppConfig.get("smtp.port", "587");
    private final String user     = AppConfig.get("smtp.username");
    private final String password = AppConfig.get("smtp.password");
    private final String fromName = AppConfig.get("smtp.from.name", "RentAll");

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    /** Envoi d'un email HTML. */
    public boolean send(String to, String subject, String htmlBody) {
        if (user == null || user.isBlank()) {
            System.err.println("❌ smtp.username vide dans config.properties");
            return false;
        }
        try {
            MimeMessage msg = new MimeMessage(buildSession());
            msg.setFrom(new InternetAddress(user, fromName));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject, "UTF-8");
            msg.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(msg);
            System.out.println("📧 Email envoyé → " + to);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    // =================== Templates prêts à l'emploi ===================

    public boolean sendConfirmation(String to, String username, String token) {
        String link = AppConfig.get("app.base.url", "http://localhost:8080")
                    + "/verify?token=" + token;
        String html = ""
            + "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:30px'>"
            + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:12px;padding:32px;"
            + "             box-shadow:0 4px 14px rgba(0,0,0,.08)'>"
            + "  <h2 style='color:#6C63FF;margin-top:0'>🏠 Bienvenue sur RentAll</h2>"
            + "  <p>Bonjour <strong>" + escape(username) + "</strong>,</p>"
            + "  <p>Merci de votre inscription. Pour activer votre compte, cliquez sur le bouton ci-dessous :</p>"
            + "  <p style='text-align:center;margin:24px 0'>"
            + "    <a href='" + link + "' style='background:#6C63FF;color:#fff;text-decoration:none;"
            + "       padding:12px 28px;border-radius:6px;display:inline-block;font-weight:bold'>"
            + "       Confirmer mon compte</a>"
            + "  </p>"
            + "  <p style='font-size:12px;color:#888'>Si le bouton ne fonctionne pas, copiez ce lien :<br><code>"
            + link + "</code></p>"
            + "  <p style='font-size:12px;color:#888'>Ce lien expire dans 24h.</p>"
            + "</div></body></html>";
        return send(to, "Confirmez votre compte RentAll", html);
    }

    public boolean sendPasswordResetInfo(String to, String username) {
        String html = "<p>Bonjour " + escape(username) + ",</p>"
                + "<p>Une demande de réinitialisation de votre mot de passe a été effectuée. "
                + "Un code à 6 chiffres vient de vous être envoyé par WhatsApp.</p>"
                + "<p>Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.</p>";
        return send(to, "🔐 Demande de réinitialisation", html);
    }

    /**
     * Email de bienvenue après inscription OAuth (Google/Facebook).
     * Pas de lien de confirmation : le compte est déjà actif.
     */
    public boolean sendOAuthWelcome(String to, String username, String provider) {
        String providerLabel = "Google".equalsIgnoreCase(provider) ? "Google" : capitalize(provider);
        String providerColor = "Google".equalsIgnoreCase(provider) ? "#4285F4" : "#1877F2";

        String html = ""
            + "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:30px'>"
            + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:12px;padding:32px;"
            + "             box-shadow:0 4px 14px rgba(0,0,0,.08)'>"
            + "  <h2 style='color:#6C63FF;margin-top:0'>🎉 Bienvenue sur RentAll, " + escape(username) + " !</h2>"
            + "  <p>Votre compte a été créé avec succès via "
            + "    <strong style='color:" + providerColor + "'>" + providerLabel + "</strong>."
            + "  </p>"
            + "  <p>Vous pouvez dès maintenant :</p>"
            + "  <ul style='line-height:1.8'>"
            + "    <li>🔍 Rechercher des logements partout dans le monde</li>"
            + "    <li>📅 Réserver vos séjours en quelques clics</li>"
            + "    <li>🏠 Devenir Host pour proposer vos propres logements</li>"
            + "  </ul>"
            + "  <div style='background:#f0f3ff;border-left:4px solid #6C63FF;padding:14px;margin:20px 0;border-radius:6px'>"
            + "    <p style='margin:0;font-size:13px;color:#5a4fcf'>"
            + "      💡 <strong>Astuce</strong> : pour pouvoir vous reconnecter aussi avec un mot de passe, "
            + "      utilisez la fonctionnalité <em>« Mot de passe oublié »</em> sur l'écran de connexion."
            + "    </p>"
            + "  </div>"
            + "  <p style='font-size:12px;color:#888;margin-top:24px'>"
            + "    Vous recevez cet email parce que vous venez de créer un compte sur RentAll. "
            + "    Si ce n'est pas vous, contactez-nous immédiatement."
            + "  </p>"
            + "</div></body></html>";

        return send(to, "🎉 Bienvenue sur RentAll", html);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
