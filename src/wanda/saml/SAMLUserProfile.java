/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
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
package wanda.saml;

public class SAMLUserProfile
  {
    public SAMLUserProfile(String id, String email, String nameFirst, String nameLast, String orgId, String returnUrl)
      {
        _id = id;
        _email = email;
        _nameFirst = nameFirst;
        _nameLast = nameLast;
        _orgId = orgId;
        _returnUrl = returnUrl;
      }

    public final String _id;
    public final String _email;
    public final String _nameFirst;
    public final String _nameLast;
    public final String _orgId;
    public final String _returnUrl;
  }
