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

    private final ObservableList<String> navItems = FXCollections.observableArrayList("用户管理", "系统设置"); // Add more items as needed
    private final Map<String, String> fxmlMapping = new HashMap<>();
    private final Map<String, Node> loadedTabs = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize FXML mapping
        fxmlMapping.put("用户管理", "/com/example/app/ui/view/user_view.fxml");
        // fxmlMapping.put("系统设置", "/com/example/app/ui/view/settings_view.fxml"); // Example for another view

        navigationList.setItems(navItems);
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
                // alert.setTitle("加载错误");
                // alert.setHeaderText("无法加载模块: " + tabName);
                // alert.setContentText("请检查FXML文件路径或联系管理员。");
                // alert.showAndWait();
            }
        } else {
            System.err.println("No FXML mapping found for: " + tabName);
            // Optionally, show a placeholder or error tab
            Tab errorTab = new Tab(tabName + " (未实现)");
            errorTab.setContent(new javafx.scene.control.Label("功能 '" + tabName + "' 尚未实现。"));
            contentTabPane.getTabs().add(errorTab);
            contentTabPane.getSelectionModel().select(errorTab);
        }
    }
}