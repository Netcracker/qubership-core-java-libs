package com.netcracker.core.scheduler.po.repository;

import com.netcracker.core.scheduler.po.DataContext;

import java.util.List;

public interface ContextRepository {

    DataContext getContext(String id);

    void putContext(DataContext context);

    void addContextsBulk(List<DataContext> contexts);
}
