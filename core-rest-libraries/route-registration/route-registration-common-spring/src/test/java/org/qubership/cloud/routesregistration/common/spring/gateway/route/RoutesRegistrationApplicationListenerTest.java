package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutesRegistrationApplicationListenerTest {

    @Mock
    private RouteAnnotationProcessor routeAnnotationProcessor;
    @Mock
    private RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    void testRegisterRoutes() throws Exception {
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
