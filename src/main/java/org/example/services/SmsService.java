package org.example.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.security.SecureRandom;

/**
 * Envoi de messages WhatsApp via Twilio.
 * Configurez twilio.account.sid / twilio.auth.token / twilio.whatsapp.from
 * (ou twilio.from.number) dans config.properties.
 */
public class SmsService {

    private static boolean initialized = false;

    private final String accountSid = AppConfig.get("twilio.account.sid");
    private final String authToken  = AppConfig.get("twilio.auth.token");
    private final String whatsappFrom = AppConfig.get("twilio.whatsapp.from");
    private final String fromNumber   = AppConfig.get("twilio.from.number");

    /** True si Twilio est correctement configuré (sinon on fallback en mode dev console). */
    private boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && !accountSid.startsWith("ACxxxx")
                && authToken != null && !authToken.isBlank()
                && !authToken.equals("your_auth_token_here")
                && resolveFromAddress() != null;
    }

    private synchronized void ensureInit() {
        if (!initialized && isConfigured()) {
            Twilio.init(accountSid, authToken);
            initialized = true;
            System.out.println("✅ Twilio initialisé");
        }
    }

    private String resolveFromAddress() {
        if (whatsappFrom != null && !whatsappFrom.isBlank()) {
            return normalizeWhatsappAddress(whatsappFrom);
        }
        if (fromNumber != null && !fromNumber.isBlank() && !fromNumber.equals("+15551234567")) {
            return normalizeWhatsappAddress(fromNumber);
        }
        return null;
    }

    private String normalizeWhatsappAddress(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) return null;
        return v.startsWith("whatsapp:") ? v : ("whatsapp:" + v);
    }

    /** Envoie un message WhatsApp. Le numéro doit être au format international : +216XXXXXXXX. */
    public boolean send(String toPhone, String text) {
        // Mode DEV : Twilio non configuré → on affiche le message dans la console
        if (!isConfigured()) {
            System.out.println("┌──────────────────────────────────────────────────");
            System.out.println("│ 💬 [MODE DEV - WhatsApp simulé, Twilio non configuré]");
            System.out.println("│ Destinataire : " + toPhone);
            System.out.println("│ Message      : " + text);
            System.out.println("└──────────────────────────────────────────────────");
            return true; // on considère que l'envoi a "réussi"
        }

        ensureInit();
        try {
            String toAddress = normalizeWhatsappAddress(toPhone);
            String fromAddress = resolveFromAddress();
            Message msg = Message.creator(
                    new PhoneNumber(toAddress),
                    new PhoneNumber(fromAddress),
                    text
            ).create();
            System.out.println("💬 WhatsApp envoyé → " + toPhone + " (SID " + msg.getSid() + ")");
            return true;
        } catch (Exception e) {
            // ÉCHEC Twilio → fallback mode DEV : on affiche le message dans la console
            // (typiquement : compte d'essai Twilio = numéros destinataires non vérifiés,
            // région Tunisie non activée, quota dépassé, etc.)
            System.err.println("⚠️  Twilio a refusé l'envoi : " + e.getMessage());
            System.out.println("┌──────────────────────────────────────────────────");
            System.out.println("│ 💬 [FALLBACK DEV - WhatsApp affiché ici car Twilio a échoué]");
            System.out.println("│ Destinataire : " + toPhone);
            System.out.println("│ Message      : " + text);
            System.out.println("│");
            System.out.println("│ 💡 Pour que Twilio marche réellement :");
            System.out.println("│   1. Activez WhatsApp dans Twilio (Sandbox ou numéro business)");
            System.out.println("│   2. Ajoutez et vérifiez " + toPhone);
            System.out.println("│   3. (ou passez en compte payant)");
            System.out.println("└──────────────────────────────────────────────────");
            return false; // échec réel: ne pas afficher "code envoyé" dans l'UI
        }
    }

    /** Génère un code aléatoire à 6 chiffres. */
    public static String generateCode() {
        SecureRandom rnd = new SecureRandom();
        return String.format("%06d", rnd.nextInt(1_000_000));
    }

    /** Envoi d'un code de reset avec texte prédéfini. */
    public boolean sendResetCode(String phone, String code) {
        String text = "RentAll : votre code de réinitialisation est " + code
                    + ". Il expire dans 10 minutes. Ne le partagez avec personne.";
        return send(phone, text);
    }
}
