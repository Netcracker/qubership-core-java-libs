package org.qubership.cloud.quarkus.routesregistration.deployment;

import org.qubership.cloud.quarkus.routesregistration.runtime.RoutesRegistrationRecorder;
import org.qubership.cloud.routesregistration.common.annotation.*;
import org.qubership.cloud.routesregistration.common.annotation.processing.*;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.*;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.logging.Logger;

import jakarta.ws.rs.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

public class RoutesRegistrationProcessor {
    private static final DotName PATH = DotName.createSimple(Path.class.getName());
    private static final DotName ROUTE = DotName.createSimple(Route.class.getName());
    private static final DotName ROUTES = DotName.createSimple(Routes.class.getName());
    private static final DotName FACADE_ROUTE = DotName.createSimple(FacadeRoute.class.getName());
    private static final DotName GATEWAY_REQUEST_MAPPING = DotName.createSimple(Gateway.class.getName());
    private static final DotName FACADE_GATEWAY_REQUEST_MAPPING = DotName.createSimple(FacadeGateway.class.getName());
    private static final String GATEWAY_NAME_PROP = "mesh.gateway.name";
    private static final String GATEWAY_VIRTUAL_HOST_PROP = "mesh.gateway.virtualHosts";

    private static final String FEATURE = "routes-registration";

    private static final Long UNSET_TIMEOUT_VALUE = -1L;
    private static final String TIMEOUT_ANNOTATION_PARAM = "timeout";
    private static final String TYPE_ANNOTATION_PARAM = "type";
    private static final String GATEWAYS_ANNOTATION_PARAM = "gateways";
    private static final String HOSTS_ANNOTATION_PARAM = "hosts";

