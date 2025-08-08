package org.qubership.cloud.dbaas.client.arangodb.service;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.template.ArangoTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ExtendWith(SpringExtension.class)
public class DbaasArangoTemplateTest {

    @Mock
    private ArangoTemplate arangoTemplate;
    private DbaasArangoTemplate dbaasArangoTemplate;

    @BeforeEach
    public void setup() {
        dbaasArangoTemplate = Mockito.mock(DbaasArangoTemplate.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(arangoTemplate).when(dbaasArangoTemplate).getArangoTemplate();
    }

    @Test
    public void testProxiedMethods() throws InvocationTargetException, IllegalAccessException {
        Method[] methods = ArangoOperations.class.getDeclaredMethods();
        for (Method method : methods) {
            Object[] params = new Object[method.getParameterCount()];
            method.invoke(dbaasArangoTemplate, params);
            method.invoke(Mockito.verify(arangoTemplate), params);
        }
    }
}