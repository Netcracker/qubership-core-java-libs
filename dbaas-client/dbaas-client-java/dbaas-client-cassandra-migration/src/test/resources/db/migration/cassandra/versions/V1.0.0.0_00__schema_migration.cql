CREATE table table0(
                       data1 text,
                       data2 text,
                       value1 int,
                       received_time timestamp,

                       primary key ((data1, data2), value1, received_time)
);