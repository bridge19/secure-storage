package io.bridge.secure.storage.tokenizer;

import java.util.List;

public interface ITokenizer {
  List<String> parse(String content);
}
