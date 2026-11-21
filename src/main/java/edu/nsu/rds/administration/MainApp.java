package edu.nsu.rds.administration;

import edu.nsu.rds.administration.config.DatabaseConnection;
import edu.nsu.rds.administration.controller.AdminDashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void init() {
        try {
            DatabaseConnection.initializeDatabase();
        } catch (Throwable t) {
            System.err.println("Notice during database initialization: " + t.getMessage());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("NSU RDS 2026 - Team 20: RDS Administration & System Monitoring (Sakhawat Rahman Anan - 2614160642)");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);

        try {
            AdminDashboardView dashboardView = new AdminDashboardView();
            Scene scene = new Scene(dashboardView.createContent(), 1240, 800);

            URL cssUrl = getClass().getClassLoader().getResource("styles/dashboard.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.toFront();
        } catch (Throwable t) {
            System.err.println("FATAL EXCEPTION in MainApp.start():");
            t.printStackTrace();

            VBox errorBox = new VBox(12);
            errorBox.setStyle("-fx-padding: 24; -fx-background-color: #fee2e2;");
            Label errTitle = new Label("Startup Exception on JavaFX Application Thread:");
            errTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #991b1b;");

            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            TextArea errArea = new TextArea(sw.toString());
            errArea.setEditable(false);
            VBox.setVgrow(errArea, Priority.ALWAYS);

            errorBox.getChildren().addAll(errTitle, errArea);
            Scene errScene = new Scene(errorBox, 900, 600);
            primaryStage.setScene(errScene);
            primaryStage.show();
            primaryStage.toFront();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
