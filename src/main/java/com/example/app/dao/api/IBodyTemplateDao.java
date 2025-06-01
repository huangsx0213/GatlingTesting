package com.example.app.dao.api;

import com.example.app.model.BodyTemplate;
import java.sql.SQLException;
import java.util.List;

public interface IBodyTemplateDao {
    void addBodyTemplate(BodyTemplate template) throws SQLException;
    BodyTemplate getBodyTemplateById(int id) throws SQLException;
    BodyTemplate getBodyTemplateByName(String name) throws SQLException;
    List<BodyTemplate> getAllBodyTemplates() throws SQLException;
    void updateBodyTemplate(BodyTemplate template) throws SQLException;
    void deleteBodyTemplate(int id) throws SQLException;
} 