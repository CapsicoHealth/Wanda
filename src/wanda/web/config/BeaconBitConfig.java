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

package wanda.web.config;

import com.google.gson.annotations.SerializedName;

import wanda.web.BeaconBit;

public class BeaconBitConfig
  {
    
    public enum LookbackUnit { DAY, HOUR, MINUTE };
    
    /*@formatter:off*/
    @SerializedName("className" ) public String       _className  = null;
    @SerializedName("lookback"  ) public int          _lookback   = 24;
    @SerializedName("timing"    ) public LookbackUnit _timing     = LookbackUnit.HOUR;
    /*@formatter:on*/
    
    public transient BeaconBit    _bitObj;
  }
