package com.example.app;

import com.example.app.util.DBUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize database and create tables if they don't exist
        DBUtil.initializeDatabase();
        
        // Load the FXML file for the main view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/app/ui/view/main_view.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Gatling Testing System");
        primaryStage.setScene(new Scene(root, 1024, 768));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}