    private static final Logger LOGGER = Logger.getLogger(RoutesRegistrationProcessor.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RoutesRegistrationBuildItem collectRoutesRegistrations(CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();
        final String microserviceName = getMicroserviceName();
        final RouteHostMapping routeHostMapping = getRouteHostMapping();

        final Map<String, ClassInfo> classesWithRoutes = collectClassesWithRoutes(index);

        final MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();
        classesWithRoutes.values()
                .stream()
                .map(classInfo -> collectRoutesForClass(index, classInfo, microserviceName, routeHostMapping))
                .forEach(microserviceRoutesBuilder::withClass);

        final Collection<RouteEntry> routes = microserviceRoutesBuilder.build();

        LOGGER.info("To Register=" + routes);
        return new RoutesRegistrationBuildItem(routes);
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    public void registerRoutes(RoutesRegistrationRecorder recorder, RoutesRegistrationBuildItem routesRegistrationBuildItem, ApplicationStartBuildItem applicationStartBuildItem) {
        LOGGER.info("Start registerRoutes()");
        recorder.register(routesRegistrationBuildItem.getRouteEntries());
        LOGGER.info("End registerRoutes()");
    }

    private Map<String, ClassInfo> collectClassesWithRoutes(IndexView index) {
        final Map<String, ClassInfo> classesWithRoutes = collectClassesWithAnnotationOfAnyLevel(index, ROUTE);
        classesWithRoutes.putAll(collectClassesWithAnnotationOfAnyLevel(index, ROUTES));
        classesWithRoutes.putAll(collectClassesWithAnnotationOfAnyLevel(index, FACADE_ROUTE));
        return classesWithRoutes;
    }

    private Map<String, ClassInfo> collectClassesWithAnnotationOfAnyLevel(IndexView index, DotName annotationName) {
        Map<String, ClassInfo> classesWithRoutes = new HashMap<>();

        index.getAnnotations(annotationName).forEach(annotationInstance -> {
            ClassInfo classInfo = null;
            if (annotationInstance.target().kind() == Kind.CLASS) {
                classInfo = annotationInstance.target().asClass();
            } else if (annotationInstance.target().kind() == Kind.METHOD) {
                MethodInfo methodInfo = annotationInstance.target().asMethod();
                if (!methodInfo.name().equals("<init>")) {
                    classInfo = methodInfo.declaringClass();
                }
            }
            if (classInfo != null) {
                String className = classInfo.name().toString();
                classesWithRoutes.put(className, classInfo);
            }
        });
        return classesWithRoutes;
    }

    private ClassRoutesBuilder collectRoutesForClass(IndexView index, ClassInfo classInfo, String microserviceName, RouteHostMapping routeHostMapping) {
        ClassRoutesBuilder classRoutesBuilder = new ClassRoutesBuilder(microserviceName, routeHostMapping);

        // collect information about routes based on class-level annotations
        AnnotationInstance routesAnnotation = classInfo.declaredAnnotation(ROUTES);
        if (routesAnnotation != null) {
            for (AnnotationValue annotationValue : routesAnnotation.value().asArrayList()) {
                classRoutesBuilder.withRouteAnnotation(readRouteAnnotation(index, (AnnotationInstance) annotationValue.value()));
            }
        }

        AnnotationInstance routeAnnotation = classInfo.declaredAnnotation(ROUTE);
        if (routeAnnotation != null) {
            classRoutesBuilder.withRouteAnnotation(readRouteAnnotation(index, routeAnnotation));
        }

        AnnotationInstance facadeRouteAnnotation = classInfo.declaredAnnotation(FACADE_ROUTE);
        if (facadeRouteAnnotation != null) {
            classRoutesBuilder.withFacadeRouteAnnotation(readFacadeRouteAnnotation(facadeRouteAnnotation));
        }

        AnnotationInstance pathAnnotation = findAnnotationForClass(PATH, classInfo, index);
        if (pathAnnotation != null) {
            classRoutesBuilder.withPathsTo(readStringArray(pathAnnotation.value()));
        }

        AnnotationInstance gatewayAnnotation = classInfo.declaredAnnotation(GATEWAY_REQUEST_MAPPING);
        if (gatewayAnnotation != null) {
            classRoutesBuilder.withGatewayPathsFrom(readStringArray(gatewayAnnotation.value()));
        }

        AnnotationInstance facadeGatewayAnnotation = classInfo.declaredAnnotation(FACADE_GATEWAY_REQUEST_MAPPING);
        if (facadeGatewayAnnotation != null) {
            classRoutesBuilder.withFacadeGatewayPathsFrom(readStringArray(facadeGatewayAnnotation.value()));
        }

        // add routes based on method-level annotations if they present
        classInfo.methods()
                .stream()
                .filter(methodInfo -> !methodInfo.name().equals("<init>"))
                .filter(methodInfo -> methodInfo.hasAnnotation(ROUTES) || methodInfo.hasAnnotation(ROUTE) || methodInfo.hasAnnotation(FACADE_ROUTE))
                .map(methodInfo -> processMethod(microserviceName, index, methodInfo, routeHostMapping))
                .forEach(classRoutesBuilder::withMethod);

        return classRoutesBuilder;
    }

    private AnnotationInstance findAnnotationForClass(DotName annotationName, ClassInfo classInfo, IndexView index) {
        if (classInfo == null) {
            return null;
        }

        AnnotationInstance pathAnnotation = classInfo.classAnnotation(annotationName);
        if (pathAnnotation == null) {
            pathAnnotation = searchInHierarchy(index, classInfo,
                    classInf -> classInf,
                    annotationTarget -> annotationTarget.asClass().classAnnotation(annotationName));
        }
        return pathAnnotation;
    }

    private AnnotationInstance findAnnotationForMethod(DotName annotationName, MethodInfo methodInfo, IndexView index) {
        if (methodInfo == null) {
            return null;
        }

        AnnotationInstance pathAnnotation = methodInfo.annotation(annotationName);
        if (pathAnnotation == null) {
            pathAnnotation = searchInHierarchy(index, methodInfo.declaringClass(),
                    classInfo -> classInfo.method(methodInfo.name(), methodInfo.parameterTypes().toArray(new Type[0])),
                    annotationTarget -> annotationTarget.asMethod().annotation(annotationName));
        }
        return pathAnnotation;
    }

    private AnnotationInstance searchInHierarchy(IndexView index, ClassInfo classInfo,
                                                 Function<ClassInfo, AnnotationTarget> classInfoMapper,
                                                 Function<AnnotationTarget, AnnotationInstance> annotationMapper) {
        return getListSuperClassesStream(index).apply(classInfo)
                .map(classInfoMapper)
                .filter(Objects::nonNull)
                .map(annotationMapper)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private Function<ClassInfo, Stream<ClassInfo>> getListSuperClassesStream(IndexView index) {
        return cl -> {
            if (cl == null) {
                return Stream.empty();
            }
            Stream<ClassInfo> interfacesStream = cl.interfaceNames().stream()
                    .map(index::getClassByName)
                    .filter(Objects::nonNull);
            Stream<ClassInfo> superInterfacesStream = cl.interfaceNames().stream()
                    .map(index::getClassByName)
                    .filter(Objects::nonNull)
                    .flatMap(classInfo -> getListSuperClassesStream(index).apply(classInfo));

            Stream<ClassInfo> superClassesStream = Stream.empty();
            DotName superClassName;
            ClassInfo superClass;
            if ((superClassName = cl.superName()) != null
                    && (superClass = index.getClassByName(superClassName)) != null) {
                superClassesStream = Stream.concat(Stream.of(superClass), getListSuperClassesStream(index).apply(superClass));
            }

            return Stream.of(interfacesStream, superInterfacesStream, superClassesStream).flatMap(i -> i);
        };
    }

    private MethodRoutesBuilder processMethod(String microserviceName, IndexView index, MethodInfo methodInfo, RouteHostMapping routeHostMapping) {
        MethodRoutesBuilder methodRoutesBuilder = new MethodRoutesBuilder(microserviceName, routeHostMapping);

        AnnotationInstance routesAnnotation = methodInfo.annotation(ROUTES);
        if (routesAnnotation != null) {
            for (AnnotationValue annotationValue : routesAnnotation.value().asArrayList()) {
                methodRoutesBuilder.withRouteAnnotation(readRouteAnnotation(index, (AnnotationInstance) annotationValue.value()));
            }
        }

        AnnotationInstance routeAnnotation = methodInfo.annotation(ROUTE);
        if (routeAnnotation != null) {
            methodRoutesBuilder.withRouteAnnotation(readRouteAnnotation(index, routeAnnotation));
        }

        AnnotationInstance facadeRouteAnnotation = methodInfo.annotation(FACADE_ROUTE);
        if (facadeRouteAnnotation != null) {
            methodRoutesBuilder.withFacadeRouteAnnotation(readFacadeRouteAnnotation(facadeRouteAnnotation));
        }

        AnnotationInstance pathAnnotation = findAnnotationForMethod(PATH, methodInfo, index);
        if (pathAnnotation != null && pathAnnotation.target().kind() == Kind.METHOD) {
            methodRoutesBuilder.withPathsTo(readStringArray(pathAnnotation.value()));
        }

        AnnotationInstance gatewayAnnotation = methodInfo.annotation(GATEWAY_REQUEST_MAPPING);
        if (gatewayAnnotation != null && gatewayAnnotation.target().kind() == Kind.METHOD) {
            methodRoutesBuilder.withGatewayPathsFrom(readStringArray(gatewayAnnotation.value()));
        }

        AnnotationInstance facadeGatewayAnnotation = methodInfo.annotation(FACADE_GATEWAY_REQUEST_MAPPING);
        if (facadeGatewayAnnotation != null && facadeGatewayAnnotation.target().kind() == Kind.METHOD) {
            methodRoutesBuilder.withFacadeGatewayPathsFrom(readStringArray(facadeGatewayAnnotation.value()));
        }

        return methodRoutesBuilder.build();
    }

    private RouteAnnotationInfo readRouteAnnotation(IndexView index, AnnotationInstance routeAnnotation) {
        return RouteAnnotationInfo.builder()
                .routeType(readRouteTypeField(index, routeAnnotation))
                .gateways(readRouteGatewaysField(routeAnnotation))
                .hosts(readRouteHostsField(routeAnnotation))
                .timeout(readRouteTimeoutField(routeAnnotation))
                .build();
    }

    private RouteAnnotationInfo readFacadeRouteAnnotation(AnnotationInstance facadeRouteAnnotation) {
        return RouteAnnotationInfo.builder()
                .routeType(RouteType.FACADE)
                .gateways(readRouteGatewaysField(facadeRouteAnnotation))
                .timeout(readRouteTimeoutField(facadeRouteAnnotation))
                .build();
    }

    private RouteType readRouteTypeField(IndexView index, AnnotationInstance routeAnnotation) {
        RouteType value = RouteType.valueOf(routeAnnotation.valueWithDefault(index).asEnum());
        if (value == RouteType.INTERNAL) {
            return RouteType.valueOf(routeAnnotation.valueWithDefault(index, TYPE_ANNOTATION_PARAM).asEnum());
        }
        return value;
    }

    private Long readRouteTimeoutField(AnnotationInstance routeAnnotation) {
        AnnotationValue timeout = routeAnnotation.value(TIMEOUT_ANNOTATION_PARAM);
        if (timeout == null) {
            return null;
        }
        Long timeoutValue = timeout.asLong();
        if (timeoutValue.longValue() == UNSET_TIMEOUT_VALUE) {
            return null;
        }
        return timeoutValue;
    }

    private Set<String> readRouteGatewaysField(AnnotationInstance routeAnnotation) {
        return readArrayField(routeAnnotation, GATEWAYS_ANNOTATION_PARAM);
    }

    private Set<String> readRouteHostsField(AnnotationInstance routeAnnotation) {
        return readArrayField(routeAnnotation, HOSTS_ANNOTATION_PARAM);
    }

    private Set<String> readArrayField(AnnotationInstance routeAnnotation, String fieldName) {
        AnnotationValue values = routeAnnotation.value(fieldName);
        if (values == null) {
            return null;
        }
        String[] array = readStringArray(values);
        if (RouteAnnotationUtils.isSingleEmptyStringArray(array)) {
            return null;
        }
        Set<String> result = new HashSet<>(array.length);
        Collections.addAll(result, array);
        return result;
    }

    private String[] readStringArray(AnnotationValue value) {
        if (value.kind() == AnnotationValue.Kind.ARRAY) {
            return value.asStringArray();
        } else {
            return new String[]{value.asString()};
        }
    }

    private String getMicroserviceName() {
        return ConfigProvider.getConfig()
                .getValue("cloud.microservice.name", String.class);
    }

    private RouteHostMapping getRouteHostMapping() {
        Optional<String> gatewayName = ConfigProvider.getConfig().getOptionalValue(GATEWAY_NAME_PROP, String.class);
        Optional<List<String>> vHosts = ConfigProvider.getConfig().getOptionalValues(GATEWAY_VIRTUAL_HOST_PROP, String.class);

        if (gatewayName.isPresent() && vHosts.isPresent()) {
            return new RouteHostMapping(gatewayName.get(), vHosts.get());
        }
        return new RouteHostMapping();
    }
}