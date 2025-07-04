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
import com.qa.app.util.GatlingTestExecutor;
import com.qa.app.model.BodyTemplate;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.api.IBodyTemplateService;
import com.qa.app.service.api.IHeadersTemplateService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.ResponseCheck;


public class GatlingTestServiceImpl implements IGatlingTestService {

    private final IGatlingTestDao testDao = new GatlingTestDaoImpl(); // In a real app, use dependency injection
    private final IEndpointService endpointService = new EndpointServiceImpl();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();

    @Override
    public void createTest(GatlingTest test) throws ServiceException {
        try {
            if (test == null || test.getTcid() == null || test.getTcid().trim().isEmpty() ||
                test.getSuite() == null || test.getSuite().trim().isEmpty() ||
                test.getEndpointName() == null || test.getEndpointName().trim().isEmpty()) {
                throw new ServiceException("Test validation failed: TCID, Suite, and Endpoint Name are required.");
            }
            GatlingTest existingTest = testDao.getTestByTcid(test.getTcid());
            if (existingTest != null) {
                throw new ServiceException("Test with TCID '" + test.getTcid() + "' already exists.");
            }
            testDao.addTest(test);
        } catch (SQLException e) {
            throw new ServiceException("Database error while creating test: " + e.getMessage(), e);
        }
    }

    @Override
    public GatlingTest findTestById(int id) throws ServiceException {
        try {
            return testDao.getTestById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding test by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public GatlingTest findTestByTcid(String tcid) throws ServiceException {
        try {
            return testDao.getTestByTcid(tcid);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding test by TCID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GatlingTest> findAllTests() throws ServiceException {
        try {
            return testDao.getAllTests();
        } catch (SQLException e) {
            throw new ServiceException("Database error while retrieving all tests: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GatlingTest> findTestsBySuite(String suite) throws ServiceException {
        try {
            return testDao.getTestsBySuite(suite);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding tests by suite: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateTest(GatlingTest test) throws ServiceException {
        try {
            if (test == null || test.getId() <= 0 ||
                test.getTcid() == null || test.getTcid().trim().isEmpty() ||
                test.getSuite() == null || test.getSuite().trim().isEmpty() ||
                test.getEndpointName() == null || test.getEndpointName().trim().isEmpty()) {
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
            GatlingTest existingTest = testDao.getTestById(id);
            if (existingTest == null) {
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
            GatlingTest test = testDao.getTestById(id);
            if (test == null) {
                throw new ServiceException("Test with ID " + id + " not found.");
            }
            testDao.updateTestRunStatus(id, !test.isEnabled());
        } catch (SQLException e) {
            throw new ServiceException("Database error while toggling test run status: " + e.getMessage(), e);
        }
    }

    @Override
    public void runTests(java.util.List<GatlingTest> tests, GatlingLoadParameters params, Runnable onComplete) throws ServiceException {
        if (tests == null || tests.isEmpty()) {
            throw new ServiceException("Test list cannot be null or empty.");
        }

        // ① Expand dependencies and capture extra meta (origin, mode)
        ExpandedResult expanded = expandTestsWithDependencies(tests);
        List<GatlingTest> executionList = expanded.tests;

        // ② Mark all response checks as pending for all to-run tests
        markTestsPending(executionList);

        java.util.List<Endpoint> endpoints = new java.util.ArrayList<>();
        for (GatlingTest test : executionList) {
            enrichTemplates(test);
            Endpoint endpoint = endpointService.getEndpointByName(test.getEndpointName());
            if (endpoint == null) {
                throw new ServiceException("Endpoint not found for test: " + test.getTcid());
            }
            endpoints.add(endpoint);
        }

        try {
            // Asynchronous execution, avoid blocking the calling thread (e.g. JavaFX UI thread)
            GatlingTestExecutor.executeBatch(executionList, params, endpoints,
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

    @Override
    public void markTestsPending(List<GatlingTest> tests) throws ServiceException {
        ObjectMapper mapper = new ObjectMapper();
        for (GatlingTest test : tests) {
            try {
                java.util.List<ResponseCheck> list = mapper.readValue(test.getResponseChecks(), new TypeReference<java.util.List<ResponseCheck>>(){});
                for (ResponseCheck rc : list) {
                    rc.setActual("TO_RUN");
                }
                test.setResponseChecks(mapper.writeValueAsString(list));
                testDao.updateTest(test);
            } catch (Exception ex) {
                System.err.println("Failed to mark test " + test.getTcid() + " pending: " + ex.getMessage());
            }
        }
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

    private ExpandedResult expandTestsWithDependencies(List<GatlingTest> selected) throws ServiceException {
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