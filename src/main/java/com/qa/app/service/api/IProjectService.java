package com.qa.app.service.api;

import com.qa.app.model.Project;
import com.qa.app.service.ServiceException;
import java.util.List;

public interface IProjectService {
    void addProject(Project project) throws ServiceException;
    void updateProject(Project project) throws ServiceException;
    void deleteProject(Integer id) throws ServiceException;
    Project getProjectById(Integer id) throws ServiceException;
    Project getProjectByName(String name) throws ServiceException;
    List<Project> getAllProjects() throws ServiceException;
} 