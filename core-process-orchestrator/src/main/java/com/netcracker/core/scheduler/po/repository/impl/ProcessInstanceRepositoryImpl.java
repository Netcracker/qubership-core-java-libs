package com.netcracker.core.scheduler.po.repository.impl;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.repository.ProcessInstanceRepository;
import com.netcracker.core.scheduler.po.repository.VersionMismatchException;
import com.netcracker.core.scheduler.po.repository.mapper.ProcessInstanceResultMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;

public class ProcessInstanceRepositoryImpl extends AbstractRepository implements ProcessInstanceRepository {


    public ProcessInstanceRepositoryImpl(DataSource dataSource) {
        super(dataSource, "pe_process_instance");
    }

    @Override
    public ProcessInstanceImpl getProcess(String id) {
        return jdbcRunner.query(
                "select * from " +
                tableName +
                " where pi_id=?",
                (PreparedStatement p) -> p.setString(1, id),
                new ProcessInstanceResultMapper()
        );
    }

    @Override
    public void putProcessInstance(ProcessInstanceImpl processInstance) {
        Integer version = jdbcRunner.query(
                "select version from " + tableName + " where pi_id=?",
                (PreparedStatement p) -> p.setString(1, processInstance.getId()),
                (ResultSetMapper<Integer>) resultSet -> resultSet.next() ? resultSet.getInt("version") : null
        );
        if (version == null) {
            insertInstance(processInstance);
        } else {
            // The version check must live in the UPDATE itself: a check-then-act
            // compare leaves a window where a concurrent writer slips between the
            // SELECT above and the UPDATE, silently losing one of the writes. The
            // in-memory version is bumped only after the guarded UPDATE succeeded.
            int expectedVersion = processInstance.getVersion();
            if (updateInstance(processInstance, expectedVersion) == 0) {
                throw new VersionMismatchException("Current version are less than saved");
            }
            processInstance.setVersion(expectedVersion + 1);
        }
    }

    private int updateInstance(ProcessInstanceImpl processInstance, int expectedVersion) {
        return jdbcRunner.execute(
                "update " +
                tableName +
                " set " +
                "   state=?," +
                "   start_time=?," +
                "   end_time=?," +
                "   version=?" +
                " where pi_id=? and version=?"
                ,
                (PreparedStatement p) -> {
                    p.setString(1, processInstance.getState().toString());
                    if (processInstance.getStartTime() != null) {
                        p.setLong(2, processInstance.getStartTime().getTime());
                    } else {
                        p.setLong(2, 0L);
                    }
                    if (processInstance.getEndTime() != null) {
                        p.setLong(3, processInstance.getEndTime().getTime());
                    } else p.setLong(3, 0L);
                    p.setInt(4, expectedVersion + 1);

                    p.setString(5, processInstance.getId());
                    p.setInt(6, expectedVersion);
                }
        );
    }

    private void insertInstance(ProcessInstanceImpl processInstance) {
        jdbcRunner.execute(
                "insert into " +
                tableName +
                " (pi_id,name,def_id,version, state,start_time, end_time) values(?,?,?,?,?,?,?)"
                , (PreparedStatement p) -> {
                    p.setString(1, processInstance.getId());
                    p.setString(2, processInstance.getName());
                    p.setString(3, processInstance.getProcessDefinitionID());
                    p.setInt(4, processInstance.getVersion());
                    p.setString(5, processInstance.getState().toString());
                    if (processInstance.getStartTime() != null) {
                        p.setLong(6, processInstance.getStartTime().getTime());
                    } else {
                        p.setLong(6, 0L);
                    }
                    if (processInstance.getEndTime() != null) {
                        p.setLong(7, processInstance.getEndTime().getTime());
                    } else p.setLong(7, 0L);
                }
        );
    }
}
