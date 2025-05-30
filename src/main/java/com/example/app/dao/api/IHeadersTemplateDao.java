package com.example.app.dao.api;

import com.example.app.model.HeadersTemplate;
import java.sql.SQLException;
import java.util.List;

public interface IHeadersTemplateDao {
    void addTemplate(HeadersTemplate template) throws SQLException;
    void updateTemplate(HeadersTemplate template) throws SQLException;
    void deleteTemplate(int id) throws SQLException;
    HeadersTemplate getTemplateById(int id) throws SQLException;
    HeadersTemplate getTemplateByName(String name) throws SQLException;
    List<HeadersTemplate> getAllTemplates() throws SQLException;
} 