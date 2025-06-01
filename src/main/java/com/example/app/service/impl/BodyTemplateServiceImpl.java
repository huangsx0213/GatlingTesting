package com.example.app.service.impl;

import com.example.app.dao.api.IBodyTemplateDao;
import com.example.app.dao.impl.BodyTemplateDaoImpl;
import com.example.app.model.BodyTemplate;
import com.example.app.service.ServiceException;
import com.example.app.service.api.IBodyTemplateService;

import java.sql.SQLException;
import java.util.List;

public class BodyTemplateServiceImpl implements IBodyTemplateService {

    private final IBodyTemplateDao templateDao = new BodyTemplateDaoImpl();

    @Override
    public void createBodyTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template name is required.");
            }
            if (templateDao.getBodyTemplateByName(template.getName()) != null) {
                throw new ServiceException("Template with name '" + template.getName() + "' already exists.");
            }
            templateDao.addBodyTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error while creating template: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate findBodyTemplateById(int id) throws ServiceException {
        try {
            return templateDao.getBodyTemplateById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding template by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate findBodyTemplateByName(String name) throws ServiceException {
        try {
            return templateDao.getBodyTemplateByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding template by name: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BodyTemplate> findAllBodyTemplates() throws ServiceException {
        try {
            return templateDao.getAllBodyTemplates();
        } catch (SQLException e) {
            throw new ServiceException("Database error while retrieving all templates: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateBodyTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getId() <= 0 || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template ID and name are required for update.");
            }
            BodyTemplate existingTemplate = templateDao.getBodyTemplateById(template.getId());
            if (existingTemplate == null) {
                throw new ServiceException("Template with ID " + template.getId() + " not found.");
            }
            if (!existingTemplate.getName().equals(template.getName())) {
                if (templateDao.getBodyTemplateByName(template.getName()) != null) {
                    throw new ServiceException("Template with name '" + template.getName() + "' already exists.");
                }
            }
            templateDao.updateBodyTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error while updating template: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBodyTemplate(int id) throws ServiceException {
        try {
            if (templateDao.getBodyTemplateById(id) == null) {
                throw new ServiceException("Template with ID " + id + " not found.");
            }
            templateDao.deleteBodyTemplate(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting template: " + e.getMessage(), e);
        }
    }
} 