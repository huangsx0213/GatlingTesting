package com.example.app.service;

import com.example.app.dao.IUserDao;
import com.example.app.dao.UserDaoImpl;
import com.example.app.model.User;

import java.sql.SQLException;
import java.util.List;

public class UserServiceImpl implements IUserService {

    private final IUserDao userDao = new UserDaoImpl(); // In a real app, use dependency injection

    @Override
    public void registerUser(User user) throws ServiceException {
        try {
            // Basic validation (can be more complex)
            if (user == null || user.getName() == null || user.getName().trim().isEmpty() ||
                user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                throw new ServiceException("User name and email cannot be empty.");
            }
            if (userDao.getUserByEmail(user.getEmail()) != null) {
                throw new ServiceException("Email already exists: " + user.getEmail());
            }
            userDao.addUser(user);
        } catch (SQLException e) {
            // Log the exception (e.g., using a logging framework)
            System.err.println("Error registering user: " + e.getMessage());
            throw new ServiceException("Error registering user. Please try again later.", e);
        }
    }

    @Override
    public User findUserById(int id) throws ServiceException {
        try {
            return userDao.getUserById(id);
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            throw new ServiceException("Error finding user. Please try again later.", e);
        }
    }

    @Override
    public User findUserByEmail(String email) throws ServiceException {
        try {
            return userDao.getUserByEmail(email);
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
            throw new ServiceException("Error finding user. Please try again later.", e);
        }
    }

    @Override
    public List<User> findAllUsers() throws ServiceException {
        try {
            return userDao.getAllUsers();
        } catch (SQLException e) {
            System.err.println("Error finding all users: " + e.getMessage());
            throw new ServiceException("Error retrieving user list. Please try again later.", e);
        }
    }

    @Override
    public void updateUserProfile(User user) throws ServiceException {
        try {
            if (user == null || user.getId() <= 0) {
                throw new ServiceException("Invalid user data for update.");
            }
            // Ensure the user exists before updating
            User existingUser = userDao.getUserById(user.getId());
            if (existingUser == null) {
                throw new ServiceException("User not found for update with ID: " + user.getId());
            }
            // Check if email is being changed and if the new email already exists for another user
            if (!existingUser.getEmail().equals(user.getEmail())) {
                User userWithNewEmail = userDao.getUserByEmail(user.getEmail());
                if (userWithNewEmail != null && userWithNewEmail.getId() != user.getId()) {
                    throw new ServiceException("Email already exists for another user: " + user.getEmail());
                }
            }
            userDao.updateUser(user);
        } catch (SQLException e) {
            System.err.println("Error updating user profile: " + e.getMessage());
            throw new ServiceException("Error updating user profile. Please try again later.", e);
        }
    }

    @Override
    public void removeUser(int id) throws ServiceException {
        try {
            if (userDao.getUserById(id) == null) {
                throw new ServiceException("User not found for deletion with ID: " + id);
            }
            userDao.deleteUser(id);
        } catch (SQLException e) {
            System.err.println("Error removing user: " + e.getMessage());
            throw new ServiceException("Error removing user. Please try again later.", e);
        }
    }
}