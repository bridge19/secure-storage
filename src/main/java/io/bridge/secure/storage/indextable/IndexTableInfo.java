package io.bridge.secure.storage.indextable;

import lombok.Data;

@Data
public class IndexTableInfo {
  private String refTableName;
  private String refTableIdColumnName;
  private String tableName;
  private String columnName;
}
