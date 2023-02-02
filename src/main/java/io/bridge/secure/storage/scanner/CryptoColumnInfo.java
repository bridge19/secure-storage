package io.bridge.secure.storage.scanner;

import io.bridge.secure.storage.enums.Algorithm;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CryptoColumnInfo {
  private boolean fuzzy;
  private String fieldName;
  private String columnName;
  private Class<?> tokenizer;
  private Class<?> cryptor;
}
