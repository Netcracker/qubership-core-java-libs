package com.netcracker.core.scheduler.po.repository;

import com.netcracker.core.scheduler.po.DataContext;

public interface ContextRepository {

    DataContext getContext(String id);
    void putContext(DataContext context);
}
