package com.onetattva.infron.tests;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Integration Tests Suite")
@SelectPackages({
    "com.onetattva.infron.tests.deploy",
    "com.onetattva.infron.tests.system",
    "com.onetattva.infron.tests.tenant"
})
@IncludeClassNamePatterns(".*Tests?")
public class IntegrationTestSuite {

}
