    CREATE table table1(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_ttl_for_ak/>;

    CREATE table table2(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_ttl_for_ak/>
    <@fn.add_compaction_for_c/>;

    CREATE table table3(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_ttl_for_ak/>;