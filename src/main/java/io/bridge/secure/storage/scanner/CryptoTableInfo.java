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
  private Boolean logicDelete = Boolean.FALSE;
  private String deleteColumnName;
  private String deleteFieldName;
  private List<String> allEncryptColumns;
  private Map<String, CryptoColumnInfo> cryptoColumnInfoMap;
}
