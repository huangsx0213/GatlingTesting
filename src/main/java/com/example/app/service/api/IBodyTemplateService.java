package com.example.app.service.api;

import com.example.app.model.BodyTemplate;
import com.example.app.service.ServiceException;

import java.util.List;

public interface IBodyTemplateService {
    void createTemplate(BodyTemplate template) throws ServiceException;
    BodyTemplate findTemplateById(int id) throws ServiceException;
    BodyTemplate findTemplateByName(String name) throws ServiceException;
    List<BodyTemplate> findAllTemplates() throws ServiceException;
    void updateTemplate(BodyTemplate template) throws ServiceException;
    void deleteTemplate(int id) throws ServiceException;
} 