package com.example.app.dao;

import com.example.app.model.User;

import java.sql.SQLException;
import java.util.List;

public interface IUserDao {
    void addUser(User user) throws SQLException;
    User getUserById(int id) throws SQLException;
    User getUserByEmail(String email) throws SQLException;
    List<User> getAllUsers() throws SQLException;
    void updateUser(User user) throws SQLException;
    void deleteUser(int id) throws SQLException;
}