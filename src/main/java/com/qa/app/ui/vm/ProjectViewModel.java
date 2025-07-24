package com.qa.app.ui.vm;

import com.qa.app.model.Project;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IProjectService;
import com.qa.app.service.impl.ProjectServiceImpl;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class ProjectViewModel implements Initializable {
    @FXML private TextField projectNameField;
    @FXML private TextArea projectDescriptionField;
    @FXML private Button addButton;
    @FXML
    private Button duplicateButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, String> projectNameColumn;
    @FXML private TableColumn<Project, String> projectDescriptionColumn;

    private final ObservableList<Project> projectList = FXCollections.observableArrayList();
    private final IProjectService projectService = new ProjectServiceImpl();
    private Project selectedProject = null;
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        projectNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        projectDescriptionColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        projectDescriptionColumn.setCellFactory(param -> new ClickableTooltipTableCell<>());
        projectTable.setItems(projectList);
        projectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        loadProjects();
        projectTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> onTableSelectionChanged(newSel));
        addButton.setOnAction(e -> handleAddProject());
        duplicateButton.setOnAction(e -> handleDuplicateProject());
        updateButton.setOnAction(e -> handleUpdateProject());
        deleteButton.setOnAction(e -> handleDeleteProject());
        clearButton.setOnAction(e -> handleClearForm());
    }

    private void loadProjects() {
        try {
            projectList.clear();
            projectList.addAll(projectService.getAllProjects());
            if (!projectList.isEmpty()) {
                projectTable.getSelectionModel().selectFirst();
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load projects: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void onTableSelectionChanged(Project project) {
        selectedProject = project;
        if (project != null) {
            projectNameField.setText(project.getName());
            projectDescriptionField.setText(project.getDescription());
        } else {
            clearForm();
        }
    }

    private void handleDuplicateProject() {
        if (selectedProject == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Please select a project to duplicate.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        String newName = selectedProject.getName() + " (copy)";
        // A check for project name uniqueness should be added if necessary.

        try {
            projectService.addProject(new Project(newName, selectedProject.getDescription()));
            loadProjects();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Project duplicated and added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to duplicate project: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void handleAddProject() {
        String name = projectNameField.getText().trim();
        String desc = projectDescriptionField.getText().trim();
        if (name.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Project name is required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            projectService.addProject(new Project(name, desc));
            loadProjects();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Project added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to add project: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void handleUpdateProject() {
        if (selectedProject == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a project to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        selectedProject.setName(projectNameField.getText().trim());
        selectedProject.setDescription(projectDescriptionField.getText().trim());
        try {
            projectService.updateProject(selectedProject);
            loadProjects();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Project updated successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update project: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void handleDeleteProject() {
        if (selectedProject == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a project to delete.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            projectService.deleteProject(selectedProject.getId());
            loadProjects();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Project deleted successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to delete project: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void handleClearForm() {
        clearForm();
    }

    private void clearForm() {
        projectNameField.clear();
        projectDescriptionField.clear();
        projectTable.getSelectionModel().clearSelection();
        selectedProject = null;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadProjects();
        if (!projectList.isEmpty()) {
            projectTable.getSelectionModel().selectFirst();
        }
    }
} 