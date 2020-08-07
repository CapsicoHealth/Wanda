---------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------
-- Anonymous User
---------------------------------------------------------------------------------------------------

insert into WANDA.USER ( "refnum", "email", "id", "loginCount", "pswd", "pswdCreateTZ", "pswdCreate", "roles", "loginType", "created", "lastUpdated")
                 values (-666,'anonymous','__ANONYMOUS__',0,'--'
                             ,'USEa', statement_timestamp(), '{}', 'LO', statement_timestamp(), statement_timestamp())
    on conflict do nothing;
 
 