    CREATE table table3(
                           data1 text,
                           data2 text,
                           value1 int,
                           received_time timestamp,

                           primary key ((data1, data2), value1, received_time)
    );

    CREATE table table4(
                           data1 text,
                           data2 text,
                           value1 int,
                           received_time timestamp,

                           primary key ((data1, data2), value1, received_time)
    );

    CREATE table table5(
                           data1 text,
                           data2 text,
                           value1 int,
                           received_time timestamp,

                           primary key ((data1, data2), value1, received_time)
    );