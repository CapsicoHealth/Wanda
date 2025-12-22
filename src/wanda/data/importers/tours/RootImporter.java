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

package wanda.data.importers.tours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

import tilda.Importer;
import tilda.db.Connection;
import wanda.data.TourPart_Data;
import wanda.data.TourStep_Data;
import wanda.data.TourStep_Factory;
import wanda.data.Tour_Data;

public class RootImporter extends Tour_Data implements Importer
  {
    /*@formatter:off*/
    @SerializedName("parts" ) public List<TourPart_Data> _parts = new ArrayList<TourPart_Data>();
    /*@formatter:on*/

    @Override
    public int process(Connection C)
    throws Exception
      {
        int Count = 0;

        validateOtherTitles("Tour '" + this.getId() + "'", this.getOtherLanguages(), this.getOtherTitles());
        if (this.getDefaultLanguage() == null)
         this.setDefaultLanguage("en");

        if (this.upsert(C) == false)
          throw new Exception("Cannot upsert Tour record '" + this.getId() + "'.");

        short partPos = 0;
        Set<String> partIds = new HashSet<String>();
        for (TourPart_Data p : _parts)
          {
            if (p == null)
              continue;
            if (partIds.add(p.getId()) == false)
              throw new Exception("Duplicate part Id '" + p.getId() + "' for Tour '" + this.getId() + "'.");
            validateOtherTitles("Tour '" + this.getId() + "', TourPart '" + p.getId() + "'", this.getOtherLanguages(), p.getOtherTitles());
            ++Count;
            p.setTourRefnum(this.getRefnum());
            p.setPos(++partPos);
            if (p.upsert(C) == false)
              throw new Exception("Cannot upsert Part '" + p.getId() + "' for Tour '" + this.getId() + "'.");
            short stepPos = 0;
            Set<String> stepIds = new HashSet<String>();
            TourStep_Factory.shiftOutSteps(C, p.getRefnum());
            for (TourStep_Data s : p._steps)
              {
                if (s == null)
                  continue;
                if (stepIds.add(s.getId()) == false)
                  throw new Exception("Duplicate Step '" + s.getId() + "' for Part '" + p.getId() + "', for Tour '" + this.getId() + "'.");
                validateOtherTitles("Tour '" + this.getId() + "', TourPart '" + p.getId() + "', TourStep '" + s.getId() + "'", this.getOtherLanguages(), s.getOtherTitles());
                ++Count;
                s.setTourPartRefnum(p.getRefnum());
                s.setPos(++stepPos);
                // It is possible for parts to be moved around, so we might need to shuffle things around.
                if (s.upsert(C) == false)
                  throw new Exception("Cannot upsert Step '" + s.getId() + "' for Part '" + p.getId() + "', for Tour '" + this.getId() + "'.");
              }
            TourStep_Factory.cleanOldSteps(C, p.getRefnum());
          }

        return Count;
      }

    /**
     * Validate that the Tour, TourPart and TourStep are defining titles that match the languages specified for the whole tour
     * 
     * @param id
     * @param otherLanguages
     * @param otherTitles
     * @return
     */
    private static boolean validateOtherTitles(String id, Iterator<String> otherLanguages, List<MultiLanguageTitle> otherTitles)
      {
        boolean OK = true;
        if (otherLanguages != null)
          while (otherLanguages.hasNext() == true)
            {
              String lang = otherLanguages.next();
              if (check(lang, otherTitles) == false)
                {
                  OK = false;
                  LOG.error("This tour was defined with language '" + lang + "' but there was no title matching for " + id + ".");
                }
            }
        if (otherTitles != null)
          for (MultiLanguageTitle mlt : otherTitles)
            if (check(mlt._lang, otherLanguages) == false)
              {
                OK = false;
                LOG.error(id + " has a title with language '" + mlt._lang + "' which was not defined as a supported language for the Tour.");
              }

        return OK;
      }

    private static boolean check(String otherLanguage, List<MultiLanguageTitle> otherTitles)
      {
        if (otherTitles != null)
          for (MultiLanguageTitle mlt : otherTitles)
            if (mlt._lang.equals(otherLanguage) == true)
              return true;
        return false;
      }

    private static boolean check(String lang, Iterator<String> otherLanguages)
      {
        while (otherLanguages != null && otherLanguages.hasNext() == true)
          if (lang.equals(otherLanguages.next()) == true)
            return true;
        return false;
      }
  }
