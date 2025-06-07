package com.qa.app.service.impl;

import java.sql.SQLException;
import java.util.List;

import com.qa.app.dao.api.IHeadersTemplateDao;
import com.qa.app.dao.impl.HeadersTemplateDaoImpl;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IHeadersTemplateService;

public class HeadersTemplateServiceImpl implements IHeadersTemplateService {
    private final IHeadersTemplateDao dao = new HeadersTemplateDaoImpl();

    @Override
    public void addHeadersTemplate(HeadersTemplate template) throws ServiceException {
        try {
            if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
                throw new ServiceException("Template name is required.");
            }
            if (dao.getHeadersTemplateByName(template.getName()) != null) {
                throw new ServiceException("Template with this name already exists.");
            }
            dao.addHeadersTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateHeadersTemplate(HeadersTemplate template) throws ServiceException {
        try {
            if (template == null || template.getId() <= 0) {
                throw new ServiceException("Template ID is required.");
            }
            dao.updateHeadersTemplate(template);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteHeadersTemplate(int id) throws ServiceException {
        try {
            dao.deleteHeadersTemplate(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public HeadersTemplate getHeadersTemplateById(int id) throws ServiceException {
        try {
            return dao.getHeadersTemplateById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public HeadersTemplate getHeadersTemplateByName(String name) throws ServiceException {
        try {
            return dao.getHeadersTemplateByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HeadersTemplate> getAllHeadersTemplates() throws ServiceException {
        try {
            return dao.getAllHeadersTemplates();
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HeadersTemplate> getHeadersTemplatesByProjectId(Integer projectId) throws ServiceException {
        try {
            return dao.getHeadersTemplatesByProjectId(projectId);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }
} 