package com.qa.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

import com.qa.app.util.DBUtil;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize database and create tables if they don't exist
        DBUtil.initializeDatabase();
        
        // Load the FXML file for the main view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/qa/app/ui/view/main_view.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Gatling Testing System");
        primaryStage.setScene(new Scene(root, 1440, 900));
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/favicon.ico")));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}