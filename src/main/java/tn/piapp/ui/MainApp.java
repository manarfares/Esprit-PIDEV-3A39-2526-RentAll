package tn.piapp.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.piapp.db.DbConnection;
import tn.piapp.util.Alerts;

import java.sql.SQLException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DbConnection.getInstance();
        } catch (SQLException e) {
            Alerts.showError("Database Error", e.getMessage());
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load(), 900, 580);
            primaryStage.setTitle("RentAll — Login");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            Alerts.showError("Startup Error", e.getMessage());
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
