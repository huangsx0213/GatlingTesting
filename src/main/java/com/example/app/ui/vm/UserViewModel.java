package com.example.app.ui.vm;

import com.example.app.model.User;
import com.example.app.service.IUserService;
import com.example.app.service.ServiceException;
import com.example.app.service.UserServiceImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class UserViewModel implements Initializable {

    @FXML
    private TextField userIdField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Integer> idColumn;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;

    private final IUserService userService = new UserServiceImpl(); // In a real app, use dependency injection
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        userTable.setItems(userList);

        // Add listener for table selection
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showUserDetails(newValue)
        );

        // Load initial data
        loadUsers();
    }

    private void loadUsers() {
        try {
            userList.setAll(userService.findAllUsers());
        } catch (ServiceException e) {
            showErrorAlert("加载用户失败", e.getMessage());
        }
    }

    private void showUserDetails(User user) {
        if (user != null) {
            userIdField.setText(String.valueOf(user.getId()));
            nameField.setText(user.getName());
            emailField.setText(user.getEmail());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddUser() {
        String name = nameField.getText();
        String email = emailField.getText();

        if (name.isEmpty() || email.isEmpty()) {
            showErrorAlert("输入错误", "姓名和邮箱不能为空。");
            return;
        }

        User newUser = new User(name, email);
        try {
            userService.registerUser(newUser);
            userList.add(newUser); // The ID will be set by DAO after insertion
            clearFields();
            userTable.getSelectionModel().select(newUser);
            showInfoAlert("成功", "用户添加成功。");
        } catch (ServiceException e) {
            showErrorAlert("添加用户失败", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateUser() {
        String idText = userIdField.getText();
        if (idText.isEmpty()) {
            showErrorAlert("选择错误", "请先从表格中选择一个用户进行修改。");
            return;
        }

        try {
            int userId = Integer.parseInt(idText);
            String name = nameField.getText();
            String email = emailField.getText();

            if (name.isEmpty() || email.isEmpty()) {
                showErrorAlert("输入错误", "姓名和邮箱不能为空。");
                return;
            }

            User selectedUser = userTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null || selectedUser.getId() != userId) {
                 selectedUser = userService.findUserById(userId);
                 if(selectedUser == null){
                    showErrorAlert("错误", "未找到要更新的用户。");
                    return;
                 }
            }

            selectedUser.setName(name);
            selectedUser.setEmail(email);

            userService.updateUserProfile(selectedUser);
            // Refresh the table item if PropertyValueFactory is used correctly
            // or manually refresh the list/item
            userTable.refresh(); // This should update the view for the modified user
            showInfoAlert("成功", "用户信息更新成功。");

        } catch (NumberFormatException e) {
            showErrorAlert("输入错误", "用户ID格式无效。");
        } catch (ServiceException e) {
            showErrorAlert("更新用户失败", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("选择错误", "请先从表格中选择一个用户进行删除。");
            return;
        }

        try {
            userService.removeUser(selectedUser.getId());
            userList.remove(selectedUser);
            clearFields();
            showInfoAlert("成功", "用户删除成功。");
        } catch (ServiceException e) {
            showErrorAlert("删除用户失败", e.getMessage());
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
        userTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        userIdField.clear();
        nameField.clear();
        emailField.clear();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}