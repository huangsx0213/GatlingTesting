package com.qa.app.dao.api;

import java.sql.SQLException;
import java.util.List;

import com.qa.app.model.BodyTemplate;

public interface IBodyTemplateDao {
    void addBodyTemplate(BodyTemplate template) throws SQLException;
    BodyTemplate getBodyTemplateById(int id) throws SQLException;
    BodyTemplate getBodyTemplateByName(String name) throws SQLException;
    List<BodyTemplate> getAllBodyTemplates() throws SQLException;
    void updateBodyTemplate(BodyTemplate template) throws SQLException;
    void deleteBodyTemplate(int id) throws SQLException;
} 