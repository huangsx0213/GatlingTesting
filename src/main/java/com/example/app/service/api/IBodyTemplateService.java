package com.example.app.service.api;

import com.example.app.model.BodyTemplate;
import com.example.app.service.ServiceException;

import java.util.List;

public interface IBodyTemplateService {
    void createBodyTemplate(BodyTemplate template) throws ServiceException;
    BodyTemplate findBodyTemplateById(int id) throws ServiceException;
    BodyTemplate findBodyTemplateByName(String name) throws ServiceException;
    List<BodyTemplate> findAllBodyTemplates() throws ServiceException;
    void updateBodyTemplate(BodyTemplate template) throws ServiceException;
    void deleteBodyTemplate(int id) throws ServiceException;
} 