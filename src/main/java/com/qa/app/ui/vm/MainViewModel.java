package com.qa.app.ui.vm;

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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import com.qa.app.model.Project;
import com.qa.app.service.ProjectContext;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IProjectService;
import com.qa.app.service.impl.ProjectServiceImpl;
import com.qa.app.util.AppConfig;
import javafx.application.Platform;

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

    @FXML
    private Label currentProjectLabel;

    @FXML
    private Label currentEnvironmentLabel;

    private final ObservableList<String> navItems = FXCollections.observableArrayList(
        "Gatling Test Management",
        "Gatling Scenario Management",
        "Gatling Test Reports",
        "Gatling Internal Report",
        "Endpoint Management",
        "Headers Template Management",
        "Body Template Management",
        "DB Connections Mgmt",
        "Environment Management",
        "Project Management",
        "Variables Management",
        "Application Properties"
    );
    private final Map<String, String> fxmlMapping = new HashMap<>();
    private final Map<String, Node> loadedTabs = new HashMap<>();

    /**
     * The seven management functions that should always share the same single
     * Tab instance. When the user selects any of these entries from the
     * navigation list we will either re-use the existing shared tab (if it is
     * already present) or create one if it does not yet exist, replacing its
     * content and text on every switch instead of opening additional tabs.
     */
    private static final Set<String> SINGLE_TAB_ITEMS = new HashSet<>(Arrays.asList(
            "Endpoint Management",
            "Headers Template Management",
            "Body Template Management",
            "Environment Management",
            "Project Management",
            "Variables Management",
            "DB Connections Mgmt",
            "Application Properties"
    ));

    // Enum for status types
    public enum StatusType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private IProjectService projectService;
    private static MainViewModel instance;

    public MainViewModel() {
        instance = this;
        this.projectService = new ProjectServiceImpl();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reinitialize();
        loadAndSetCurrentProject();
        updateEnvironmentLabel();
        AppConfig.addChangeListener(this::onConfigChanged);
        
        // Force refresh the initial tab content
        if (!navItems.isEmpty() && !contentTabPane.getTabs().isEmpty()) {
            Tab initialTab = contentTabPane.getTabs().get(0);
            if (initialTab != null && initialTab.getContent() != null) {
                Node contentNode = initialTab.getContent();
                Object controller = contentNode.getProperties().get("controller");
                if (controller == null) {
                    controller = contentNode.getUserData();
                }
                if (controller instanceof GatlingTestViewModel) {
                    ((GatlingTestViewModel) controller).refreshDynamicVariables();
                    ((GatlingTestViewModel) controller).refresh();
                }
            }
        }
    }

    private void onConfigChanged() {
        Platform.runLater(() -> {
            loadAndSetCurrentProject();
            updateEnvironmentLabel();
        });
    }

    public void reinitialize() {
        // Initialize FXML mapping
        fxmlMapping.clear();
        fxmlMapping.put("Gatling Test Management", "/com/qa/app/ui/view/gatling_test_view.fxml");
        fxmlMapping.put("Endpoint Management", "/com/qa/app/ui/view/endpoint_view.fxml");
        fxmlMapping.put("Headers Template Management", "/com/qa/app/ui/view/headers_template_view.fxml");
        fxmlMapping.put("Body Template Management", "/com/qa/app/ui/view/body_template_view.fxml");
        fxmlMapping.put("Environment Management", "/com/qa/app/ui/view/environment_view.fxml");
        fxmlMapping.put("DB Connections Mgmt", "/com/qa/app/ui/view/db_connection_view.fxml");
        fxmlMapping.put("Project Management", "/com/qa/app/ui/view/project_view.fxml");
        fxmlMapping.put("Variables Management", "/com/qa/app/ui/view/groovy_variable_view.fxml");
        fxmlMapping.put("Gatling Scenario Management", "/com/qa/app/ui/view/gatling_scenario_view.fxml");
        fxmlMapping.put("Application Properties", "/com/qa/app/ui/view/application_properties_view.fxml");
        fxmlMapping.put("Gatling Test Reports", "/com/qa/app/ui/view/gatling_test_report_view.fxml");
        fxmlMapping.put("Gatling Internal Report", "/com/qa/app/ui/view/gatling_internal_report_view.fxml");
        // Removed System Settings mapping

        navigationList.setItems(navItems);
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
            if (currentFeatureLabel != null) {
                currentFeatureLabel.setText(firstItem);
            }
            // Open scenario tab by default as second fixed tab
            openOrSelectTab("Gatling Scenario Management");
            // Ensure we revert selection back to first tab
            contentTabPane.getSelectionModel().select(0);
        }
    }

    private void openOrSelectTab(String tabName) {
        // ------------------------------------------------------------------
        // 1. General check: if a tab with the exact same name already exists,
        //    simply select it and refresh its content (original behaviour).
        //    This prevents duplicate tabs for items like "Gatling Test
        //    Management" which are *not* part of SINGLE_TAB_ITEMS.
        // ------------------------------------------------------------------
        for (Tab tab : contentTabPane.getTabs()) {
            if (tab.getText().equals(tabName)) {
                // Ensure Gatling Test Management stays at index 0 and is not closable
                if ("Gatling Test Management".equals(tabName)) {
                    tab.setClosable(false);
                    if (contentTabPane.getTabs().indexOf(tab) != 0) {
                        contentTabPane.getTabs().remove(tab);
                        contentTabPane.getTabs().add(0, tab);
                    }
                }
                // Ensure Scenario tab fixed at index 1 and not closable
                if ("Gatling Scenario Management".equals(tabName)) {
                    tab.setClosable(false);
                    int desiredIndex = Math.min(1, contentTabPane.getTabs().size());
                    if (contentTabPane.getTabs().indexOf(tab) != desiredIndex) {
                        contentTabPane.getTabs().remove(tab);
                        contentTabPane.getTabs().add(desiredIndex, tab);
                    }
                }

                contentTabPane.getSelectionModel().select(tab);
                refreshTabContent(tabName, tab.getContent());
                return;
            }
        }

        // First, handle the group that should always occupy a single Tab
        if (SINGLE_TAB_ITEMS.contains(tabName)) {
            // Try to find an existing tab in this group (could be the same or another item)
            Tab existingGroupTab = null;
            for (Tab tab : contentTabPane.getTabs()) {
                if (SINGLE_TAB_ITEMS.contains(tab.getText())) {
                    existingGroupTab = tab;
                    // If it's already showing the requested page, simply select and refresh
                    if (tab.getText().equals(tabName)) {
                        contentTabPane.getSelectionModel().select(tab);
                        refreshTabContent(tabName, tab.getContent());
                        return;
                    }
                    break; // We need only one such tab
                }
            }

            // Prepare (or retrieve) the content node for the requested page
            String fxmlPath = fxmlMapping.get(tabName);
            if (fxmlPath == null) {
                showGlobalStatus("No FXML mapping for: " + tabName, StatusType.ERROR);
                return;
            }

            try {
                Node contentNode = loadedTabs.get(tabName);
                FXMLLoader loader = null;
                if (contentNode == null) {
                    loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    contentNode = loader.load();
                    Object controller = loader.getController();
                    // Re-use the same helper to wire the controller (code extracted below)
                    wireController(controller);
                    contentNode.getProperties().put("controller", controller);
                    loadedTabs.put(tabName, contentNode);
                }

                if (existingGroupTab != null) {
                    // Replace the content and label of the existing tab
                    existingGroupTab.setText(tabName);
                    existingGroupTab.setContent(contentNode);
                    attachContextMenu(existingGroupTab);
                    contentTabPane.getSelectionModel().select(existingGroupTab);
                } else {
                    // No tab from the group is open yet – create one normally
                    Tab newTab = new Tab(tabName);
                    newTab.setContent(contentNode);
                    newTab.setClosable(true);
                    contentTabPane.getTabs().add(newTab);
                    attachContextMenu(newTab);
                    newTab.setOnClosed(event -> loadedTabs.remove(tabName));
                    contentTabPane.getSelectionModel().select(newTab);
                }

                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }

                // Refresh the content we just displayed
                refreshTabContent(tabName, contentNode);
            } catch (IOException e) {
                System.err.println("Failed to load FXML for tab: " + tabName + " from " + fxmlPath);
                e.printStackTrace();
                showGlobalStatus("Error loading page: " + tabName, StatusType.ERROR);
            }
            return; // We are done handling the single-tab items
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
                    } else if (controller instanceof EnvironmentViewModel) {
                        ((EnvironmentViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof EndpointViewModel) {
                        ((EndpointViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof ProjectViewModel) {
                        ((ProjectViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof ApplicationPropertiesViewModel) {
                        ((ApplicationPropertiesViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof GroovyVariableViewModel) {
                        ((GroovyVariableViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof GatlingScenarioViewModel) {
                        ((GatlingScenarioViewModel) controller).setMainViewModel(this);
                    } else if (controller instanceof GatlingInternalReportViewModel) {
                        // currently no setMainViewModel method needed, placeholder for future
                    } else if (controller instanceof DbConnectionViewModel) {
                        ((DbConnectionViewModel) controller).setMainViewModel(this);
                    }
                    // let Node find controller
                    contentNode.getProperties().put("controller", controller);
                    loadedTabs.put(tabName, contentNode);
                }

                Tab newTab = new Tab(tabName);
                newTab.setContent(contentNode);
                if (tabName.equals("Gatling Test Management")) {
                    newTab.setClosable(false);
                    // Always insert Gatling Test tab at the first position
                    contentTabPane.getTabs().add(0, newTab);
                } else if (tabName.equals("Gatling Scenario Management")) {
                    newTab.setClosable(false);
                    // Always insert Gatling Scenario tab at the second position
                    int insertIndex = Math.min(1, contentTabPane.getTabs().size());
                    contentTabPane.getTabs().add(insertIndex, newTab);
                } else {
                    newTab.setClosable(true);
                    contentTabPane.getTabs().add(newTab);
                }
                attachContextMenu(newTab);
                newTab.setOnClosed(event -> loadedTabs.remove(tabName)); // Remove from cache on close

                contentTabPane.getSelectionModel().select(newTab);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }
                // refresh content when open new tab
                refreshTabContent(tabName, contentNode);
            } catch (IOException e) {
                System.err.println("Failed to load FXML for tab: " + tabName + " from " + fxmlPath);
                e.printStackTrace();
                showGlobalStatus("Error loading page: " + tabName, StatusType.ERROR);
            }
        } else {
            showGlobalStatus("No FXML mapping for: " + tabName, StatusType.ERROR);
        }
    }

    /**
     * Wires the shared MainViewModel into sub-controllers that expose a
     * setMainViewModel method to maintain previous behaviour.
     */
    private void wireController(Object controller) {
        if (controller instanceof GatlingTestViewModel) {
            ((GatlingTestViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof BodyTemplateViewModel) {
            ((BodyTemplateViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof HeadersTemplateViewModel) {
            ((HeadersTemplateViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof EnvironmentViewModel) {
            ((EnvironmentViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof EndpointViewModel) {
            ((EndpointViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof ProjectViewModel) {
            ((ProjectViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof ApplicationPropertiesViewModel) {
            ((ApplicationPropertiesViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof GroovyVariableViewModel) {
            ((GroovyVariableViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof GatlingScenarioViewModel) {
            ((GatlingScenarioViewModel) controller).setMainViewModel(this);
        } else if (controller instanceof DbConnectionViewModel) {
            ((DbConnectionViewModel) controller).setMainViewModel(this);
        }
    }

    /**
     * Centralised method to refresh a tab's controller based on its type in
     * order to avoid duplication in several locations.
     */
    private void refreshTabContent(String tabName, Node contentNode) {
        Object controller = null;
        if (contentNode != null) {
            controller = contentNode.getProperties().get("controller");
            if (controller == null) {
                controller = contentNode.getUserData();
            }
        }

        if (controller instanceof GatlingTestViewModel) {
            ((GatlingTestViewModel) controller).refreshDynamicVariables();
            ((GatlingTestViewModel) controller).refresh();
        } else if (controller instanceof BodyTemplateViewModel) {
            ((BodyTemplateViewModel) controller).refresh();
        } else if (controller instanceof HeadersTemplateViewModel) {
            ((HeadersTemplateViewModel) controller).refresh();
        } else if (controller instanceof EnvironmentViewModel) {
            ((EnvironmentViewModel) controller).refresh();
        } else if (controller instanceof EndpointViewModel) {
            ((EndpointViewModel) controller).refresh();
        } else if (controller instanceof ProjectViewModel) {
            ((ProjectViewModel) controller).refresh();
        } else if (controller instanceof ApplicationPropertiesViewModel) {
            ((ApplicationPropertiesViewModel) controller).loadProperties();
        } else if (controller instanceof GroovyVariableViewModel) {
            ((GroovyVariableViewModel) controller).refresh();
        } else if (controller instanceof GatlingTestReportViewModel) {
            ((GatlingTestReportViewModel) controller).refresh();
        } else if (controller instanceof GatlingScenarioViewModel) {
            ((GatlingScenarioViewModel) controller).refresh();
        } else if (controller instanceof GatlingInternalReportViewModel) {
            ((GatlingInternalReportViewModel) controller).refresh();
        } else if (controller instanceof DbConnectionViewModel) {
            ((DbConnectionViewModel) controller).refresh();
        }
    }

    public void updateStatus(String message, StatusType type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type) {
                case INFO:
                    statusLabel.setTextFill(Color.BLACK);
                    break;
                case SUCCESS:
                    statusLabel.setTextFill(Color.GREEN);
                    break;
                case WARNING:
                    statusLabel.setTextFill(Color.ORANGE);
                    break;
                case ERROR:
                    statusLabel.setTextFill(Color.RED);
                    break;
            }
        });
    }

    public void reloadAllTabs() {
        loadedTabs.clear();
        contentTabPane.getTabs().clear();
        reinitialize();
    }

    private void loadAndSetCurrentProject() {
        String currentProjectName = AppConfig.getProperty("current.project.name");
        if (currentProjectName != null && !currentProjectName.isEmpty()) {
            try {
                Project project = projectService.getProjectByName(currentProjectName);
                if (project != null) {
                    ProjectContext.setCurrentProject(project);
                    if (currentProjectLabel != null) {
                        currentProjectLabel.setText(project.getName());
                    }
                } else {
                    handleNoProject("Project '" + currentProjectName + "' not found.");
                }
            } catch (ServiceException e) {
                updateStatus("Failed to load project: " + e.getMessage(), StatusType.ERROR);
                handleNoProject(e.getMessage());
            }
        } else {
            handleNoProject("No project selected. Please select or create a project.");
        }
    }

    private void handleNoProject(String message) {
        ProjectContext.clearCurrentProject();
        updateStatus(message, StatusType.INFO);
        if (currentProjectLabel != null) {
            currentProjectLabel.setText("-");
        }
    }

    public void closeProject() {
        Project currentProject = ProjectContext.getCurrentProject();
        if (currentProject != null) {
            currentProject.setLastUsed(false);
            try {
                projectService.updateProject(currentProject);
            } catch (ServiceException e) {
                updateStatus("Failed to update project state on close: " + e.getMessage(), StatusType.ERROR);
            }
        }
        ProjectContext.clearCurrentProject();
        AppConfig.removeProperty("current.project.name");
        AppConfig.saveProperties();
        loadAndSetCurrentProject(); // Reload to reflect closed state
    }
    
    @FXML
    private void handleReloadConfig() {
        AppConfig.reload();
        updateStatus("Configuration reloaded.", StatusType.INFO);
    }

    public static void showGlobalStatus(String message, StatusType type) {
        if (instance != null) {
            instance.updateStatus(message, type);
        }
    }

    // Adds context menu to a tab with Close and Close Left Tabs actions
    private void attachContextMenu(Tab tab) {
        ContextMenu contextMenu = new ContextMenu();

        // Close Right Tabs
        MenuItem closeRightItem = new MenuItem("Close Right Tabs");
        closeRightItem.setOnAction(e -> {
            int idx = contentTabPane.getTabs().indexOf(tab);
            // iterate from rightmost to one after current
            for (int i = contentTabPane.getTabs().size() - 1; i > idx; i--) {
                Tab t = contentTabPane.getTabs().get(i);
                if (t.isClosable()) {
                    contentTabPane.getTabs().remove(t);
                }
            }
        });

        // Close All Tabs (except non-closable)
        MenuItem closeAllItem = new MenuItem("Close All Tabs");
        closeAllItem.setOnAction(e -> {
            contentTabPane.getTabs().removeIf(t -> t.isClosable());
        });

        // Only add context menu to closable tabs or add different options for fixed tabs
        if (tab.isClosable()) {
            contextMenu.getItems().addAll(closeRightItem, closeAllItem);
        } else {
            // For fixed tabs, only add the option to close other tabs
            contextMenu.getItems().add(closeAllItem);
        }
        tab.setContextMenu(contextMenu);
    }

    /**
     * Updates the environment label shown in the banner using the latest value
     * from {@link com.qa.app.service.EnvironmentContext}.
     */
    private void updateEnvironmentLabel() {
        String envName = com.qa.app.service.EnvironmentContext.getCurrentEnvironmentName();
        if (currentEnvironmentLabel != null) {
            currentEnvironmentLabel.setText(envName != null ? envName : "-");
        }
    }
}