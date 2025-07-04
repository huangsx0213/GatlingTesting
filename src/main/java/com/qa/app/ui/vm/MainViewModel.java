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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

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

    private final ObservableList<String> navItems = FXCollections.observableArrayList(
        "Gatling Test Management",
        "Gatling Scenario Management",
        "Gatling Test Reports",
        "Gatling Internal Report",
        "Endpoint Management",
        "Headers Template Management",
        "Body Template Management",
        "Environment Management",
        "Project Management",
        "Variables Management",
        "Application Properties"
    );
    private final Map<String, String> fxmlMapping = new HashMap<>();
    private final Map<String, Node> loadedTabs = new HashMap<>();

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
        Platform.runLater(this::loadAndSetCurrentProject);
    }

    public void reinitialize() {
        // Initialize FXML mapping
        fxmlMapping.clear();
        fxmlMapping.put("Gatling Test Management", "/com/qa/app/ui/view/gatling_test_view.fxml");
        fxmlMapping.put("Endpoint Management", "/com/qa/app/ui/view/endpoint_view.fxml");
        fxmlMapping.put("Headers Template Management", "/com/qa/app/ui/view/headers_template_view.fxml");
        fxmlMapping.put("Body Template Management", "/com/qa/app/ui/view/body_template_view.fxml");
        fxmlMapping.put("Environment Management", "/com/qa/app/ui/view/environment_view.fxml");
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
        }
    }

    private void openOrSelectTab(String tabName) {
        // AppConfig.reload(); // It's better to reload config only when necessary, e.g., in App Properties tab
        // Check if tab is already open
        for (Tab tab : contentTabPane.getTabs()) {
            if (tab.getText().equals(tabName)) {
                contentTabPane.getSelectionModel().select(tab);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }
                // refresh content when switch to opened tab
                Node contentNode = tab.getContent();
                Object controller = null;
                if (contentNode != null) {
                    controller = contentNode.getProperties().get("controller");
                }
                if (controller == null && contentNode != null) {
                    // try to get controller by UserData
                    controller = contentNode.getUserData();
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
                    ((ProjectViewModel) controller).setMainViewModel(this);
                } else if (controller instanceof ApplicationPropertiesViewModel) {
                    ((ApplicationPropertiesViewModel) controller).setMainViewModel(this);
                    ((ApplicationPropertiesViewModel) controller).loadProperties();
                } else if (controller instanceof GroovyVariableViewModel) {
                    ((GroovyVariableViewModel) controller).refresh();
                } else if (controller instanceof GatlingTestReportViewModel) {
                    ((GatlingTestReportViewModel) controller).refresh();
                } else if (controller instanceof GatlingScenarioViewModel) {
                    // currently no refresh method needed, placeholder for future
                } else if (controller instanceof GatlingInternalReportViewModel) {
                    ((GatlingInternalReportViewModel) controller).refresh();
                }
                // Gatling tab is not closable and always in the first position
                if (tabName.equals("Gatling Test Management")) {
                    tab.setClosable(false);
                    if (contentTabPane.getTabs().indexOf(tab) != 0) {
                        contentTabPane.getTabs().remove(tab);
                        contentTabPane.getTabs().add(0, tab);
                    }
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
                    }
                    // let Node find controller
                    contentNode.getProperties().put("controller", controller);
                    loadedTabs.put(tabName, contentNode);
                }

                Tab newTab = new Tab(tabName);
                newTab.setContent(contentNode);
                // Gatling tab is not closable and always in the first position
                if (tabName.equals("Gatling Test Management")) {
                    newTab.setClosable(false);
                    contentTabPane.getTabs().add(0, newTab);
                } else {
                    newTab.setClosable(true);
                    contentTabPane.getTabs().add(newTab);
                }
                newTab.setOnClosed(event -> loadedTabs.remove(tabName)); // Remove from cache on close

                contentTabPane.getSelectionModel().select(newTab);
                if (currentFeatureLabel != null) {
                    currentFeatureLabel.setText(tabName);
                }
                // refresh content when open new tab
                Object controller = contentNode.getProperties().get("controller");
                if (controller == null) {
                    controller = contentNode.getUserData();
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
                    ((ApplicationPropertiesViewModel) controller).setMainViewModel(this);
                    ((ApplicationPropertiesViewModel) controller).loadProperties();
                } else if (controller instanceof GroovyVariableViewModel) {
                    ((GroovyVariableViewModel) controller).refresh();
                } else if (controller instanceof GatlingTestReportViewModel) {
                    ((GatlingTestReportViewModel) controller).refresh();
                } else if (controller instanceof GatlingScenarioViewModel) {
                    // currently no refresh method needed, placeholder for future
                } else if (controller instanceof GatlingInternalReportViewModel) {
                    ((GatlingInternalReportViewModel) controller).refresh();
                }
            } catch (IOException e) {
                System.err.println("Failed to load FXML for tab: " + tabName + " from " + fxmlPath);
                e.printStackTrace();
                showGlobalStatus("Error loading page: " + tabName, StatusType.ERROR);
            }
        } else {
            showGlobalStatus("No FXML mapping for: " + tabName, StatusType.ERROR);
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
                    updateStatus("Current project: " + project.getName(), StatusType.INFO);
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
}