package wanda.web.chatgpt;

class Request
{
  public Request(String model, String prompt, float temperature, int max_tokens)
    {
      this.model = model;
      this.messages = new RequestMessage[] { new RequestMessage(prompt)
      };
      this.temperature = temperature;
      this.max_tokens = max_tokens;
    }

  public final String           model;
  public final RequestMessage[] messages;
  public final float            temperature;
  public final int              max_tokens;
}