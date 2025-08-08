package org.qubership.core.scheduler.po;

import com.fasterxml.jackson.core.type.TypeReference;
import org.qubership.core.scheduler.helpers.MyTestObject;
import org.qubership.core.scheduler.helpers.SchedulerUtils;
import org.qubership.core.scheduler.po.repository.ContextRepository;
import org.qubership.core.scheduler.po.repository.VersionMismatchException;
import org.qubership.core.scheduler.po.repository.impl.ContextRepositoryImpl;
import org.qubership.core.scheduler.po.serializers.CustomSerializer;
import org.qubership.core.scheduler.po.serializers.ExtendedSerializer;
import org.qubership.core.scheduler.po.serializers.JsonPOSerializer;
import org.qubership.core.scheduler.po.task.NamedTask;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class ContextTest {


    DataSource dataSource;

    @BeforeEach
    public void setup() {
        dataSource = SchedulerUtils.initDatabase();
    }

    @AfterEach
    public void teardown() {
        ((HikariDataSource) dataSource).close();
    }

    @Test
    void testInit() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        Assertions.assertNotNull(repository);
    }

    @Test
    void testPut() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        DataContext context = new DataContext("3");
        context.put("test", "data");
        repository.putContext(context);
        Assertions.assertEquals(0, context.getVersion());
    }

    @Test
    void testGet() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        DataContext context = new DataContext("3");
        context.put("test", "data");
        repository.putContext(context);
        DataContext context1 = repository.getContext("3");
        Assertions.assertEquals(context1, context);
    }

    @Test
    void testConflict() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        DataContext context = new DataContext("3");
        context.put("test", "data");
        repository.putContext(context);
        DataContext context1 = repository.getContext("3");
        Assertions.assertEquals(context1, context);
        context.put("1", 2);
        repository.putContext(context);
        context1.put("2", 3);
        Assertions.assertThrows(VersionMismatchException.class, () -> repository.putContext(context1));
    }


    @SneakyThrows
    @Test
    void serializationTest() {
        DataContext context = new DataContext("1");
        context.setVersion(10);
        context.put("TestDate", Calendar.getInstance().getTime());
        context.put("a",null);
        MyTestObject d = new MyTestObject();
        d.setData("x");
        context.put("xx", d);
        ExtendedSerializer serializer = new JsonPOSerializer();
        byte[] s = serializer.serialize(context);

        context = serializer.deserialize(DataContext.class, s);
        List<NamedTask> tasks = new ArrayList<>();
        tasks.add(new NamedTask("x", "y"));
        tasks = serializer.deserialize(new TypeReference<List<NamedTask>>() {
        }, serializer.serialize(tasks));
        tasks.forEach(t -> System.out.println(t.getTaskName()));

        serializer = new CustomSerializer();
        tasks = serializer.deserialize(new TypeReference<List<NamedTask>>() {
        }, serializer.serialize(tasks));
        tasks.forEach(t -> System.out.println(t.getTaskName()));
        Assertions.assertNotNull(context);
    }

}
