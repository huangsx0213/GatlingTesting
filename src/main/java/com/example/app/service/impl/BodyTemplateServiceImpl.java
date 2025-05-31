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
    public void createTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template name is required.");
            }
            if (templateDao.getTemplateByName(template.getName()) != null) {
                throw new ServiceException("Template with name '" + template.getName() + "' already exists.");
            }
            templateDao.addTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error while creating template: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate findTemplateById(int id) throws ServiceException {
        try {
            return templateDao.getTemplateById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding template by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate findTemplateByName(String name) throws ServiceException {
        try {
            return templateDao.getTemplateByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding template by name: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BodyTemplate> findAllTemplates() throws ServiceException {
        try {
            return templateDao.getAllTemplates();
        } catch (SQLException e) {
            throw new ServiceException("Database error while retrieving all templates: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getId() <= 0 || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template ID and name are required for update.");
            }
            BodyTemplate existingTemplate = templateDao.getTemplateById(template.getId());
            if (existingTemplate == null) {
                throw new ServiceException("Template with ID " + template.getId() + " not found.");
            }
            if (!existingTemplate.getName().equals(template.getName())) {
                if (templateDao.getTemplateByName(template.getName()) != null) {
                    throw new ServiceException("Template with name '" + template.getName() + "' already exists.");
                }
            }
            templateDao.updateTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error while updating template: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTemplate(int id) throws ServiceException {
        try {
            if (templateDao.getTemplateById(id) == null) {
                throw new ServiceException("Template with ID " + id + " not found.");
            }
            templateDao.deleteTemplate(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting template: " + e.getMessage(), e);
        }
    }
} 