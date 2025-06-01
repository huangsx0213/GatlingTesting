package com.qa.app.service.api;

import java.util.List;

import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.ServiceException;

public interface IHeadersTemplateService {
    void addHeadersTemplate(HeadersTemplate template) throws ServiceException;
    void updateHeadersTemplate(HeadersTemplate template) throws ServiceException;
    void deleteHeadersTemplate(int id) throws ServiceException;
    HeadersTemplate getHeadersTemplateById(int id) throws ServiceException;
    HeadersTemplate getHeadersTemplateByName(String name) throws ServiceException;
    List<HeadersTemplate> getAllHeadersTemplates() throws ServiceException;
} 