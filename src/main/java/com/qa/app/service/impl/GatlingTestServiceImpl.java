package com.qa.app.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.qa.app.dao.api.IGatlingTestDao;
import com.qa.app.dao.impl.GatlingTestDaoImpl;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.model.Endpoint;
import com.qa.app.service.runner.GatlingTestRunner;
import com.qa.app.model.BodyTemplate;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.api.IBodyTemplateService;
import com.qa.app.service.api.IHeadersTemplateService;
import com.qa.app.service.EnvironmentContext;

public class GatlingTestServiceImpl implements IGatlingTestService {

    private final IGatlingTestDao testDao = new GatlingTestDaoImpl(); // In a real app, use dependency injection
    private final IEndpointService endpointService = new EndpointServiceImpl();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();

    /* -------------------------------------------------
     *  Helper functional interfaces & utility wrappers
     * ------------------------------------------------- */
    @FunctionalInterface
    private interface SqlCallable<T> {
        T call() throws SQLException;
    }

    private <T> T withSql(SqlCallable<T> action, String errMsg) throws ServiceException {
        try {
            return action.call();
        } catch (SQLException e) {
            throw new ServiceException(errMsg + ": " + e.getMessage(), e);
        }
    }


    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void validateTestBasicFields(GatlingTest test) throws ServiceException {
        if (test == null || isBlank(test.getTcid()) || isBlank(test.getSuite()) || isBlank(test.getEndpointName())) {
            throw new ServiceException("Test validation failed: TCID, Suite, and Endpoint Name are required.");
        }
    }

    @Override
    public void createTest(GatlingTest test) throws ServiceException {
        validateTestBasicFields(test);

        try {
            GatlingTest existing = testDao.getTestByTcid(test.getTcid());
            if (existing != null) {
                throw new ServiceException("Test with TCID '" + test.getTcid() + "' already exists.");
            }
            testDao.addTest(test);
        } catch (SQLException e) {
            throw new ServiceException("Database error while creating test: " + e.getMessage(), e);
        }
    }

    @Override
    public GatlingTest findTestById(int id) throws ServiceException {
        return withSql(() -> testDao.getTestById(id), "Database error while finding test by ID");
    }

    @Override
    public GatlingTest findTestByTcid(String tcid) throws ServiceException {
        return withSql(() -> testDao.getTestByTcid(tcid), "Database error while finding test by TCID");
    }

    @Override
    public List<GatlingTest> findAllTests() throws ServiceException {
        return withSql(testDao::getAllTests, "Database error while retrieving all tests");
    }

    @Override
    public List<GatlingTest> findAllTestsByProjectId(Integer projectId) throws ServiceException {
        return withSql(() -> testDao.getTestsByProjectId(projectId), "Database error while retrieving tests by projectId");
    }

    @Override
    public List<GatlingTest> findTestsBySuite(String suite) throws ServiceException {
        return withSql(() -> testDao.getTestsBySuite(suite), "Database error while finding tests by suite");
    }

