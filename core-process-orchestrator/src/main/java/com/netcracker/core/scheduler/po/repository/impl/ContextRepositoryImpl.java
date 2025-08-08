package com.netcracker.core.scheduler.po.repository.impl;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.github.kagkarlsson.scheduler.serializer.JavaSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.netcracker.core.scheduler.po.DataContext;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.repository.VersionMismatchException;
import com.netcracker.core.scheduler.po.repository.mapper.ContextResultMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.Objects;

public class ContextRepositoryImpl extends AbstractRepository implements ContextRepository {

    private final Serializer serializer;


    public ContextRepositoryImpl(DataSource dataSource) {
        this(dataSource, new JavaSerializer());
    }

    public ContextRepositoryImpl(DataSource dataSource, Serializer serializer) {
        super(dataSource, "po_context");
        this.serializer = serializer;
    }

    @Override
    public DataContext getContext(String id) {
        return jdbcRunner.query(
                "select * from " +
                        tableName +
                        " where id=?",
                (PreparedStatement p) -> p.setString(1, id),
                new ContextResultMapper(serializer)
        );
    }

    @Override
    public void putContext(DataContext context) {
        if (!context.isDirty()) return;

        Integer version = jdbcRunner.query(
                "select version from " + tableName + " where id=?",
                (PreparedStatement p) -> p.setString(1, context.getId()),
                (ResultSetMapper<Integer>) resultSet -> resultSet.next() ? resultSet.getInt("version") : null
        );
        if (version == null) {
            jdbcRunner.execute(
                    "insert into " +
                            tableName +
                            " (id,context_data,version) values(?,?,?)"
                    , (PreparedStatement p) -> {
                        p.setString(1, context.getId());
                        p.setObject(2, serializer.serialize(context));
                        p.setInt(3, context.getVersion());
                    }
            );
            context.setDirty(false);
        } else if (Objects.equals(context.getVersion(), version)) {
            context.setVersion(version + 1);
            jdbcRunner.execute(
                    "update " +
                            tableName +
                            " set " +
                            "version = ?, " +
                            "context_data = ? where id=?"
                    , (PreparedStatement p) -> {
                        p.setString(3, context.getId());
                        p.setObject(2, serializer.serialize(context));
                        p.setInt(1, context.getVersion());
                    }
            );
            context.setDirty(false);
        } else throw new

                VersionMismatchException("Current version are less than saved");
    }
}
