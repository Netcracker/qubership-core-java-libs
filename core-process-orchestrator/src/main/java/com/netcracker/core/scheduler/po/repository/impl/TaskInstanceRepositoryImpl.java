package com.netcracker.core.scheduler.po.repository.impl;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.repository.VersionMismatchException;
import com.netcracker.core.scheduler.po.repository.mapper.TaskInstanceResultMapper;
import com.netcracker.core.scheduler.po.serializers.CustomSerializer;
import com.netcracker.core.scheduler.po.serializers.ExtendedSerializer;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class TaskInstanceRepositoryImpl extends AbstractRepository implements TaskInstanceRepository {

    private final ExtendedSerializer serializer;

    public TaskInstanceRepositoryImpl(DataSource dataSource) {
        this(dataSource, new CustomSerializer());
    }

    public TaskInstanceRepositoryImpl(DataSource dataSource, ExtendedSerializer serializer) {
        super(dataSource, "pe_task_instance");
        this.serializer = serializer;
    }

    @Override
    public void putTaskInstance(TaskInstanceImpl taskInstance) {
        Integer version = jdbcRunner.query(
                "select version from " + tableName + " where task_id=?",
                (PreparedStatement p) -> p.setString(1, taskInstance.getId()),
                (ResultSetMapper<Integer>) resultSet -> resultSet.next() ? resultSet.getInt("version") : null
        );
        if (version == null) {
            jdbcRunner.execute(
                    "insert into " +
                            tableName +
                            " (task_id,name,def_id,version, state,type,pi_id,depends_on) values(?,?,?,?,?,?,?,?)"
                    , (PreparedStatement p) -> {
                        p.setString(1, taskInstance.getId());
                        p.setString(2, taskInstance.getName());
                        p.setString(3, "1");
                        p.setInt(4, taskInstance.getVersion());
                        p.setString(5, taskInstance.getState().toString());
                        p.setString(6, taskInstance.getType());
                        p.setString(7, taskInstance.getProcessID());
                        p.setObject(8, serializer.serialize(taskInstance.getDependsOn()));
                    });
        } else {
            if (version.equals(taskInstance.getVersion())) {
                taskInstance.setVersion(version + 1);
                jdbcRunner.execute(
                        "update " +
                                tableName +
                                " set " +
                                "   state=?," +
                                "   type=?," +
                                "   version=?," +
                                "   name=?," +
                                "   depends_on=?" +
                                " where task_id=?"
                        ,
                        (PreparedStatement p) -> {
                            p.setString(1, taskInstance.getState().toString());
                            p.setString(2, taskInstance.getType());
                            p.setInt(3, taskInstance.getVersion());
                            p.setString(4, taskInstance.getName());
                            p.setObject(5, serializer.serialize(taskInstance.getDependsOn()));
                            p.setString(6, taskInstance.getId());

                        });
            } else throw new VersionMismatchException(String.format("Version in repository: %s, Task Version %s",version,taskInstance.getVersion()));
        }
    }

    @Override
    public void addTaskInstancesBulk(List<TaskInstanceImpl> taskInstances) {
        String query = "insert into " + tableName + " (task_id,name,def_id,version, state,type,pi_id,depends_on) values(?,?,?,?,?,?,?,?)";
        jdbcRunner.executeBatch(query, taskInstances, (taskInstance, p) -> {
            p.setString(1, taskInstance.getId());
            p.setString(2, taskInstance.getName());
            p.setString(3, "1");
            p.setInt(4, taskInstance.getVersion());
            p.setString(5, taskInstance.getState().toString());
            p.setString(6, taskInstance.getType());
            p.setString(7, taskInstance.getProcessID());
            p.setObject(8, serializer.serialize(taskInstance.getDependsOn()));
        });
    }

    @Override
    public TaskInstanceImpl getTaskInstance(String id) {
        return jdbcRunner.query(
                "select * from " +
                        tableName +
                        " where task_id=?",
                (PreparedStatement p) -> p.setString(1, id),
                new TaskInstanceResultMapper(serializer)
        );
    }

    @Override
    public List<TaskInstanceImpl> getProcessTaskInstances(String processId) {
        return jdbcRunner.query(
                "select * from " +
                        tableName +
                        " where pi_id=?",
                (PreparedStatement p) -> p.setString(1, processId),
                (ResultSetMapper<List<TaskInstanceImpl>>) resultSet -> {
                    List<TaskInstanceImpl> tasks = new ArrayList<>();
                    TaskInstanceImpl task;
                    ResultSetMapper<TaskInstanceImpl> rm = new TaskInstanceResultMapper(serializer);
                    do {
                        task = rm.map(resultSet);
                        if (task != null) tasks.add(task);
                    } while (task != null);
                    return tasks;
                }
        );
    }
}
