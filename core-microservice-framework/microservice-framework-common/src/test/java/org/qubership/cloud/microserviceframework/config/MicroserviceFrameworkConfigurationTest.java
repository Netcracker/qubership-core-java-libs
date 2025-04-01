package org.qubership.cloud.microserviceframework.config;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Locale;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MicroserviceFrameworkConfiguration.class)
public class MicroserviceFrameworkConfigurationTest {

    @Autowired
    private ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource;

    @Test
    public void testLocale(){
        String actualMessage = reloadableResourceBundleMessageSource.getMessage(TEST_ERROR_CODE,null,Locale.US);
        assertEquals(TEST_ERROR_MESSAGE,actualMessage);
        actualMessage = reloadableResourceBundleMessageSource.getMessage(TEST_CODE,null,Locale.US);
        assertEquals(TEST_MESSAGE,actualMessage);
    }

    @Test(expected = NoSuchMessageException.class)
    public void testLocaleCodeNotFound(){
        reloadableResourceBundleMessageSource.getMessage(WRONG_TEST_CODE,null,Locale.US);
    }

}
