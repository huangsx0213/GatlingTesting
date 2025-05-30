package com.example.app.dao.api;

import com.example.app.model.BodyTemplate;
import java.sql.SQLException;
import java.util.List;

public interface IBodyTemplateDao {
    void addTemplate(BodyTemplate template) throws SQLException;
    void updateTemplate(BodyTemplate template) throws SQLException;
    void deleteTemplate(int id) throws SQLException;
    BodyTemplate getTemplateById(int id) throws SQLException;
    BodyTemplate getTemplateByName(String name) throws SQLException;
    List<BodyTemplate> getAllTemplates() throws SQLException;
} 