package com.example.app.service.impl;

import com.example.app.dao.impl.GatlingTestDaoImpl;
import com.example.app.dao.api.IGatlingTestDao;
import com.example.app.model.GatlingTest;
import com.example.app.service.ServiceException;
import com.example.app.service.api.IGatlingTestService;

import java.sql.SQLException;
import java.util.List;

public class GatlingTestServiceImpl implements IGatlingTestService {

    private final IGatlingTestDao testDao = new GatlingTestDaoImpl(); // In a real app, use dependency injection

    @Override
    public void createTest(GatlingTest test) throws ServiceException {
        try {
            // Basic validation
            if (test == null || test.getTcid() == null || test.getTcid().trim().isEmpty() ||
                test.getSuite() == null || test.getSuite().trim().isEmpty() ||
                test.getEndpoint() == null || test.getEndpoint().trim().isEmpty() ||
                test.getHttpMethod() == null || test.getHttpMethod().trim().isEmpty()) {
                throw new ServiceException("Test validation failed: TCID, Suite, Endpoint, and HTTP Method are required.");
            }

            // Check if TCID already exists
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
            // Basic validation
            if (test == null || test.getId() <= 0 ||
                test.getTcid() == null || test.getTcid().trim().isEmpty() ||
                test.getSuite() == null || test.getSuite().trim().isEmpty() ||
                test.getEndpoint() == null || test.getEndpoint().trim().isEmpty() ||
                test.getHttpMethod() == null || test.getHttpMethod().trim().isEmpty()) {
                throw new ServiceException("Test validation failed: ID, TCID, Suite, Endpoint, and HTTP Method are required.");
            }

            // Check if test exists
            GatlingTest existingTest = testDao.getTestById(test.getId());
            if (existingTest == null) {
                throw new ServiceException("Test with ID " + test.getId() + " not found.");
            }

            // Check if TCID is being changed to an existing one
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
    public void runTest(GatlingTest test) throws ServiceException {
        // This is a placeholder for actual test execution logic
        // In a real implementation, this would integrate with Gatling or HTTP client
        try {
            if (test == null) {
                throw new ServiceException("Test cannot be null.");
            }

            // Mark test as running
            testDao.updateTestRunStatus(test.getId(), true);

            // For now, just simulate test execution
            System.out.println("Executing test: " + test.getTcid() + " on endpoint: " + test.getEndpoint());
            
            // Simulate some processing time
            Thread.sleep(test.getWaitTime() * 1000L);
            
            // Mark test as completed (not running)
            testDao.updateTestRunStatus(test.getId(), false);
            
        } catch (SQLException e) {
            throw new ServiceException("Database error while running test: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Test execution was interrupted: " + e.getMessage(), e);
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
                    runTest(test);
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Database error while running test suite: " + e.getMessage(), e);
        }
    }
}