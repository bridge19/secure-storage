package io.bridge.secure.storage.tokenizer;

import java.util.List;
import java.util.regex.Pattern;

public class MobileTokenizer implements ITokenizer{
  private Pattern pattern = Pattern.compile("1[0-9]{2}([0-9]{4})([0-9]{4})");
  @Override
  public List<String> parse(String content) {
    return findGroupValues(content,pattern);
  }

//  public static void main(String[] args) {
//    MobileTokenizer tokenizer = new MobileTokenizer();
//    List<String> result = tokenizer.parse("13600527223");
//    System.out.println(String.join(",",result));
//  }
}
