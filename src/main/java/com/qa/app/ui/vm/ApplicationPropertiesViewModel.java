package com.qa.app.ui.vm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

import java.net.URL;
import java.util.*;

import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.dao.impl.ProjectDaoImpl;
import com.qa.app.dao.impl.EnvironmentDaoImpl;
import com.qa.app.model.Project;
import com.qa.app.util.AppConfig;
import com.qa.app.model.Environment;

public class ApplicationPropertiesViewModel implements Initializable, AppConfigChangeListener {
    @FXML
    private TableView<PropertyItem> propertiesTable;
    @FXML
    private TableColumn<PropertyItem, String> keyColumn;
    @FXML
    private TableColumn<PropertyItem, String> valueColumn;

    private final ObservableList<PropertyItem> propertyList = FXCollections.observableArrayList();

    private MainViewModel mainViewModel;
    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    private final ProjectDaoImpl projectDao = new ProjectDaoImpl();
    private final EnvironmentDaoImpl environmentDao = new EnvironmentDaoImpl();

    // Cached lists for combobox options
    private final ObservableList<String> projectNameOptions = FXCollections.observableArrayList();
    private final ObservableList<String> environmentNameOptions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        keyColumn.setEditable(false);
        valueColumn.setCellFactory(col -> new ComboBoxEditingCell());
        propertiesTable.setItems(propertyList);
        propertiesTable.setEditable(true);
        AppConfig.addChangeListener(this);
        loadProperties();
        loadComboBoxOptions();
    }

    @Override
    public void onConfigChanged() {
        loadProperties();
    }

    public void loadProperties() {
        propertyList.clear();
        Properties props = AppConfig.getProperties();
        for (String key : props.stringPropertyNames()) {
            propertyList.add(new PropertyItem(key, props.getProperty(key)));
        }
        // Refresh combo-box items so that newly added Projects/Environments
        // are available whenever the tab is (re)opened or reloaded.
        loadComboBoxOptions();
    }

    @FXML
    private void onSave() {
        Properties props = new Properties();
        for (PropertyItem item : propertyList) {
            props.setProperty(item.getKey(), item.getValue());
        }
        AppConfig.saveProperties(props);
        loadProperties();
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Save Success: Properties updated.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void onReload() {
        loadProperties();
    }

    private void loadComboBoxOptions() {
        try {
            projectNameOptions.setAll(
                projectDao.getAllProjects()
                    .stream()
                    .map(Project::getName)
                    .toList()
            );
            environmentNameOptions.setAll(
                environmentDao.getAllEnvironments()
                    .stream()
                    .map(Environment::getName)
                    .toList()
            );
        } catch (Exception e) {
            System.err.println("Failed to load combo box options: " + e.getMessage());
        }
    }

    public static class PropertyItem {
        private final StringProperty key;
        private final StringProperty value;
        public PropertyItem(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }
        public String getKey() { return key.get(); }
        public void setKey(String k) { key.set(k); }
        public StringProperty keyProperty() { return key; }
        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public StringProperty valueProperty() { return value; }
    }

    private class ComboBoxEditingCell extends TableCell<PropertyItem, String> {
        private ComboBox<String> comboBox;

        private void createComboBox() {
            comboBox = new ComboBox<>();
            comboBox.setEditable(true);
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.getSelectionModel().select(getItem());
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    commitEdit(newVal);
                    PropertyItem rowItem = getTableView().getItems().get(getIndex());
                    rowItem.setValue(newVal);
                }
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (comboBox == null) {
                createComboBox();
            }
            updateComboBoxItems();
            setText(null);
            setGraphic(comboBox);
            comboBox.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (comboBox != null) {
                        comboBox.setValue(item);
                    }
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        }

        private void updateComboBoxItems() {
            PropertyItem propertyItem = getTableView().getItems().get(getIndex());
            if (propertyItem == null) {
                return;
            }
            switch (propertyItem.getKey()) {
                case "current.project.name" -> comboBox.setItems(projectNameOptions);
                case "current.env" -> comboBox.setItems(environmentNameOptions);
                default -> comboBox.setItems(FXCollections.observableArrayList());
            }
        }
    }
} 