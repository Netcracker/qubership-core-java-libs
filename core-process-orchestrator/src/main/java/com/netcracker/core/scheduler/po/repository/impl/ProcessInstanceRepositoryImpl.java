package org.qubership.core.scheduler.po.repository.impl;

import com.github.kagkarlsson.jdbc.ResultSetMapper;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.repository.ProcessInstanceRepository;
import org.qubership.core.scheduler.po.repository.VersionMismatchException;
import org.qubership.core.scheduler.po.repository.mapper.ProcessInstanceResultMapper;

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
                        if (processInstance.getStartTime() != null)
                            p.setLong(6, processInstance.getStartTime().getTime());
                        else
                            p.setLong(6, 0L);
                        if (processInstance.getEndTime() != null)
                            p.setLong(7, processInstance.getEndTime().getTime());
                        else p.setLong(7, 0L);
                    }
            );
        } else {
            if (version.equals(processInstance.getVersion())) {
                processInstance.setVersion(version + 1);

                jdbcRunner.execute(
                        "update " +
                                tableName +
                                " set " +
                                "   state=?," +
                                "   start_time=?," +
                                "   end_time=?," +
                                "   version=?" +
                                " where pi_id=?"
                        ,
                        (PreparedStatement p) -> {
                            p.setString(1, processInstance.getState().toString());
                            if (processInstance.getStartTime() != null)
                                p.setLong(2, processInstance.getStartTime().getTime());
                            else
                                p.setLong(2, 0L);
                            if (processInstance.getEndTime() != null)
                                p.setLong(3, processInstance.getEndTime().getTime());
                            else p.setLong(3, 0L);
                            p.setInt(4, processInstance.getVersion());

                            p.setString(5, processInstance.getId());
                        }
                );

            } else throw new VersionMismatchException("Current version are less than saved");

        }
    }


}
