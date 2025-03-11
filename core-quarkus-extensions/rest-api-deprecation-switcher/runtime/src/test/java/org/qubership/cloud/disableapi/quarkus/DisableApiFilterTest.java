package org.qubership.cloud.disableapi.quarkus;

import org.qubership.cloud.core.error.rest.tmf.TmfErrorResponse;
import org.qubership.cloud.disableapi.quarkus.annotations.AbstractAnnotationsTest;
import io.quarkus.runtime.StartupEvent;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class DisableApiFilterTest {


    @Test
    void testFeatureDisabled() {
        Optional<Boolean> disabled = Optional.of(false);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        FeatureContext context = Mockito.mock(FeatureContext.class);
        Class<AbstractAnnotationsTest.ControllerV1> controllerV1Class = AbstractAnnotationsTest.ControllerV1.class;
        Mockito.when(resourceInfo.getResourceClass()).thenAnswer(i -> controllerV1Class);
        Mockito.when(resourceInfo.getResourceMethod())
                .thenAnswer(i -> controllerV1Class.getMethod("apiGet"));

        filter.configure(resourceInfo, context);

        Assertions.assertTrue(filter.pathsMapFromAnnotations.isEmpty());
    }

    @Test
    void testDeprecatedClass() {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        FeatureContext context = Mockito.mock(FeatureContext.class);
        Class<AbstractAnnotationsTest.ControllerV1> controllerV1Class = AbstractAnnotationsTest.ControllerV1.class;
        Mockito.when(resourceInfo.getResourceClass()).thenAnswer(i -> controllerV1Class);
        Mockito.when(resourceInfo.getResourceMethod())
                .thenAnswer(i -> controllerV1Class.getMethod("apiGet"));

        filter.configure(resourceInfo, context);

        Assertions.assertFalse(filter.pathsMapFromAnnotations.isEmpty());
        Assertions.assertEquals(Set.of("GET"), filter.pathsMapFromAnnotations.get("/api/v1/test"));
    }

    @Test
    void testDeprecatedMethod() {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        FeatureContext context = Mockito.mock(FeatureContext.class);
        Class<AbstractAnnotationsTest.ControllerV2> controllerV2Class = AbstractAnnotationsTest.ControllerV2.class;
        Mockito.when(resourceInfo.getResourceClass()).thenAnswer(i -> controllerV2Class);
        Mockito.when(resourceInfo.getResourceMethod())
                .thenAnswer(i -> controllerV2Class.getMethod("apiGet"));

        filter.configure(resourceInfo, context);

        Assertions.assertFalse(filter.pathsMapFromAnnotations.isEmpty());
        Assertions.assertEquals(Set.of("GET"), filter.pathsMapFromAnnotations.get("/api/v2/test"));
    }

    @Test
    void testOnStartupApiEnabled() {
        Optional<Boolean> disabled = Optional.of(false);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        StartupEvent startupEvent = Mockito.mock(StartupEvent.class);

        filter.onStartup(startupEvent);
        Assertions.assertTrue(filter.urlPatternsToHttpMethods.isEmpty());
    }

    @Test
    void testOnStartupApiDisabledConflict() {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);
        filter.pathsMapFromProperties.put("/test", Set.of("GET"));
        filter.pathsMapFromAnnotations.put("/test", Set.of("GET"));
        StartupEvent startupEvent = Mockito.mock(StartupEvent.class);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            filter.onStartup(startupEvent);
        });
    }

    @Test
    void testOnStartupApiDisabledProperties() {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);
        filter.pathsMapFromProperties.put("/test", Set.of("GET"));
        StartupEvent startupEvent = Mockito.mock(StartupEvent.class);

        filter.onStartup(startupEvent);
        Assertions.assertEquals(Set.of("GET"), filter.urlPatternsToHttpMethods.get(new AntPathMatcher("/test")));
    }

    @Test
    void testOnStartupApiDisabledAnnotations() {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);
        filter.pathsMapFromAnnotations.put("/test", Set.of("GET"));
        StartupEvent startupEvent = Mockito.mock(StartupEvent.class);

        filter.onStartup(startupEvent);
        Assertions.assertEquals(Set.of("GET"), filter.urlPatternsToHttpMethods.get(new AntPathMatcher("/test")));
    }

    @Test
    void testFilterWhenApiEnabled() throws IOException {
        Optional<Boolean> disabled = Optional.of(false);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
        filter.filter(context);

        Mockito.verifyNoInteractions(context);
    }

    @Test
    void testFilterWhenApiDisabledMatchingRequest() throws IOException {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
        filter.info = Mockito.mock(UriInfo.class);
        Mockito.when(context.getMethod()).thenReturn("GET");
        Mockito.when(filter.info.getPath()).thenReturn("/test");
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        filter.urlPatternsToHttpMethods = Map.of(new AntPathMatcher("/test"), Set.of("GET"));

        filter.filter(context);

        Mockito.verify(context).abortWith(captor.capture());
        Response response = captor.getValue();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(404, response.getStatus());
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getEntity() instanceof TmfErrorResponse);
        TmfErrorResponse entity = (TmfErrorResponse) response.getEntity();
        Assertions.assertEquals("404", entity.getStatus());
        Assertions.assertEquals("NC-COMMON-2101", entity.getCode());
    }

    @Test
    void testFilterWhenApiDisabledNotMatchingRequest() throws IOException {
        Optional<Boolean> disabled = Optional.of(true);
        Optional<Set<String>> patterns = Optional.of(Collections.emptySet());
        DisableApiFilter filter = new DisableApiFilter(disabled, patterns);

        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);
        filter.info = Mockito.mock(UriInfo.class);
        Mockito.when(context.getMethod()).thenReturn("GET");
        Mockito.when(filter.info.getPath()).thenReturn("/test1");
        filter.urlPatternsToHttpMethods = Map.of(new AntPathMatcher("/test2"), Set.of("GET"));

        filter.filter(context);

        Mockito.verify(context, Mockito.never()).abortWith(Mockito.any(Response.class));
    }
}
