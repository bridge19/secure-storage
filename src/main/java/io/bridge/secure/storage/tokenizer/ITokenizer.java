package io.bridge.secure.storage.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ITokenizer {
  List<String> parse(String content);

  default List<String> findGroupValues(String content,Pattern... patterns){
    List<String> result = new ArrayList<>();
    result.add(content);
    for(Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(content);
      if (matcher.find()) {
        for (int i = 0; i < matcher.groupCount(); i++) {
          result.add(content.substring(matcher.start(i + 1), matcher.end(i + 1)));
        }
      }
    }
    return result;
  }
}
