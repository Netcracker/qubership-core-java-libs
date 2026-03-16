package com.netcracker.core.scheduler.po.repository.mapper;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.netcracker.core.scheduler.po.DataContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ContextResultMapper implements ResultSetMapper<DataContext> {

    final Serializer serializer;

    public ContextResultMapper(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public DataContext map(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) return null;
        byte[] data = resultSet.getBytes("context_data");
        DataContext context = serializer.deserialize(DataContext.class, data);
        context.setId(resultSet.getString("id"));
        context.setVersion(resultSet.getInt("version"));
        return context;
    }
}
