/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tilda.db.Connection;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;

/**
 * This is the application class <B>Data_Assessment</B> mapped to the table <B>WANDA.Assessment</B>.
 * 
 * @see wanda.data._Tilda.TILDA__ASSESSMENT
 */
public class Assessment_Data extends wanda.data._Tilda.TILDA__ASSESSMENT
  {
    protected static final Logger LOG = LogManager.getLogger(Assessment_Data.class.getName());

    public Assessment_Data()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected boolean beforeWrite(Connection C)
    throws Exception
      {
        // Do things before writing the object to disk, for example, take care of AUTO fields.
        return true;
      }

    @Override
    protected boolean afterRead(Connection C)
    throws Exception
      {
        // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
        return true;
      }

    /**
     * updates the Assessment object with a count of questions. It counts all questions in a form
     * that have a name then checks the answers. If the question in the form has a "correct answer"
     * specified, it will count correct answers.
     * 
     * @param formObj
     * @param data
     * @throws Exception
     */
    public void updateStats(JsonObject formObj, String data)
    throws Exception
      {
        if (isNullStartDt() == true)
          setStartDtNow();
        setData(data);

        JsonObject dataObj = JSONUtil.fromJSONObj(data);
        short totalQuestions = 0;
        short totalQuestionsAnswered = 0;
        short totalQuestionsAnsweredCorrectly = 0;
        JsonArray sections = formObj.get("data").getAsJsonObject().get("sections").getAsJsonArray();
        for (int i = 0; i < sections.size(); ++i)
          {
            JsonArray formDefs = sections.get(i).getAsJsonObject().get("formDef").getAsJsonArray();
            for (int j = 0; j < formDefs.size(); ++j)
              {
                JsonObject q = formDefs.get(j).getAsJsonObject();
                JsonElement jsonName = q.get("name");
                String n = jsonName == null || jsonName.isJsonNull() == true ? null : jsonName.getAsString();
                // This must be a valid question element of formDefs
                if (TextUtil.isNullOrEmpty(n) == true)
                  continue;
                ++totalQuestions;
                JsonElement jsonAnswer = dataObj.get(n);
                String a = jsonAnswer == null || jsonAnswer.isJsonNull() == true ? null : jsonAnswer.getAsString();
                if (TextUtil.isNullOrEmpty(a) == true) // if that question is being answered, let's process.
                  continue;
                ++totalQuestionsAnswered;
                JsonElement correctJson = q.get("answer");
                String correctAnswer = correctJson == null || correctJson.isJsonNull() == true ? null : correctJson.getAsString();
                // Some questions can be free-form and count as valid answers. but if a "correct answer" was provider
                // then only count if the answer is indeed correct.
                if (TextUtil.isNullOrEmpty(correctAnswer) == true || a.equals(correctAnswer) == true)
                  ++totalQuestionsAnsweredCorrectly;
              }
          }
        setQTotal(totalQuestions);
        setQCompleted(totalQuestionsAnswered);
        setQCorrect(totalQuestionsAnsweredCorrectly);
        if (totalQuestionsAnswered == totalQuestions)
          setCompletionDtNow();
      }
  }
