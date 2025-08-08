package com.netcracker.core.scheduler.po.repository.mapper;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.task.TaskState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class ProcessInstanceResultMapper implements ResultSetMapper<ProcessInstanceImpl> {

    @Override
    public ProcessInstanceImpl map(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) return null;
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl(resultSet.getString("name"), resultSet.getString("pi_id"), resultSet.getString("def_id"));
        processInstance.setState(TaskState.fromString(resultSet.getString("state")));
        long time = resultSet.getLong("start_time");
        processInstance.setStartTime(time == 0L ? null : new Date(time));
        time = resultSet.getLong("end_time");
        processInstance.setEndTime(time == 0L ? null : new Date(time));
        processInstance.setVersion(resultSet.getInt("version"));
        return processInstance;
    }
}
