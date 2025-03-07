package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RoutesRegistrationApplicationListenerTest {

    @Mock
    private RouteAnnotationProcessor routeAnnotationProcessor;
    @Mock
    private RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    public void testRegisterRoutes() throws Exception {
        RoutesRegistrationApplicationListener routesRegistrationApplicationListener
                = new RoutesRegistrationApplicationListener(routeAnnotationProcessor, routesRestRegistrationProcessor);
        final ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
        Set<RouteEntry> fakeCollection = new HashSet<>();
        fakeCollection.add(mock(RouteEntry.class));

        when(routeAnnotationProcessor.scanForRoutes()).thenReturn(Collections.emptySet());
        routesRegistrationApplicationListener.onApplicationEvent(event);

        when(routeAnnotationProcessor.scanForRoutes()).thenReturn(fakeCollection);
        routesRegistrationApplicationListener.onApplicationEvent(event);

        verify(routesRestRegistrationProcessor, times(1))
                .postRoutes(anySet());
    }

}
