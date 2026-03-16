CREATE table sample_migration_table_1(
    id uuid,
    text_column text,

    primary key (id)
)
<@fn.add_ttl_for_ak/>;

CREATE table sample_migration_table_2(
    id uuid,
    text_column text,

    primary key (id)
)