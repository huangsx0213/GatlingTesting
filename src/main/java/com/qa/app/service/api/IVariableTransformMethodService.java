package com.qa.app.service.api;

import com.qa.app.service.ServiceException;
import com.qa.app.model.VariableTransformMethod;

import java.util.List;
import java.util.Map;

public interface IVariableTransformMethodService {

    void create(VariableTransformMethod method) throws ServiceException;

    void update(VariableTransformMethod method) throws ServiceException;

    void delete(int id) throws ServiceException;

    VariableTransformMethod findByName(String name) throws ServiceException;

    List<VariableTransformMethod> findAll() throws ServiceException;

    Map<String, VariableTransformMethod> findAllEnabledAsMap() throws ServiceException;

    /**
     * Apply a transform method (built-in or custom) to the given value.
     *
     * @param methodName converter identifier
     * @param value      original value
     * @param params     parameter list (may be empty)
     * @return converted value
     */
    Object apply(String methodName, Object value, List<String> params) throws ServiceException;

    /** Trigger a full reload from database and rebuild cache */
    void reload() throws ServiceException;
} 