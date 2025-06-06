package com.qa.app.service.impl;

import com.qa.app.dao.api.IProjectDao;
import com.qa.app.dao.impl.ProjectDaoImpl;
import com.qa.app.model.Project;
import com.qa.app.service.api.IProjectService;
import java.util.List;

public class ProjectServiceImpl implements IProjectService {
    private final IProjectDao projectDao = new ProjectDaoImpl();

    @Override
    public void addProject(Project project) {
        projectDao.addProject(project);
    }

    @Override
    public void updateProject(Project project) {
        projectDao.updateProject(project);
    }

    @Override
    public void deleteProject(int id) {
        projectDao.deleteProject(id);
    }

    @Override
    public Project getProjectById(int id) {
        return projectDao.getProjectById(id);
    }

    @Override
    public List<Project> getAllProjects() {
        return projectDao.getAllProjects();
    }
} 