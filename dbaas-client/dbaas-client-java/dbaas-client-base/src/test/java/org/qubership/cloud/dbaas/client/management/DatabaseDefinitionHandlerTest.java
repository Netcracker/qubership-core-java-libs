package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.annotaion.Role;
import org.qubership.cloud.dbaas.client.entity.test.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatabaseDefinitionHandlerTest {

    @Mock
    private DbaasClient dbaasClient;

    @Test
    public void checkRetryingDefaultRole() {
        TestDbDefinitionProcessor dbDefinitionProcessor = Mockito.mock(TestDbDefinitionProcessor.class);
        when(dbDefinitionProcessor.getSupportedDatabaseType()).thenCallRealMethod();
        List<DatabaseDefinitionProcessor<?>> testDbDefinitionProcessors = Collections.singletonList(dbDefinitionProcessor);

        TestDbClientCreator dbClientCreator = Mockito.mock(TestDbClientCreator.class);
        when(dbClientCreator.getSupportedDatabaseType()).thenCallRealMethod();

        List<DatabaseClientCreator<?,?>> testDbClientCreators = Collections.singletonList(dbClientCreator);
        DatabaseDefinitionHandler databaseDefinitionHandler = new DatabaseDefinitionHandler(Optional.of(testDbDefinitionProcessors),
                Optional.of(testDbClientCreators),
                dbaasClient);

        when(dbaasClient.getOrCreateDatabase(eq(TestDBType.INSTANCE), anyString(), anyMap(), any())).then(invocation -> {
            String capturedRole = invocation.getArgument(3, DatabaseConfig.class).getUserRole();
            Assertions.assertEquals("admin", capturedRole);
            return new TestDatabase();
        });

        databaseDefinitionHandler.applyDefinitionProcess(TestDBType.INSTANCE, DatabaseConfig.builder().build(), Collections.EMPTY_MAP, "test-ns");
    }

    @Test
    public void checkRetryingCustomRole() {
        TestDbDefinitionProcessor testDbDefinitionProcessor = new TestDbDefinitionProcessor() {
            @Override
            @Role("ddl")
            public void process(TestDatabase database) {
            }
        };
        List<DatabaseDefinitionProcessor<?>> testDbDefinitionProcessors = Collections.singletonList(testDbDefinitionProcessor);

        TestDbClientCreator dbClientCreator = Mockito.mock(TestDbClientCreator.class);
        when(dbClientCreator.getSupportedDatabaseType()).thenCallRealMethod();

        List<DatabaseClientCreator<?,?>> testDbClientCreators = Collections.singletonList(dbClientCreator);
        DatabaseDefinitionHandler databaseDefinitionHandler = new DatabaseDefinitionHandler(Optional.of(testDbDefinitionProcessors),
                Optional.of(testDbClientCreators),
                dbaasClient);

        when(dbaasClient.getOrCreateDatabase(eq(TestDBType.INSTANCE), anyString(), anyMap(), any())).then(invocation -> {
            String capturedRole = invocation.getArgument(3, DatabaseConfig.class).getUserRole();
            Assertions.assertEquals("ddl", capturedRole);
            return new TestDatabase();
        });

        databaseDefinitionHandler.applyDefinitionProcess(TestDBType.INSTANCE, DatabaseConfig.builder().build(), Collections.EMPTY_MAP, "test-ns");
    }

    @Test
    public void checkExceptionWhenDbaasClientCreatorNotFound() {
        TestDbDefinitionProcessor dbDefinitionProcessor = Mockito.mock(TestDbDefinitionProcessor.class);
        when(dbDefinitionProcessor.getSupportedDatabaseType()).thenCallRealMethod();
        List<DatabaseDefinitionProcessor<?>> testDbDefinitionProcessors = Collections.singletonList(dbDefinitionProcessor);

        DatabaseDefinitionHandler databaseDefinitionHandler = new DatabaseDefinitionHandler(Optional.of(testDbDefinitionProcessors),
                Optional.empty(),
                dbaasClient);
        DatabaseConfig databaseConfig = DatabaseConfig.builder().build();
        try {
            databaseDefinitionHandler.applyDefinitionProcess(TestDBType.INSTANCE, databaseConfig, Collections.EMPTY_MAP, "test-ns");
            fail();
        } catch (RuntimeException exception) {
            Assertions.assertEquals("database client creator is not found for: " + TestDBType.INSTANCE.getDatabaseClass().getName(), exception.getMessage());
        }
    }

    @Test
    public void checkRuntimeUserInConfigAfterDefinitionProcess() {
        TestDbDefinitionProcessor dbDefinitionProcessor = Mockito.mock(TestDbDefinitionProcessor.class);
        when(dbDefinitionProcessor.getSupportedDatabaseType()).thenCallRealMethod();
        List<DatabaseDefinitionProcessor<?>> testDbDefinitionProcessors = Collections.singletonList(dbDefinitionProcessor);

        TestDbClientCreator dbClientCreator = Mockito.mock(TestDbClientCreator.class);
        when(dbClientCreator.getSupportedDatabaseType()).thenCallRealMethod();

        List<DatabaseClientCreator<?,?>> testDbClientCreators = Collections.singletonList(dbClientCreator);
        DatabaseDefinitionHandler databaseDefinitionHandler = new DatabaseDefinitionHandler(Optional.of(testDbDefinitionProcessors),
                Optional.of(testDbClientCreators),
                dbaasClient);

        when(dbaasClient.getOrCreateDatabase(eq(TestDBType.INSTANCE), anyString(), anyMap(), any())).thenReturn(new TestDatabase());

        DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("rw").build();
        databaseDefinitionHandler.applyDefinitionProcess(TestDBType.INSTANCE, databaseConfig, Collections.EMPTY_MAP, "test-ns");
        assertEquals("rw", databaseConfig.getUserRole());
    }

    @Test
    public void checkConnectionClosing() throws Exception {
        TestDbDefinitionProcessor dbDefinitionProcessor = Mockito.mock(TestDbDefinitionProcessor.class);
        when(dbDefinitionProcessor.getSupportedDatabaseType()).thenCallRealMethod();
        List<DatabaseDefinitionProcessor<?>> testDbDefinitionProcessors = Collections.singletonList(dbDefinitionProcessor);

        TestDbClientCreator dbClientCreator = Mockito.mock(TestDbClientCreator.class);
        when(dbClientCreator.getSupportedDatabaseType()).thenCallRealMethod();
        TestDBConnection testDBConnection = Mockito.mock(TestDBConnection.class);
        Mockito.doAnswer(invocation -> {
            TestDatabase testDatabase = invocation.getArgument(0, TestDatabase.class);
            testDatabase.setConnectionProperties(testDBConnection);
            return null;
        }).when(dbClientCreator).create(any(TestDatabase.class));

        List<DatabaseClientCreator<?,?>> testDbClientCreators = Collections.singletonList(dbClientCreator);
        DatabaseDefinitionHandler databaseDefinitionHandler = new DatabaseDefinitionHandler(Optional.of(testDbDefinitionProcessors),
                Optional.of(testDbClientCreators),
                dbaasClient);

        when(dbaasClient.getOrCreateDatabase(eq(TestDBType.INSTANCE), anyString(), anyMap(), any())).thenReturn(new TestDatabase());
        DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("rw").build();
        databaseDefinitionHandler.applyDefinitionProcess(TestDBType.INSTANCE, databaseConfig, Collections.EMPTY_MAP, "test-ns");
        verify(testDBConnection).close();
    }

}