    @Override
    public void updateTest(GatlingTest test) throws ServiceException {
        try {
            if (test == null || test.getId() <= 0 ||
                test.getTcid() == null || test.getTcid().trim().isEmpty() ||
                test.getSuite() == null || test.getSuite().trim().isEmpty() ||
                (test.getEndpointName() == null || test.getEndpointName().trim().isEmpty())) {
                throw new ServiceException("Test validation failed: ID, TCID, Suite, and Endpoint Name are required.");
            }
            GatlingTest existingTest = testDao.getTestById(test.getId());
            if (existingTest == null) {
                throw new ServiceException("Test with ID " + test.getId() + " not found.");
            }
            if (!existingTest.getTcid().equals(test.getTcid())) {
                GatlingTest testWithSameTcid = testDao.getTestByTcid(test.getTcid());
                if (testWithSameTcid != null && testWithSameTcid.getId() != test.getId()) {
                    throw new ServiceException("Test with TCID '" + test.getTcid() + "' already exists.");
                }
            }
            testDao.updateTest(test);
        } catch (SQLException e) {
            throw new ServiceException("Database error while updating test: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeTest(int id) throws ServiceException {
        try {
            GatlingTest existing = testDao.getTestById(id);
            if (existing == null) {
                throw new ServiceException("Test with ID " + id + " not found.");
            }
            testDao.deleteTest(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while removing test: " + e.getMessage(), e);
        }
    }

    @Override
    public void toggleTestRunStatus(int id) throws ServiceException {
        try {
            GatlingTest t = testDao.getTestById(id);
            if (t == null) {
                throw new ServiceException("Test with ID " + id + " not found.");
            }
            testDao.updateTestRunStatus(id, !t.isEnabled());
        } catch (SQLException e) {
            throw new ServiceException("Database error while toggling test run status: " + e.getMessage(), e);
        }
    }

    public void runTests(java.util.List<GatlingTest> tests, GatlingLoadParameters params) throws ServiceException {
        runTests(tests, params, null);
    }

    @Override
    public void runTests(java.util.List<GatlingTest> tests, GatlingLoadParameters params, Runnable onComplete) throws ServiceException {
        if (tests == null || tests.isEmpty()) {
            throw new ServiceException("Test list cannot be null or empty.");
        }

        // ① Expand dependencies and capture extra meta (origin, mode)
        ExpandedResult expanded = expandTestsWithDependenciesForRun(tests);
        List<GatlingTest> executionList = expanded.tests;

        java.util.List<Endpoint> endpoints = new java.util.ArrayList<>();
        for (GatlingTest test : executionList) {
            enrichTemplates(test);

            Endpoint endpoint = null;
            try {
                Integer envId = EnvironmentContext.getCurrentEnvironmentId();
                endpoint = endpointService.getEndpointByNameAndEnv(test.getEndpointName(), envId);

            } catch (Exception ex) {
                throw new ServiceException("Error retrieving endpoint for test: " + test.getTcid() + ": " + ex.getMessage(), ex);
            }

            if (endpoint == null) {
                throw new ServiceException("Endpoint '" + test.getEndpointName() + "' not found in current environment for test: " + test.getTcid());
            }

            endpoints.add(endpoint);
        }

        try {
            GatlingTestRunner.executeBatch(executionList, params, endpoints,
                    expanded.origins, expanded.modes, onComplete);

        } catch (Exception e) {
            throw new ServiceException("Failed to run Gatling batch tests: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GatlingTest> findTestsByProjectId(Integer projectId) throws ServiceException {
        try {
            return testDao.getTestsByProjectId(projectId);
        } catch (SQLException e) {
            throw new ServiceException("Database error while retrieving tests by projectId: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateOrder(List<GatlingTest> tests) throws ServiceException {
        try {
            if (tests == null || tests.isEmpty()) {
                return; // Nothing to update
            }
            testDao.updateOrder(tests);
        } catch (SQLException e) {
            throw new ServiceException("Database error while updating test order: " + e.getMessage(), e);
        }
    }

    private void enrichTemplates(GatlingTest test) {
        try {
            if ((test.getBody() == null || test.getBody().isEmpty()) && test.getBodyTemplateId() > 0) {
                BodyTemplate bt = bodyTemplateService.findBodyTemplateById(test.getBodyTemplateId());
                if (bt != null) test.setBody(bt.getContent());
            }
        } catch (Exception ignored) { }

        try {
            if ((test.getHeaders() == null || test.getHeaders().isEmpty()) && test.getHeadersTemplateId() > 0) {
                HeadersTemplate ht = headersTemplateService.getHeadersTemplateById(test.getHeadersTemplateId());
                if (ht != null) test.setHeaders(ht.getContent());
            }
        } catch (Exception ignored) { }
    }


    /**
     * Parse the Condition string, e.g. "[Setup]TC001,TC002;[Teardown]TC003" -> Map
     */
    private Map<String, java.util.List<String>> parseConditionString(String cond) {
        java.util.Map<String, java.util.List<String>> map = new java.util.HashMap<>();
        if (cond == null || cond.isBlank()) return map;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[(\\w+)]([^;]*)");
        java.util.regex.Matcher m = p.matcher(cond);
        while (m.find()) {
            String prefix = m.group(1);
            String body = m.group(2).trim();
            if (body.isBlank()) continue;
            java.util.List<String> tcids = java.util.Arrays.stream(body.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            map.put(prefix, tcids);
        }
        return map;
    }

    private static class ExpandedResult {
        List<GatlingTest> tests = new java.util.ArrayList<>();
        List<String> origins = new java.util.ArrayList<>();
        List<String> modes = new java.util.ArrayList<>();
    }

    private ExpandedResult expandTestsWithDependenciesForRun(List<GatlingTest> selected) throws ServiceException {
        ExpandedResult res = new ExpandedResult();

        for (GatlingTest main : selected) {
            Map<String, List<String>> condMap = parseConditionString(main.getConditions());

            // 1) Setup
            List<String> setups = condMap.getOrDefault("Setup", java.util.Collections.emptyList());
            for (String tcid : setups) {
                GatlingTest setupTest = findTestByTcid(tcid);
                if (setupTest == null) {
                    throw new ServiceException("Setup test not found: " + tcid + " (required by " + main.getTcid() + ")");
                }
                res.tests.add(setupTest);
                res.origins.add(main.getTcid());
                res.modes.add("SETUP");
            }

            // 2) Main test
            res.tests.add(main);
            res.origins.add(main.getTcid());
            res.modes.add("MAIN");

            // 3) Teardown
            List<String> teardowns = condMap.getOrDefault("Teardown", java.util.Collections.emptyList());
            for (String tcid : teardowns) {
                GatlingTest teardownTest = findTestByTcid(tcid);
                if (teardownTest == null) {
                    throw new ServiceException("Teardown test not found: " + tcid + " (required by " + main.getTcid() + ")");
                }
                res.tests.add(teardownTest);
                res.origins.add(main.getTcid());
                res.modes.add("TEARDOWN");
            }
        }
        return res;
    }

}