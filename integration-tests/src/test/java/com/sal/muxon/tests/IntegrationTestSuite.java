package com.sal.muxon.tests;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests Suite")
@SelectPackages({
    "com.sal.muxon.tests.deploy",
    "com.sal.muxon.tests.system",
    "com.sal.muxon.tests.tenant",
    "com.sal.muxon.tests.vm"
})
@IncludeClassNamePatterns(".*Tests?")
public class IntegrationTestSuite {

}
