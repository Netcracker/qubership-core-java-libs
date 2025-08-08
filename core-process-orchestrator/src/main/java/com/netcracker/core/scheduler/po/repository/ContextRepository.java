package org.qubership.core.scheduler.po.repository;

import org.qubership.core.scheduler.po.DataContext;

public interface ContextRepository {

    DataContext getContext(String id);
    void putContext(DataContext context);
}
