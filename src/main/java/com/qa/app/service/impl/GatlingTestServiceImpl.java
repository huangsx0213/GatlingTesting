package com.qa.app.service.impl;

import java.sql.SQLException;
import java.util.List;

import com.qa.app.dao.api.IGatlingTestDao;
import com.qa.app.dao.impl.GatlingTestDaoImpl;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.threadgroups.StandardThreadGroup;
import com.qa.app.model.threadgroups.ThreadGroupType;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.model.Endpoint;
import com.qa.app.util.GatlingTestExecutor;

public class GatlingTestServiceImpl implements IGatlingTestService {

    private final IGatlingTestDao testDao = new GatlingTestDaoImpl(); // In a real app, use dependency injection
    private final IEndpointService endpointService = new EndpointServiceImpl();

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
    public void runTest(GatlingTest test, GatlingLoadParameters params) throws ServiceException {
        if (test == null) {
            throw new ServiceException("Test cannot be null.");
        }
        Endpoint endpoint = endpointService.getEndpointByName(test.getEndpointName());
        if (endpoint == null) {
            throw new ServiceException("Endpoint not found for test.");
        }

        try {
            testDao.updateTestRunStatus(test.getId(), true);
            GatlingTestExecutor.execute(test, params, endpoint);
        } catch (Exception e) {
            throw new ServiceException("Failed to run Gatling test: " + e.getMessage(), e);
        } finally {
            try {
                testDao.updateTestRunStatus(test.getId(), false);
            } catch (SQLException e) {
                // Log this error, but don't re-throw as the primary exception is more important
                System.err.println("Failed to update test run status after execution: " + e.getMessage());
            }
        }
    }

    @Override
    public void runTestSuite(String suite) throws ServiceException {
        try {
            List<GatlingTest> tests = testDao.getTestsBySuite(suite);
            if (tests.isEmpty()) {
                throw new ServiceException("No tests found for suite: " + suite);
            }

            for (GatlingTest test : tests) {
                if (test.isEnabled()) {
                    // This will run with default parameters.
                    GatlingLoadParameters defaultParams = new GatlingLoadParameters();
                    defaultParams.setType(ThreadGroupType.STANDARD);
                    StandardThreadGroup standardThreadGroup = new StandardThreadGroup();
                    standardThreadGroup.setNumThreads(1);
                    standardThreadGroup.setRampUp(0);
                    standardThreadGroup.setDuration(1);
                    defaultParams.setStandardThreadGroup(standardThreadGroup);
                    runTest(test, defaultParams);
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Database error while running test suite: " + e.getMessage(), e);
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
}