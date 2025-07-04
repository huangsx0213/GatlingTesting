package com.qa.app.service.impl;

import com.qa.app.dao.api.IProjectDao;
import com.qa.app.dao.impl.ProjectDaoImpl;
import com.qa.app.model.Project;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IProjectService;
import java.sql.SQLException;
import java.util.List;

public class ProjectServiceImpl implements IProjectService {

    private IProjectDao projectDao;

    public ProjectServiceImpl() {
        this.projectDao = new ProjectDaoImpl();
    }

    @Override
    public List<Project> getAllProjects() throws ServiceException {
        try {
            return projectDao.getAllProjects();
        } catch (SQLException e) {
            throw new ServiceException("Failed to get all projects", e);
        }
    }

    @Override
    public Project getProjectById(Integer id) throws ServiceException {
        try {
            return projectDao.getProject(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to get project by id: " + id, e);
        }
    }

    @Override
    public Project getProjectByName(String name) throws ServiceException {
        try {
            return projectDao.getProjectByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Failed to get project by name: " + name, e);
        }
    }

    @Override
    public void addProject(Project project) throws ServiceException {
        try {
            projectDao.addProject(project);
        } catch (SQLException e) {
            throw new ServiceException("Failed to add project", e);
        }
    }

    @Override
    public void updateProject(Project project) throws ServiceException {
        try {
            projectDao.updateProject(project);
        } catch (SQLException e) {
            throw new ServiceException("Failed to update project", e);
        }
    }

    @Override
    public void deleteProject(Integer id) throws ServiceException {
        try {
            projectDao.deleteProject(id);
        } catch (SQLException e) {
            throw new ServiceException("Failed to delete project", e);
        }
    }
}