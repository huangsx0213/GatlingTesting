package com.example.app.service.impl;

import com.example.app.dao.impl.HeadersTemplateDaoImpl;
import com.example.app.dao.api.IHeadersTemplateDao;
import com.example.app.model.HeadersTemplate;
import com.example.app.service.ServiceException;
import com.example.app.service.api.IHeadersTemplateService;

import java.sql.SQLException;
import java.util.List;

public class HeadersTemplateServiceImpl implements IHeadersTemplateService {
    private final IHeadersTemplateDao dao = new HeadersTemplateDaoImpl();

    @Override
    public void addTemplate(HeadersTemplate template) throws ServiceException {
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
    public void updateTemplate(HeadersTemplate template) throws ServiceException {
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
    public HeadersTemplate getTemplateById(int id) throws ServiceException {
        try {
            return dao.getTemplateById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public HeadersTemplate getTemplateByName(String name) throws ServiceException {
        try {
            return dao.getTemplateByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HeadersTemplate> getAllTemplates() throws ServiceException {
        try {
            return dao.getAllTemplates();
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }
} 