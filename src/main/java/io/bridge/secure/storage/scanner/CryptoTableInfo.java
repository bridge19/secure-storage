package io.bridge.secure.storage.scanner;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CryptoTableInfo {
  private Class<?> javaClass;
  private String tableName;
  private String idColumnName;
  private String idFieldName;
  private List<String> allEncryptColumns;
  private Map<String, CryptoColumnInfo> cryptoColumnInfoMap;
}
