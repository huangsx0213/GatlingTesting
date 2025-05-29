package com.example.app.service;

import com.example.app.model.User;

import java.util.List;

public interface IUserService {
    void registerUser(User user) throws ServiceException;
    User findUserById(int id) throws ServiceException;
    User findUserByEmail(String email) throws ServiceException;
    List<User> findAllUsers() throws ServiceException;
    void updateUserProfile(User user) throws ServiceException;
    void removeUser(int id) throws ServiceException;
}