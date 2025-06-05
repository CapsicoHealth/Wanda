---------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------
-- Anonymous User
---------------------------------------------------------------------------------------------------

insert into WANDA.USER ( "refnum", "email", "id", "loginCount", "pswd", "pswdSalt", "pswdCreateTZ", "pswdCreate", "roles", "loginType", "created", "lastUpdated")
                 values (-666,'anonymous','__ANONYMOUS__',0,'--','--'
                             ,'USEa', statement_timestamp(), '{}', 'LO', statement_timestamp(), statement_timestamp())
    on conflict do nothing;
 

---------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------
-- Default Roles
---------------------------------------------------------------------------------------------------

insert into WANDA.ROLE ( "id", "value", "label", "created", "lastUpdated")
                 values ('SA'     , 'Super Admin'  , 'Super Administrator', statement_timestamp(), statement_timestamp())
                       ,('TA'     , 'Tenant Admin' , 'Tenant Admin'       , statement_timestamp(), statement_timestamp())
                       ,('Creator', 'Creator'      , 'Creator'            , statement_timestamp(), statement_timestamp())
                       ,('FU'     , 'File Uploader', 'File Uploader'      , statement_timestamp(), statement_timestamp())
                       ,('GST'    , 'Guest'        , 'Guest'              , statement_timestamp(), statement_timestamp())
    on conflict("id") do update set
        "label" = EXCLUDED."label"
       ,"value" = EXCLUDED."value"
    ;


DO $$
BEGIN

insert into WANDA.AppConfig("appRefnum", "hostName", "label", "seq", "icon", "active")
select "refnum", '', "label", "seq", "icon", "active"
  from WANDA.App
on conflict("appRefnum", "hostName") do nothing
;

ALTER TABLE WANDA.App drop COLUMN IF EXISTS "label" ;
ALTER TABLE WANDA.App drop COLUMN IF EXISTS "seq"   ;
ALTER TABLE WANDA.App drop COLUMN IF EXISTS "icon"  ;
ALTER TABLE WANDA.App drop COLUMN IF EXISTS "active";

EXCEPTION WHEN OTHERS THEN

END; $$
;


update wanda.TourUserClick set type='LLMs'      where type='X' and "tourId" = 'health-buddy-docs';
update wanda.TourUserClick set type='Cohorts'   where type='X' and "tourId" = 'cohort-insights';
update wanda.TourUserClick set type='Textbooks' where type='X' and "tourId" like 'textbooks-CARDIOLOGY%';

