package com.netcracker.cloud.mongoevolution.java.annotation;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netcracker.cloud.mongoevolution.java.MongoEvolution;
import com.netcracker.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static com.mongodb.client.model.Filters.*;
import static com.netcracker.cloud.mongoevolution.java.annotation.ChangeEntry.*;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * Class to collect annotations
 */
public class AnnotationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class);
    private static final String DEFAULT_PROFILE = "default";

    private final List<String> changeLogsPackages;
    private ConnectionSearchKey connectionSearchKey;
    private MongoClient client;
    private String dbName;
    private static final ConcurrentHashMap<ConnectionSearchKey, List<ChangeEntry>> changeEntriesCache = new ConcurrentHashMap<>();

    /* Possible input arguments for methods annotated with @ChangeSet */
    private DBManagerEntity dbManagerEntity;

    public AnnotationProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                               String changeLogsBasePackage) {
        this(client, dbName, connectionSearchKey, Collections.singletonList(changeLogsBasePackage), null);
    }

    public AnnotationProcessor(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                               List<String> changeLogsPackages, DBManagerEntity dbManagerEntity) {
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.changeLogsPackages = changeLogsPackages;
        this.dbManagerEntity = (null == dbManagerEntity) ? new DBManagerEntity() : dbManagerEntity;
    }

    protected MongoDatabase getMongoDatabase() {
        if (null == dbManagerEntity.getMongoDatabase()) {
            PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
            CodecRegistry codecRegistry = fromProviders(getDefaultCodecRegistry(), pojoCodecProvider);
            dbManagerEntity.setMongoDatabase(client.getDatabase(dbName).withCodecRegistry(codecRegistry));
        }
        return dbManagerEntity.getMongoDatabase();
    }

    private List<Method> filterChangeSetAnnotation(List<Method> allMethods) {
        final List<Method> changesetMethods = new ArrayList<>();
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(ChangeSet.class)) {
                changesetMethods.add(method);
            }
        }
        return changesetMethods;
    }

    protected List<Class<?>> fetchChangeLogs() {
        Set<Class<?>> changeLogs = new HashSet<>();
        for (String changeLogsPackage : changeLogsPackages) {
            Reflections reflections = new Reflections(changeLogsPackage);
            changeLogs.addAll(reflections.getTypesAnnotatedWith(ChangeLog.class));
        }
        return changeLogs
                .stream()
                .filter(changelog ->
                        changelog.getAnnotation(ChangeLog.class).dbClassifier().equals(connectionSearchKey.getDbClassifier()))
                .collect(Collectors.toList());
    }

    private List<Method> fetchChangeSets(final Class<?> type) {
        return filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    }

    private ChangeEntry createChangeEntry(Method changesetMethod, Object changeLogInstance, long version, int orderChangeLog) {
        if (changesetMethod.isAnnotationPresent(ChangeSet.class)) {
            ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);

            return new ChangeEntry(
                    version,
                    orderChangeLog,
                    annotation.order(),
                    changesetMethod,
                    changeLogInstance);
        } else {
            return null;
        }
    }

    protected void registerProcessUpdateTime() {
        MongoCollection<Document> updatesTracker = getMongoDatabase().getCollection(MongoEvolution.TRACKER_COLLECTION);
        MongoEvolution.updateFieldWithMongoCurrentDate(updatesTracker, MongoEvolution.TRACKER_KEY_UPDATE_LAST, null);
    }


    private List<ChangeEntry> getSortedChangeEntries() throws Exception {
        List<ChangeEntry> changesList = new ArrayList<>();
        for (Class<?> changelogClass : this.fetchChangeLogs()) {
            Object changelogInstance = null;
            try {
                ChangeLog changeLogAnnotation = changelogClass.getAnnotation(ChangeLog.class);
                long changelogVersion = changeLogAnnotation.version();
                int changelogOrder = changeLogAnnotation.order();
                changelogInstance = acquireInstance(changelogClass);
                List<Method> changesetMethods = this.fetchChangeSets(changelogInstance.getClass());

                for (Method changesetMethod : changesetMethods) {

                    ChangeEntry entry = this.createChangeEntry(changesetMethod, changelogInstance, changelogVersion, changelogOrder);
                    if (entry != null) {
                        changesList.add(entry);
                    }
                    /* notify that process is alive*/
                    this.registerProcessUpdateTime();
                }
            } catch (Exception e) {
                LOGGER.error("Error during annotations collecting: {}", e);
                throw new Exception(e.getMessage(), e);
            }
        }

        changesList.sort(ChangeEntry.COMPARATOR);

        /* notify that process is alive*/
        this.registerProcessUpdateTime();
        return changesList;
    }

    @NotNull
    protected Object acquireInstance(Class<?> changelogClass) {
        LOGGER.info("Try to acquire instance of {} to include it in change logs list", changelogClass.getName());
        try {
            return changelogClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance)
            throws IllegalAccessException, InvocationTargetException, Exception {
        Object result;

        int methodParamsLength = changeSetMethod.getParameterTypes().length;
        if (methodParamsLength > 0
                && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
            LOGGER.debug("Invoking method with MongoDatabase argument: {}", changeSetMethod);
            result = (methodParamsLength == 2 && changeSetMethod.getParameterTypes()[1].equals(Map.class))
                    ? changeSetMethod.invoke(changeLogInstance, getMongoDatabase())
                    : changeSetMethod.invoke(changeLogInstance, getMongoDatabase());

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

    private List<ChangeEntry> getChangeEntriesFromCache() throws Exception {
        List<ChangeEntry> changeEntries = changeEntriesCache.get(connectionSearchKey);
        LOGGER.debug("ChangeEntry list from cache:{}", changeEntries);
        if (null == changeEntries) {
            changeEntries = this.getSortedChangeEntries();
            LOGGER.debug("ChangeEntry list cache update:{}", changeEntries);
            changeEntriesCache.put(connectionSearchKey, changeEntries);
        }
        return changeEntries;
    }

    public long getMaxChangeLogVersion() throws Exception {
        List<ChangeEntry> changeEntries = getChangeEntriesFromCache();
        LOGGER.debug("ChangeEntry list:{}", changeEntries);
        return (changeEntries.isEmpty()) ? 0 : changeEntries.get(changeEntries.size() - 1).getVersion();
    }

    private boolean entryIsNotPresentInDb(ChangeEntry entry) {
        MongoCollection<Document> collection = getMongoDatabase().getCollection(CHANGELOG_COLLECTION);
        Document doc = collection.find(and(
                gte(KEY_VERSION, entry.getVersion()),
                eq(KEY_CHANGELOGCLASS, entry.getChangeClassName()),
                eq(KEY_CHANGESETMETHOD, entry.getChangeMethodName()))).first();

        if (doc != null) {
            LOGGER.debug("ChangeEntry {} is already present in db: {}", entry, doc.toJson());
            return false;
        }
        return true;
    }

    public void applyChanges(long currentVersion) throws Exception {

        List<ChangeEntry> changeEntries = getChangeEntriesFromCache();

        for (ChangeEntry entry : changeEntries) {

            if (entry.getVersion() > currentVersion && entryIsNotPresentInDb(entry)) {

                try {
                    this.executeChangeSetMethod(entry.getChangeSetMethod(), entry.getChangeLogInstance());
                    entry.saveEntryInChangeLog(getMongoDatabase());
                } catch (Exception e) {
                    throw new Exception("The changeset failed: " + entry, e);
                }
                /* notify that process is alive*/
                this.registerProcessUpdateTime();
            }
        }

    }

    /**
     * Just for testing purpose
     */
    @SuppressWarnings("unused")
    private static void clearCache() {
        changeEntriesCache.clear();
    }
}
