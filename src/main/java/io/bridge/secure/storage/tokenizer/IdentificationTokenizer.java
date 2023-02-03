package io.bridge.secure.storage.tokenizer;

import java.util.List;
import java.util.regex.Pattern;

public class IdentificationTokenizer implements ITokenizer{

  private Pattern pattern = Pattern.compile("([0-9]{6})([0-9]{8})([0-9]{3}[0-9|X]{1})");
  @Override
  public List<String> parse(String content) {
    return findGroupValues(content,pattern);
  }
}
