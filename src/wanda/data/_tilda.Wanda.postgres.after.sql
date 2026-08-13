/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                       ,('API'    , 'API'          , 'API'                , statement_timestamp(), statement_timestamp())
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

