---------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------
-- Anonymous User
---------------------------------------------------------------------------------------------------

insert into WANDA.USER ( "refnum", "email", "id", "loginCount", "pswd", "pswdCreateTZ", "pswdCreate", "roles", "loginType", "created", "lastUpdated")
                 values (-666,'anonymous','__ANONYMOUS__',0,'--'
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

