    CREATE table table6(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_ttl_for_ak/>;

    CREATE table table7(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_ttl_for_ak/>;

    CREATE table table8(
        data1 text,
        data2 text,
        value1 int,
        received_time timestamp,

        primary key ((data1, data2), value1, received_time)
    )
    <@fn.add_compaction_for_c/>;