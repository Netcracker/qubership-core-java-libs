create table scheduled_tasks
(
    task_name            varchar(100),
    task_instance        varchar(100),
    task_data            blob,
    execution_time       TIMESTAMP WITH TIME ZONE,
    picked               BIT,
    picked_by            varchar(50),
    last_success         TIMESTAMP WITH TIME ZONE,
    last_failure         TIMESTAMP WITH TIME ZONE,
    consecutive_failures INT,
    last_heartbeat       TIMESTAMP WITH TIME ZONE,
    version              BIGINT,
    PRIMARY KEY (task_name, task_instance)
);

create table po_context
(
    id           varchar(100),
    context_data blob,
    version      BIGINT,
    PRIMARY KEY (id)
);

create table pe_process_instance
(
    pi_id      varchar(100),
    name       varchar(100),
    def_id     varchar(100),
    state      varchar(20),
    version    BIGINT,
    start_time bigint,
    end_time   bigint,
    PRIMARY KEY (pi_id)
);
create table pe_task_instance
(
    task_id varchar(100),
    def_id  varchar(100),
    pi_id   varchar(100),
    name    varchar(100),
    state   varchar(20),
    type    varchar(1000),
    depends_on blob,
    version BIGINT,
    PRIMARY KEY (task_id)
);

create table pe_process_definition
(
    pd_id   varchar(100),
    version BIGINT,
    PRIMARY KEY (pd_id)
);

create table pe_task_definition
(
    task_name  varchar(255),
    depends_on varchar,
    task_id    varchar(100),
    pd_id      varchar(100),
    version    BIGINT,
    PRIMARY KEY (task_id)
);