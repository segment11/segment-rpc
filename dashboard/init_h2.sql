create table zk_cluster (
    id int auto_increment primary key,
    name varchar(50),
    des varchar(200),
    connect_string varchar(200),
    prefix varchar(50),
    created_date timestamp,
    updated_date timestamp default current_timestamp
);