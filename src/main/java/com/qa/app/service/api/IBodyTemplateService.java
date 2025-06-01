package com.qa.app.service.api;

import java.util.List;

import com.qa.app.model.BodyTemplate;
import com.qa.app.service.ServiceException;

public interface IBodyTemplateService {
    void createBodyTemplate(BodyTemplate template) throws ServiceException;
    BodyTemplate findBodyTemplateById(int id) throws ServiceException;
    BodyTemplate findBodyTemplateByName(String name) throws ServiceException;
    List<BodyTemplate> findAllBodyTemplates() throws ServiceException;
    void updateBodyTemplate(BodyTemplate template) throws ServiceException;
    void deleteBodyTemplate(int id) throws ServiceException;
} 