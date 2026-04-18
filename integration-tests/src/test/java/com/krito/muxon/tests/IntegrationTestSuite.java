package com.krito.muxon.tests;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests Suite")
@SelectPackages({
    "com.krito.muxon.tests.deploy",
    "com.krito.muxon.tests.system",
    "com.krito.muxon.tests.tenant",
    "com.krito.muxon.tests.vm"
})
@IncludeClassNamePatterns(".*Tests?")
public class IntegrationTestSuite {

}
