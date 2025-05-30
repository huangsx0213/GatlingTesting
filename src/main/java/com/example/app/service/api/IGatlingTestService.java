package com.example.app.service.api;

import com.example.app.model.GatlingTest;
import com.example.app.service.ServiceException;

import java.util.List;

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