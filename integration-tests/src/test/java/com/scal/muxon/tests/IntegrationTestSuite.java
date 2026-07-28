package com.scal.muxon.tests;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests Suite")
@SelectPackages({
    "com.scal.muxon.tests.deploy",
    "com.scal.muxon.tests.system",
    "com.scal.muxon.tests.tenant",
    "com.scal.muxon.tests.vm"
})
@IncludeClassNamePatterns(".*Tests?")
public class IntegrationTestSuite {

}
