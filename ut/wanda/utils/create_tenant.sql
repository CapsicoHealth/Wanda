INSERT INTO tilda.connection(active, id, driver, db, "user", pswd, initial, max, schemas, created, "lastUpdated")
    VALUES (true, 'PepperT1', 'org.postgresql.Driver', 'jdbc:postgresql://localhost/PepperT1?', 'postgres', 'xxx', 1, 10, '{}', current_timestamp, current_timestamp);


INSERT INTO wanda.tenant(refnum, name, description, "loginMsg", "connectionId", active, created, "lastUpdated")
    VALUES(123, 'PepperT1', 'PEPPER T1 PEPPER T1 PEPPER T11', 'Hi from PepperT1', 'PepperT1', true, current_timestamp, current_timestamp);


update tilda.connection set active=false;
update people.tenant set active=false;
update people.tenantuser set active=false;

select * from people.user

select * from people.tenant
select * from people.tenantuser

insert into people.tenantuser (refnum, "userRefnum", "tenantRefnum", "active", "created", "lastUpdated")
   values (1, 56792, 123, true, current_timestamp, current_timestamp)
         ,(2, 7, 123, true, current_timestamp, current_timestamp)
         ,(3, 8, 123, true, current_timestamp, current_timestamp)
         ,(4, 10, 123, true, current_timestamp, current_timestamp)
         ,(5, 66287, 123, true, current_timestamp, current_timestamp)
         ,(6, 56791, 123, true, current_timestamp, current_timestamp)
         ,(7, 9, 123, true, current_timestamp, current_timestamp)
         ,(8, 62539, 123, true, current_timestamp, current_timestamp)
         ,(9, 3, 123, true, current_timestamp, current_timestamp)
         ,(12, 7, 456, true, current_timestamp, current_timestamp)
         ,(13, 8, 456, true, current_timestamp, current_timestamp)
         ,(14, 10, 456, true, current_timestamp, current_timestamp)
         ,(15, 66287, 456, true, current_timestamp, current_timestamp)
         ,(16, 56791, 456, true, current_timestamp, current_timestamp)
;
select * from people.user


