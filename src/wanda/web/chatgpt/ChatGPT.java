package wanda.web.chatgpt;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tilda.utils.ParseUtil;
import tilda.utils.json.JSONUtil;
import wanda.web.config.WebBasics;

public class ChatGPT
  {
    static final Logger LOG = LogManager.getLogger(ChatGPT.class);

    public static void main(String[] args)
    throws Exception
      {
        String prompt = "Que pouvez vous me dire a propos de Prince?";
        String answer = call("main", prompt);
        LOG.debug("Answer: "+answer);
      }

    public static String call(String subConfigName, String prompt)
    throws IOException, InterruptedException, Exception
      {
        String url = WebBasics.getExtra("chatgpt-" + subConfigName, "url");
        String apiKey = WebBasics.getExtra("chatgpt-" + subConfigName, "key");
        String model = WebBasics.getExtra("chatgpt-" + subConfigName, "model");
        float temperature = ParseUtil.parseFloat(WebBasics.getExtra("chatgpt-" + subConfigName, "temperature"), 0.7f);
        int maxTokens = ParseUtil.parseInteger(WebBasics.getExtra("chatgpt-" + subConfigName, "max-tokens"), 150);
        String answerJsonPath = WebBasics.getExtra("chatgpt-" + subConfigName, "answerJsonPath");
        
        return call(prompt, url, apiKey, model, temperature, maxTokens, answerJsonPath);
      }

    public static String call(String prompt, String url, String apiKey, String model, float temperature, int maxTokens, String answerJsonPath)
    throws IOException, InterruptedException, Exception
      {
        Request chatGptRequest = new Request(model, prompt, temperature, maxTokens);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonStr = gson.toJson(chatGptRequest);

        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(jsonStr))
        .build();

        LOG.debug("Sending to ChatGPT");
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200)
          {
            String body = response.body();
            System.out.println(body);
            JsonObject res = new Gson().fromJson(body, JsonObject.class);
            LOG.debug("Extracting answer from path '"+answerJsonPath+"'.");
            JsonElement e = JSONUtil.getJsonElementFromPath(res, answerJsonPath);
            if (e != null && e.isJsonNull() == false && e.isJsonPrimitive() == true)
             return e.getAsString().trim();
            throw new Exception("Cannot extract answer value with path '"+answerJsonPath+"' from response "+res.toString());
          }
        else
          throw new Exception(response.body()+"\nStatusCode: "+response.statusCode());
      }
  }
