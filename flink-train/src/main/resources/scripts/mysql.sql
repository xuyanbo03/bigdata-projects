create table uesr_domain_config(
id int unsigned auto_increment,
user_id varchar(40) not null,
domain varchar(40) not null,
primary key (id)
);

insert into uesr_domain_config(user_id,domain) values('8000001','v1.awebone.com');
insert into uesr_domain_config(user_id,domain) values('8000002','v2.awebone.com');
insert into uesr_domain_config(user_id,domain) values('8000003','v3.awebone.com');
insert into uesr_domain_config(user_id,domain) values('8000004','v4.awebone.com');
insert into uesr_domain_config(user_id,domain) values('8000005','vmi.awebone.com');

select * from uesr_domain_config;