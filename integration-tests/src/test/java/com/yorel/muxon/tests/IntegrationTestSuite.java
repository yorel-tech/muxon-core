package com.yorel.muxon.tests;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests Suite")
@SelectPackages({
    "com.yorel.muxon.tests.deploy",
    "com.yorel.muxon.tests.system",
    "com.yorel.muxon.tests.tenant",
    "com.yorel.muxon.tests.vm"
})
@IncludeClassNamePatterns(".*Tests?")
public class IntegrationTestSuite {

}
