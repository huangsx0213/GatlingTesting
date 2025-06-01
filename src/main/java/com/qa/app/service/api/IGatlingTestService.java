package com.qa.app.service.api;

import java.util.List;

import com.qa.app.model.GatlingTest;
import com.qa.app.service.ServiceException;

public interface IGatlingTestService {
    void createTest(GatlingTest test) throws ServiceException;
    GatlingTest findTestById(int id) throws ServiceException;
    GatlingTest findTestByTcid(String tcid) throws ServiceException;
    List<GatlingTest> findAllTests() throws ServiceException;
    List<GatlingTest> findTestsBySuite(String suite) throws ServiceException;
    void updateTest(GatlingTest test) throws ServiceException;
    void removeTest(int id) throws ServiceException;
    void toggleTestRunStatus(int id) throws ServiceException;
    void runTest(GatlingTest test) throws ServiceException;
    void runTestSuite(String suite) throws ServiceException;
}