package com.example.app.service.api;

import com.example.app.model.HeadersTemplate;
import com.example.app.service.ServiceException;

import java.util.List;

public interface IHeadersTemplateService {
    void addHeadersTemplate(HeadersTemplate template) throws ServiceException;
    void updateHeadersTemplate(HeadersTemplate template) throws ServiceException;
    void deleteHeadersTemplate(int id) throws ServiceException;
    HeadersTemplate getHeadersTemplateById(int id) throws ServiceException;
    HeadersTemplate getHeadersTemplateByName(String name) throws ServiceException;
    List<HeadersTemplate> getAllHeadersTemplates() throws ServiceException;
} 