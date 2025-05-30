package com.example.app.service;

import com.example.app.dao.BodyTemplateDaoImpl;
import com.example.app.dao.IBodyTemplateDao;
import com.example.app.model.BodyTemplate;

import java.sql.SQLException;
import java.util.List;

public class BodyTemplateServiceImpl implements IBodyTemplateService {
    private final IBodyTemplateDao dao = new BodyTemplateDaoImpl();

    @Override
    public void addTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template name is required.");
            }
            if (dao.getTemplateByName(template.getName()) != null) {
                throw new ServiceException("Template with this name already exists.");
            }
            dao.addTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateTemplate(BodyTemplate template) throws ServiceException {
        try {
            if (template == null || template.getId() <= 0) {
                throw new ServiceException("Template ID is required.");
            }
            dao.updateTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTemplate(int id) throws ServiceException {
        try {
            dao.deleteTemplate(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate getTemplateById(int id) throws ServiceException {
        try {
            return dao.getTemplateById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public BodyTemplate getTemplateByName(String name) throws ServiceException {
        try {
            return dao.getTemplateByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BodyTemplate> getAllTemplates() throws ServiceException {
        try {
            return dao.getAllTemplates();
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }
} 