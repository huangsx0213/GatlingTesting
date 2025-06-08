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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

import java.io.*;
import java.net.URL;
import java.util.*;

public class ApplicationPropertiesViewModel implements Initializable {
    @FXML
    private TableView<PropertyItem> propertiesTable;
    @FXML
    private TableColumn<PropertyItem, String> keyColumn;
    @FXML
    private TableColumn<PropertyItem, String> valueColumn;

    private final ObservableList<PropertyItem> propertyList = FXCollections.observableArrayList();
    private final String propertiesFilePath = "src/main/resources/application.properties";

    private MainViewModel mainViewModel;
    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        keyColumn.setEditable(false);
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        valueColumn.setOnEditCommit(event -> {
            PropertyItem item = event.getRowValue();
            item.setValue(event.getNewValue());
        });
        propertiesTable.setItems(propertyList);
        propertiesTable.setEditable(true);
        loadProperties();
    }

    public void loadProperties() {
        propertyList.clear();
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(propertiesFilePath)) {
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                propertyList.add(new PropertyItem(key, props.getProperty(key)));
            }
        } catch (IOException e) {
            // 可加错误提示
        }
    }

    public void saveProperties() {
        Properties props = new Properties();
        for (PropertyItem item : propertyList) {
            props.setProperty(item.getKey(), item.getValue());
        }
        try (OutputStream output = new FileOutputStream(propertiesFilePath)) {
            props.store(output, "Updated by ApplicationPropertiesViewModel");
        } catch (IOException e) {
            // 可加错误提示
        }
    }

    @FXML
    private void onSave() {
        saveProperties();
        loadProperties(); // 保存后自动刷新
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Save Success: Properties updated.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void onReload() {
        loadProperties();
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
} 