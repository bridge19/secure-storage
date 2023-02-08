package io.bridge.secure.storage.plugin.processor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FussySearchInfo {
  private String tableName;
  private String tableAlias;
  private String columnName;
}
