package com.ccms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

import com.ccms.service.service.impl.TransactionServiceimpl;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class CcmsServiceApplicationTests {

    @Autowired
    private ApplicationContext context;
    
    @MockBean
    private WebServer webServerStartStop;

    @Value("${server.port}")
    private int port;

    /**
     * Test that the application context loads correctly.
     */
    @Test
    void contextLoads() {
        // This test will pass if the application context loads without issues.
    }
    /**
     * Test that the Spring Boot application starts without exceptions (alternative).
     * This will run the SpringApplicationBuilder to start the app.
     */
    
    @Test
    void testMainMethod() {
        String[] args = {}; 
        CcmsServiceApplication.main(args);  // Test the main method of the application
    }

    /**
     * Test that the component scanning is working correctly and beans are being loaded.
     */
    @Test
    void componentScanTest() {
        boolean isServiceLoaded = context.getBeansOfType(TransactionServiceimpl.class).size() > 0;
        assertTrue(isServiceLoaded, "Component scan did not load the required beans.");
    }

    /**
     * Test that service discovery (via @EnableDiscoveryClient) is enabled.
     * This ensures that the discovery client beans are present in the context.
     */
    @Test
    void discoveryClientEnabled() {
        boolean discoveryClientBeanPresent = context.containsBeanDefinition("discoveryClient"); // Adjust if necessary
        assertTrue(discoveryClientBeanPresent, "Service discovery client is not enabled.");
    }

    /**
     * Test that application properties are loaded correctly.
     * This ensures that the server port is set correctly in the application.
     */
    @Test
    void testApplicationProperties() {
        assertEquals(8091, port, "The server port is not as expected.");
    }

    /**
     * Test that the Spring Boot version is compatible.
     * This ensures that the version of Spring Boot is as expected.
     */
    @Test
    void testSpringBootVersion() {
        String version = SpringBootVersion.getVersion();
        assertTrue(version.startsWith("3.0"), "Expected Spring Boot version to start with 3.0, but found: " + version);
    }
}
