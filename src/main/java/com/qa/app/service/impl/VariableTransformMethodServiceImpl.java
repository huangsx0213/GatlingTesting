package com.qa.app.service.impl;

import com.qa.app.dao.api.IVariableTransformMethodDao;
import com.qa.app.dao.impl.VariableTransformMethodDaoImpl;
import com.qa.app.model.VariableTransformMethod;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableTransformMethodService;
import com.qa.app.service.util.BuiltInVariableConverter;
import com.qa.app.service.util.VariableConverter;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer combining built-in converters and user-defined Groovy converters.
 */
public class VariableTransformMethodServiceImpl implements IVariableTransformMethodService {

    private final IVariableTransformMethodDao dao = new VariableTransformMethodDaoImpl();

    /** Global cache for all converters (key = lower-case method name). */
    private final Map<String, VariableConverter> converterCache = new ConcurrentHashMap<>();

    /** Groovy class loader reused for script compilation. */
    private static final GroovyClassLoader GROOVY_CLASS_LOADER = new GroovyClassLoader();

    public VariableTransformMethodServiceImpl() {
        try {
            reload();
        } catch (ServiceException e) {
            System.err.println("[VariableTransform] Failed to preload converters: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // CRUD operations delegating to DAO
    // ------------------------------------------------------------------------
    @Override
    public void create(VariableTransformMethod method) throws ServiceException {
        try {
            if (dao.getMethodByName(method.getName()) != null) {
                throw new ServiceException("Transform method with name '" + method.getName() + "' already exists");
            }
            dao.addMethod(method);
            reload();
        } catch (Exception e) {
            throw new ServiceException("Error creating transform method: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(VariableTransformMethod method) throws ServiceException {
        try {
            dao.updateMethod(method);
            reload();
        } catch (Exception e) {
            throw new ServiceException("Error updating transform method: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws ServiceException {
        try {
            dao.deleteMethod(id);
            reload();
        } catch (Exception e) {
            throw new ServiceException("Error deleting transform method: " + e.getMessage(), e);
        }
    }

    @Override
    public VariableTransformMethod findByName(String name) throws ServiceException {
        try {
            return dao.getMethodByName(name);
        } catch (Exception e) {
            throw new ServiceException("Error fetching transform method: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VariableTransformMethod> findAll() throws ServiceException {
        try {
            return dao.getAllMethods();
        } catch (Exception e) {
            throw new ServiceException("Error fetching transform methods: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, VariableTransformMethod> findAllEnabledAsMap() throws ServiceException {
        try {
            List<VariableTransformMethod> list = dao.getEnabledMethods();
            Map<String, VariableTransformMethod> map = new HashMap<>();
            for (VariableTransformMethod m : list) {
                map.put(m.getName().toLowerCase(), m);
            }
            return map;
        } catch (Exception e) {
            throw new ServiceException("Error fetching enabled transform methods: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Reload & cache
    // ------------------------------------------------------------------------

    @Override
    public synchronized void reload() throws ServiceException {
        converterCache.clear();
        // 1) built-in
        for (BuiltInVariableConverter b : BuiltInVariableConverter.values()) {
            converterCache.put(b.name().toLowerCase(), b);
        }
        // 2) custom
        Map<String, VariableTransformMethod> customMap = findAllEnabledAsMap();
        customMap.forEach((name, m) -> {
            VariableConverter converter = wrapScript(m);
            converterCache.put(name, converter);
        });
    }

    // ------------------------------------------------------------------------
    // Apply conversion
    // ------------------------------------------------------------------------
    @Override
    public Object apply(String methodName, Object value, List<String> params) throws ServiceException {
        if (methodName == null) {
            return value;
        }
        VariableConverter converter = converterCache.get(methodName.toLowerCase());
        if (converter == null) {
            throw new ServiceException("Unknown transform method: " + methodName);
        }
        try {
            return converter.convert(value, params);
        } catch (Exception e) {
            throw new ServiceException("Error executing transform method '" + methodName + "': " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private VariableConverter wrapScript(VariableTransformMethod m) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Script> cls = (Class<? extends Script>) GROOVY_CLASS_LOADER.parseClass(m.getScript()).asSubclass(Script.class);
            return (value, params) -> {
                Script script = cls.getDeclaredConstructor().newInstance();
                Binding binding = new Binding();
                binding.setVariable("value", value);
                binding.setVariable("params", params);
                script.setBinding(binding);
                return script.run();
            };
        } catch (Exception e) {
            System.err.println("[VariableTransform] Failed to compile script '" + m.getName() + "': " + e.getMessage());
            // fallback converter which returns original value
            return (value, params) -> value;
        }
    }
} 