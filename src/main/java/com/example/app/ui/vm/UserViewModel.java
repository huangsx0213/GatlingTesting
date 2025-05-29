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
            showErrorAlert("Failed to load users", e.getMessage());
        }
    }

    private void showUserDetails(User user) {
        if (user != null) {
            // userIdField is now hidden, but we still store the ID internally if needed for logic
            // For example, if you need to display it elsewhere or use it for operations:
            // currentSelectedUserId = user.getId(); // Store it in a member variable if needed
            userIdField.setText(String.valueOf(user.getId())); // Keep this to hold the ID internally
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
            showErrorAlert("Input Error", "Name and email cannot be empty.");
            return;
        }

        User newUser = new User(name, email);
        try {
            userService.registerUser(newUser);
            userList.add(newUser); // The ID will be set by DAO after insertion
            clearFields();
            userTable.getSelectionModel().select(newUser);
            showInfoAlert("Success", "User added successfully.");
        } catch (ServiceException e) {
            showErrorAlert("Failed to add user", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateUser() {
        User selectedUserToUpdate = userTable.getSelectionModel().getSelectedItem();

        if (selectedUserToUpdate == null) {
            showErrorAlert("Selection Error", "Please select a user from the table to update.");
            return;
        }

        // ID is taken from the selected user in the table, not from a visible text field
        int userId = selectedUserToUpdate.getId();

        String name = nameField.getText();
        String email = emailField.getText();

        if (name.isEmpty() || email.isEmpty()) {
            showErrorAlert("Input Error", "Name and email cannot be empty.");
            return;
        }

        try {
            // Create a new User object or update the selected one for the service call
            User userToUpdate = new User(userId, name, email);
            // Or, if your service expects the original object to be modified:
            // selectedUserToUpdate.setName(name);
            // selectedUserToUpdate.setEmail(email);
            // User userToUpdate = selectedUserToUpdate;

            userService.updateUserProfile(userToUpdate);

            // Update the item in the list to reflect changes in TableView
            // This ensures the TableView updates if the User object's properties are JavaFX properties
            // and the table columns are correctly bound.
            // If not, you might need to replace the item in the list:
            int selectedIndex = userList.indexOf(selectedUserToUpdate);
            if (selectedIndex >= 0) {
                userList.set(selectedIndex, userToUpdate); // Assuming userToUpdate is the one with new values
            }
            userTable.refresh(); // Refresh table to show changes
            showInfoAlert("Success", "User information updated successfully.");

        } catch (ServiceException e) {
            showErrorAlert("Failed to update user", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("Selection Error", "Please select a user from the table to delete.");
            return;
        }

        try {
            userService.removeUser(selectedUser.getId());
            userList.remove(selectedUser);
            clearFields();
            showInfoAlert("Success", "User deleted successfully.");
        } catch (ServiceException e) {
            showErrorAlert("Failed to delete user", e.getMessage());
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
        userTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        userIdField.clear(); // Still clear it, as it holds the ID of the selected user internally
        nameField.clear();
        emailField.clear();
        nameField.requestFocus(); // Set focus to the first editable field
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