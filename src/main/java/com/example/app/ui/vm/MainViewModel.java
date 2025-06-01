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
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MainViewModel implements Initializable {

    @FXML
    private Label statusLabel;

    @FXML
    private BorderPane mainPane;
    @FXML
    private ListView<String> navigationList;
    @FXML
    private TabPane contentTabPane;

    @FXML
    private Label currentFeatureLabel;

    private final ObservableList<String> navItems = FXCollections.observableArrayList(
        "Gatling Test Management",
        "Body Template Management",
        "Headers Template Management",
        "System Settings"
    );
    private final Map<String, String> fxmlMapping = new HashMap<>();
    private final Map<String, Node> loadedTabs = new HashMap<>();

    // Enum for status types
    public enum StatusType {
        INFO,
        SUCCESS,
        ERROR
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize FXML mapping
        fxmlMapping.put("Gatling Test Management", "/com/example/app/ui/view/gatling_test_view.fxml");
        fxmlMapping.put("Body Template Management", "/com/example/app/ui/view/body_template_view.fxml");
        fxmlMapping.put("Headers Template Management", "/com/example/app/ui/view/headers_template_view.fxml");
        // fxmlMapping.put("System Settings", "/com/example/app/ui/view/settings_view.fxml"); // Placeholder for settings view path

        navigationList.setItems(navItems); // navItems is already initialized with "Gatling Test Management", "Body Template Management", "Headers Template Management", and "System Settings"
        updateStatus("Welcome to Gatling Testing System!", StatusType.INFO);
        navigationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openOrSelectTab(newValue);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(newValue);
                }
            }
        });

        contentTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && currentFeatureLabel != null) {
                currentFeatureLabel.setText(newValue.getText());
            }
        });

        // Select the first item by default if available
        if (!navItems.isEmpty()) {
            String firstItem = navItems.get(0);
            navigationList.getSelectionModel().selectFirst();
            // openOrSelectTab(firstItem); // This will be triggered by the listener above
            if (currentFeatureLabel != null) {
                currentFeatureLabel.setText(firstItem);
            }
        }
    }

    private void openOrSelectTab(String tabName) {
        // Check if tab is already open
        for (Tab tab : contentTabPane.getTabs()) {
            if (tab.getText().equals(tabName)) {
                contentTabPane.getSelectionModel().select(tab);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }
                // 切换到已打开tab时刷新内容
                Node contentNode = tab.getContent();
                Object controller = null;
                if (contentNode != null) {
                    controller = contentNode.getProperties().get("controller");
                }
                if (controller == null && contentNode != null) {
                    // 尝试通过UserData获取
                    controller = contentNode.getUserData();
                }
                if (controller instanceof GatlingTestViewModel) {
                    ((GatlingTestViewModel) controller).refresh();
                } else if (controller instanceof BodyTemplateViewModel) {
                    ((BodyTemplateViewModel) controller).refresh();
                } else if (controller instanceof HeadersTemplateViewModel) {
                    ((HeadersTemplateViewModel) controller).refresh();
                }
                return;
            }
        }

        // If not open, load and add new tab
        String fxmlPath = fxmlMapping.get(tabName);
        if (fxmlPath != null) {
            try {
                Node contentNode = loadedTabs.get(tabName);
                FXMLLoader loader = null;
                if (contentNode == null) {
                    loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    contentNode = loader.load();
                    Object controller = loader.getController();
                    if (controller instanceof GatlingTestViewModel) {
                        ((GatlingTestViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof BodyTemplateViewModel) {
                        ((BodyTemplateViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof HeadersTemplateViewModel) {
                        ((HeadersTemplateViewModel) controller).setMainViewModel(this);
                    }
                    // 让Node能找到controller
                    contentNode.getProperties().put("controller", controller);
                    loadedTabs.put(tabName, contentNode);
                }

                Tab newTab = new Tab(tabName);
                newTab.setContent(contentNode);
                newTab.setClosable(true);
                newTab.setOnClosed(event -> loadedTabs.remove(tabName)); // Remove from cache on close

                contentTabPane.getTabs().add(newTab);
                contentTabPane.getSelectionModel().select(newTab);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }
                // 新打开tab时也刷新内容
                Object controller = contentNode.getProperties().get("controller");
                if (controller == null) {
                    controller = contentNode.getUserData();
                }
                if (controller instanceof GatlingTestViewModel) {
                    ((GatlingTestViewModel) controller).refresh();
                } else if (controller instanceof BodyTemplateViewModel) {
                    ((BodyTemplateViewModel) controller).refresh();
                } else if (controller instanceof HeadersTemplateViewModel) {
                    ((HeadersTemplateViewModel) controller).refresh();
                }

            } catch (IOException e) {
                System.err.println("Failed to load FXML for tab: " + tabName + " from " + fxmlPath);
                e.printStackTrace();
            }
        } else {
            System.err.println("No FXML mapping found for: " + tabName);
            // Optionally, show a placeholder or error tab
             Tab errorTab = new Tab(tabName + " (Not Implemented)");
             errorTab.setContent(new javafx.scene.control.Label("Feature '" + tabName + "' is not yet implemented."));
             contentTabPane.getTabs().add(errorTab);
             contentTabPane.getSelectionModel().select(errorTab);
             if (currentFeatureLabel != null) {
                currentFeatureLabel.setText(tabName + " (Not Implemented)");
             }
             updateStatus("Feature '" + tabName + "' is not yet implemented.", StatusType.INFO);
         }
     }

    public void updateStatus(String message, StatusType type) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            switch (type) {
                case INFO:
                    statusLabel.setTextFill(Color.BLACK);
                    break;
                case SUCCESS:
                    statusLabel.setTextFill(Color.GREEN);
                    break;
                case ERROR:
                    statusLabel.setTextFill(Color.RED);
                    break;
            }
        }
    }
}