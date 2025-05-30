package com.example.app.service;

import com.example.app.model.HeadersTemplate;
import java.util.List;

public interface IHeadersTemplateService {
    void addTemplate(HeadersTemplate template) throws ServiceException;
    void updateTemplate(HeadersTemplate template) throws ServiceException;
    void deleteTemplate(int id) throws ServiceException;
    HeadersTemplate getTemplateById(int id) throws ServiceException;
    HeadersTemplate getTemplateByName(String name) throws ServiceException;
    List<HeadersTemplate> getAllTemplates() throws ServiceException;
} 