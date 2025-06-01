package com.example.app.dao.api;

import com.example.app.model.HeadersTemplate;
import java.sql.SQLException;
import java.util.List;

public interface IHeadersTemplateDao {
    void addHeadersTemplate(HeadersTemplate template) throws SQLException;
    void updateHeadersTemplate(HeadersTemplate template) throws SQLException;
    void deleteHeadersTemplate(int id) throws SQLException;
    HeadersTemplate getHeadersTemplateById(int id) throws SQLException;
    HeadersTemplate getHeadersTemplateByName(String name) throws SQLException;
    List<HeadersTemplate> getAllHeadersTemplates() throws SQLException;
} 