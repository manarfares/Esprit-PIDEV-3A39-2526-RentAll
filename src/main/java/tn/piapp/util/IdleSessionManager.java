package org.example.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.models.User;
import org.example.services.ServiceUser;

import java.util.Optional;

/**
 * Détection d'inactivité + déconnexion automatique.
 *
 * Comportement :
 *  - Tout mouvement de souris, clic ou touche clavier dans la fenêtre relance le timer.
 *  - Après {@link #IDLE_TIMEOUT} sans activité, un popup demande à l'utilisateur s'il
 *    est encore là.
 *  - S'il clique "Je suis là" → le timer redémarre.
 *  - S'il clique "Se déconnecter" OU n'agit pas pendant {@link #POPUP_TIMEOUT} →
 *    le user est déconnecté (session_token vidé en base) et redirigé vers /login.fxml.
 *
 * Usage : appelez {@link #attachToScene(Scene, User)} dans le setCurrentUser de chaque
 * controller post-login (HomeController, AdminController, etc.).
 */
public class IdleSessionManager {

    /** Durée d'inactivité avant le popup d'avertissement. */
    private static final Duration IDLE_TIMEOUT  = Duration.seconds(60); // 1 min

    /** Durée d'attente du clic sur "Je suis là" avant déconnexion auto. */
    private static final Duration POPUP_TIMEOUT = Duration.seconds(30); // 30 sec

    /** Une seule instance active à la fois (remplacée à chaque changement de scene). */
    private static IdleSessionManager current;

    private final User             user;
    private final ServiceUser      service = new ServiceUser();
    private       Scene            scene;
    private       PauseTransition  idleTimer;
    private       PauseTransition  popupCountdown;
    private       Alert            popupAlert;
    private       boolean          showingPopup    = false;
    private       boolean          alreadyLoggedOut = false;

    private IdleSessionManager(User user) {
        this.user = user;
    }

    /**
     * Active la détection d'inactivité sur le scene fourni pour l'utilisateur donné.
     * Détache automatiquement toute instance précédente.
     */
    public static synchronized void attachToScene(Scene scene, User user) {
        if (current != null) current.detach();
        current = new IdleSessionManager(user);
        current.attach(scene);
    }

    /** Désactive la détection (à appeler avant un logout volontaire). */
    public static synchronized void detachCurrent() {
        if (current != null) {
            current.detach();
            current = null;
        }
    }

    private void attach(Scene scene) {
        this.scene = scene;

        // Tout signe d'activité relance le timer
        scene.addEventFilter(MouseEvent.MOUSE_MOVED,    e -> resetIdle());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED,  e -> resetIdle());
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED,  e -> resetIdle());
        scene.addEventFilter(ScrollEvent.SCROLL,        e -> resetIdle());
        scene.addEventFilter(KeyEvent.KEY_PRESSED,      e -> resetIdle());

        idleTimer = new PauseTransition(IDLE_TIMEOUT);
        idleTimer.setOnFinished(e -> showIdlePopup());
        idleTimer.playFromStart();

        System.out.println("🕒 IdleSessionManager actif → user=" + user.getUsername()
                + ", timeout=" + (long) IDLE_TIMEOUT.toSeconds() + "s, popup="
                + (long) POPUP_TIMEOUT.toSeconds() + "s");
    }

    private void resetIdle() {
        if (showingPopup || alreadyLoggedOut) return;
        if (idleTimer != null) idleTimer.playFromStart();
    }

    private void showIdlePopup() {
        if (showingPopup || alreadyLoggedOut) return;
        showingPopup = true;

        Platform.runLater(() -> {
            popupAlert = new Alert(AlertType.CONFIRMATION);
            popupAlert.setTitle("Êtes-vous toujours là ?");
            popupAlert.setHeaderText("⏱️  Inactivité détectée");
            popupAlert.setContentText(
                    "Vous n'avez pas interagi avec l'application depuis "
                            + (long) IDLE_TIMEOUT.toSeconds() + " secondes.\n\n"
                            + "Cliquez sur \"Je suis là\" pour continuer.\n"
                            + "Sans réponse, vous serez automatiquement déconnecté dans "
                            + (long) POPUP_TIMEOUT.toSeconds() + " secondes.");

            ButtonType iAmHere   = new ButtonType("✅  Je suis là");
            ButtonType logoutBtn = new ButtonType("🚪  Se déconnecter");
            popupAlert.getButtonTypes().setAll(iAmHere, logoutBtn);

            // Toujours au-dessus
            Stage popupStage = (Stage) popupAlert.getDialogPane().getScene().getWindow();
            popupStage.setAlwaysOnTop(true);

            // Compte à rebours auto-logout
            popupCountdown = new PauseTransition(POPUP_TIMEOUT);
            popupCountdown.setOnFinished(ev -> {
                System.out.println("⏱️  Délai dépassé sans réponse → déconnexion auto");
                if (popupAlert != null && popupAlert.isShowing()) {
                    popupAlert.setResult(logoutBtn);
                    popupAlert.close();
                }
            });
            popupCountdown.playFromStart();

            Optional<ButtonType> result = popupAlert.showAndWait();
            popupCountdown.stop();

            if (result.isPresent() && result.get() == iAmHere) {
                System.out.println("✅ Utilisateur a confirmé sa présence → reset");
                showingPopup = false;
                if (idleTimer != null) idleTimer.playFromStart();
            } else {
                doLogout();
            }
        });
    }

    private void doLogout() {
        if (alreadyLoggedOut) return;
        alreadyLoggedOut = true;
        showingPopup = false;

        if (idleTimer      != null) idleTimer.stop();
        if (popupCountdown != null) popupCountdown.stop();

        Platform.runLater(() -> {
            try {
                System.out.println("🚪 Déconnexion auto → " + user.getUsername());
                service.logout(user.getId());

                if (scene == null || scene.getWindow() == null) {
                    System.err.println("⚠️ Scene/window introuvable, déconnexion DB seule");
                    return;
                }

                Parent root  = FXMLLoader.load(getClass().getResource("/login.fxml"));
                Stage  stage = (Stage) scene.getWindow();
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.show();

                // Petit message d'info
                Alert info = new Alert(AlertType.INFORMATION);
                info.setTitle("Déconnexion");
                info.setHeaderText("Vous avez été déconnecté");
                info.setContentText("Pour des raisons de sécurité, vous avez été déconnecté "
                        + "automatiquement après une période d'inactivité.");
                info.showAndWait();

            } catch (Exception e) {
                System.err.println("❌ Erreur déconnexion auto : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void detach() {
        if (idleTimer      != null) idleTimer.stop();
        if (popupCountdown != null) popupCountdown.stop();
        if (popupAlert != null && popupAlert.isShowing()) popupAlert.close();
        System.out.println("🕒 IdleSessionManager détaché");
    }
}
