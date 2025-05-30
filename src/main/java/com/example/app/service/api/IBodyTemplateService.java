package com.example.app.service.api;

import com.example.app.model.BodyTemplate;
import com.example.app.service.ServiceException;

import java.util.List;

public interface IBodyTemplateService {
    void addTemplate(BodyTemplate template) throws ServiceException;
    void updateTemplate(BodyTemplate template) throws ServiceException;
    void deleteTemplate(int id) throws ServiceException;
    BodyTemplate getTemplateById(int id) throws ServiceException;
    BodyTemplate getTemplateByName(String name) throws ServiceException;
    List<BodyTemplate> getAllTemplates() throws ServiceException;
} 