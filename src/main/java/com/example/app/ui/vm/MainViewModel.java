package com.example.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MainViewModel implements Initializable {

    @FXML
    private BorderPane mainPane;
    @FXML
    private ListView<String> navigationList;
    @FXML
    private TabPane contentTabPane;

    private final ObservableList<String> navItems = FXCollections.observableArrayList("User Management", "System Settings"); // Add more items as needed
    private final Map<String, String> fxmlMapping = new HashMap<>();
    private final Map<String, Node> loadedTabs = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize FXML mapping
        fxmlMapping.put("User Management", "/com/example/app/ui/view/user_view.fxml");
         // fxmlMapping.put("System Settings", "/com/example/app/ui/view/settings_view.fxml"); // Placeholder for settings view path

        navigationList.setItems(navItems); // navItems is already initialized with "User Management" and "System Settings"
        navigationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openOrSelectTab(newValue);
            }
        });

        // Select the first item by default if available
        if (!navItems.isEmpty()) {
            navigationList.getSelectionModel().selectFirst();
        }
    }

    private void openOrSelectTab(String tabName) {
        // Check if tab is already open
        for (Tab tab : contentTabPane.getTabs()) {
            if (tab.getText().equals(tabName)) {
                contentTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        // If not open, load and add new tab
        String fxmlPath = fxmlMapping.get(tabName);
        if (fxmlPath != null) {
            try {
                Node contentNode = loadedTabs.get(tabName);
                if (contentNode == null) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    contentNode = loader.load();
                    loadedTabs.put(tabName, contentNode);
                }

                Tab newTab = new Tab(tabName);
                newTab.setContent(contentNode);
                newTab.setClosable(true);
                newTab.setOnClosed(event -> loadedTabs.remove(tabName)); // Remove from cache on close

                contentTabPane.getTabs().add(newTab);
                contentTabPane.getSelectionModel().select(newTab);

            } catch (IOException e) {
                System.err.println("Failed to load FXML for tab: " + tabName + " from " + fxmlPath);
                e.printStackTrace();
                // Show an error dialog to the user
                // Alert alert = new Alert(Alert.AlertType.ERROR);
                // alert.setTitle("Loading Error");
                // alert.setHeaderText("Could not load module: " + tabName);
                // alert.setContentText("Please check the FXML file path or contact an administrator.");
                // alert.showAndWait();
            }
        } else {
            System.err.println("No FXML mapping found for: " + tabName);
            // Optionally, show a placeholder or error tab
            Tab errorTab = new Tab(tabName + " (Not Implemented)");
            errorTab.setContent(new javafx.scene.control.Label("Feature '" + tabName + "' is not yet implemented."));
            contentTabPane.getTabs().add(errorTab);
            contentTabPane.getSelectionModel().select(errorTab);
        }
    }
}