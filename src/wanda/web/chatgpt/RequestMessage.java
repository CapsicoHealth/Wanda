package wanda.web.chatgpt;

class RequestMessage
{
  public RequestMessage(String content)
    {
      this.role = "user";
      this.content = content;
    }

  public final String role;
  public final String content;
}