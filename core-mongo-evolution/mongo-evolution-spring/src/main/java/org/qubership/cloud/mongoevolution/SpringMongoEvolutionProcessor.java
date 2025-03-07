package org.qubership.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeEntry;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SpringMongoEvolutionProcessor extends AnnotationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class);
    private static final String DEFAULT_PROFILE = "default";
    private static final Class[] SPRING_ENVIRONMENT_PARAMETERS_MATCH = {Environment.class};
    private static final Class[] SPRING_APPLICATION_CONTEXT_PARAMETERS_MATCH = {ApplicationContext.class};

    private final List<String> changeLogsPackages;
    private final List<String> activeProfiles;
    private ConnectionSearchKey connectionSearchKey;
    private MongoClient client;
    private String dbName;
    private static final ConcurrentHashMap<ConnectionSearchKey, List<ChangeEntry>> changeEntriesCache = new ConcurrentHashMap<>();

    /* Possible input arguments for methods annotated with @ChangeSet */
    private SpringDBManagerEntity springDBManagerEntity;

    private Environment environment;
    private Map<String, Object> classNamesAndBeans;

    public SpringMongoEvolutionProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey, String changeLogsBasePackage) {
        this(client, dbName, connectionSearchKey, Collections.singletonList(changeLogsBasePackage), null, null, null);
    }

    public SpringMongoEvolutionProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey, String changeLogsBasePackage,
                                         Environment environment) {
        this(client, dbName, connectionSearchKey, Collections.singletonList(changeLogsBasePackage), environment, null, null);
    }

    public SpringMongoEvolutionProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                                         String changeLogsBasePackage, Environment environment,
                                         Map<String, Object> classNamesAndBeans) {
        this(client, dbName, connectionSearchKey, Collections.singletonList(changeLogsBasePackage), environment, null, classNamesAndBeans);
    }

    public SpringMongoEvolutionProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                                         List<String> changeLogsPackages, Environment environment, SpringDBManagerEntity springDBManagerEntity,
                                         Map<String, Object> classNamesAndBeans) {
        super(client, dbName, connectionSearchKey, changeLogsPackages, springDBManagerEntity);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.changeLogsPackages = changeLogsPackages;

        this.environment = environment;
        this.springDBManagerEntity = (null == springDBManagerEntity) ? new SpringDBManagerEntity() : springDBManagerEntity;
        this.classNamesAndBeans = classNamesAndBeans;
        this.activeProfiles =
                (environment != null && environment.getActiveProfiles() != null
                        && environment.getActiveProfiles().length > 0)
                        ? asList(environment.getActiveProfiles()) : Collections.singletonList(DEFAULT_PROFILE);
    }

    private MongoTemplate getMongoTemplate() {
        if (null == springDBManagerEntity.getMongoTemplate()) {
            springDBManagerEntity.setMongoTemplate(new MongoTemplate(client, dbName));
        }
        return springDBManagerEntity.getMongoTemplate();
    }

    private boolean matchesActiveSpringProfile(AnnotatedElement element) {
        if (element.isAnnotationPresent(Profile.class)) {
            Profile profiles = element.getAnnotation(Profile.class);
            List<String> values = asList(profiles.value());
            return activeProfiles.stream()
                    .filter(values::contains)
                    .collect(Collectors.toList()).size() > 0;
        } else {
            return true; // no-profiled changeset always matches
        }
    }

    private List<Class<?>> filterByActiveProfiles(Collection<Class<?>> annotated) {
        List<Class<?>> filtered = new ArrayList<>();
        for (Class<?> element : annotated) {
            if (matchesActiveSpringProfile(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }


    protected List<Class<?>> fetchChangeLogs() {
        Set<Class<?>> changeLogs = new HashSet<>();
        for (String changeLogsPackage : changeLogsPackages) {
            LOGGER.debug("ChangeLogsPackage:{}", changeLogsPackage);
            Reflections reflections = new Reflections(changeLogsPackage);
            Set<Class<?>> changeLogsFromReflect = reflections.getTypesAnnotatedWith(ChangeLog.class);
            LOGGER.debug("ChangeLogs from reflect:{}", changeLogsFromReflect);
            changeLogs.addAll(changeLogsFromReflect);
        }
        return filterByActiveProfiles(changeLogs)
                .stream()
                .filter(changelog ->
                        changelog.getAnnotation(ChangeLog.class).dbClassifier().equals(connectionSearchKey.getDbClassifier()))
                .collect(Collectors.toList());
    }

    protected Object acquireInstance(Class<?> changelogClass) {
        LOGGER.info("Try to acquire instance of {} to include it in change logs list", changelogClass.getName());
        return Arrays.stream(changelogClass.getConstructors())
                .filter(this::matchSpringEnvironment).findFirst()
                .map(constructor -> {
                    LOGGER.info("Found one constructor appropriate to inject Spring Environment: {}", constructor.getName());
                    try {
                        return (Object) constructor.newInstance(environment);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }).orElseGet(() -> {
                    try {
                        return changelogClass.getConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private boolean matchSpringEnvironment(Constructor<?> constructor) {
        LOGGER.debug("Check if constructor {} is appropriate to inject Spring Environment", constructor.getName());
        return Arrays.equals(SPRING_ENVIRONMENT_PARAMETERS_MATCH, constructor.getParameterTypes());
    }

    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance)
            throws IllegalAccessException, InvocationTargetException, Exception {
        Object result;

        int methodParamsLength = changeSetMethod.getParameterTypes().length;
        if (methodParamsLength > 0
                && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
            LOGGER.debug("Invoking method with MongoDatabase argument: {}", changeSetMethod);
            result = (methodParamsLength == 2 && changeSetMethod.getParameterTypes()[1].equals(Map.class))
                    ? changeSetMethod.invoke(changeLogInstance, getMongoDatabase(), classNamesAndBeans)
                    : changeSetMethod.invoke(changeLogInstance, getMongoDatabase());

        } else if (methodParamsLength > 0
                && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
            LOGGER.debug("method with MongoTemplate argument: {}", changeSetMethod);
            result = (methodParamsLength == 2 && changeSetMethod.getParameterTypes()[1].equals(Map.class))
                    ? changeSetMethod.invoke(changeLogInstance, this.getMongoTemplate(), classNamesAndBeans)
                    : changeSetMethod.invoke(changeLogInstance, this.getMongoTemplate());

        } else if (changeSetMethod.getParameterTypes().length == 0) {
            LOGGER.debug("method with no params: {}", changeSetMethod);
            result = changeSetMethod.invoke(changeLogInstance);

        } else {
            throw new Exception("ChangeSet method " + changeSetMethod +
                    " has wrong arguments list. Please see docs for more info!");
        }

        /* notify that process is alive*/
        this.registerProcessUpdateTime();
        return result;
    }
}
