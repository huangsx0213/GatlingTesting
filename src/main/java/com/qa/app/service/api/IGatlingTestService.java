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
    void runTests(java.util.List<com.qa.app.model.GatlingTest> tests,
                  com.qa.app.model.GatlingLoadParameters params,
                  java.lang.Runnable onComplete) throws ServiceException;
    List<GatlingTest> findTestsByProjectId(Integer projectId) throws ServiceException;
    List<GatlingTest> findAllTestsByProjectId(Integer projectId) throws ServiceException;
}