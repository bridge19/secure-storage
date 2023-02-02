package io.bridge.secure.storage.tokenizer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.cfg.DefaultConfig;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class DefaultTokenizer implements ITokenizer{
  private static Configuration m_wordCut_cfg = DefaultConfig.getInstance();;

  @Override
  public List<String> parse(String content) {
    StringReader sr = new StringReader(content);
    IKSegmenter ikSegmenter = new IKSegmenter(sr, false);
    Lexeme word = null;
    String w = null;
    Set<String> result = new HashSet<>();
    while(true){
      try {
        if (!((word = ikSegmenter.next()) != null)) break;
      } catch (IOException e) {
        log.warn(String.format("parse content [%s] error",content),e);
      }
      w = word.getLexemeText();
      result.add(w);
    }
    result.add(content);
    log.info("original content [{}], parse result [{}]",content, String.join(",",result));
    return new ArrayList<>(result);
  }

  public static void main(String[] args) {
    DefaultTokenizer defaultTokenizer = new DefaultTokenizer();
    defaultTokenizer.parse("李晓晓");
  }
}
