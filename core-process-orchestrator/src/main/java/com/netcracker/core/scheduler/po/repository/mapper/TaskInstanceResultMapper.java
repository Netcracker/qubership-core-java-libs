package com.netcracker.core.scheduler.po.repository.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.serializers.ExtendedSerializer;
import com.netcracker.core.scheduler.po.task.NamedTask;
import com.netcracker.core.scheduler.po.task.TaskState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TaskInstanceResultMapper implements ResultSetMapper<TaskInstanceImpl> {
    private final ExtendedSerializer serializer;

    public TaskInstanceResultMapper(ExtendedSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskInstanceImpl map(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) return null;
        TaskInstanceImpl taskInstance = new TaskInstanceImpl(
                resultSet.getString("task_id"),
                resultSet.getString("name"),
                resultSet.getString("type"),
                resultSet.getString("pi_id")
        );
        taskInstance.setState(TaskState.fromString(resultSet.getString("state")));
        taskInstance.setVersion(resultSet.getInt("version"));

        taskInstance.setDependsOn(
                serializer.deserialize(new TypeReference<List<NamedTask>>(){}, resultSet.getBytes("depends_on"))
        );
        return taskInstance;
    }
}
