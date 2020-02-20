drop table address if exists;
drop table person if exists;
create table person ( id integer, first_name varchar, last_name varchar, primary key (id));
create table address ( id integer, street varchar, foreign key (id) references person(id));
shutdown;
