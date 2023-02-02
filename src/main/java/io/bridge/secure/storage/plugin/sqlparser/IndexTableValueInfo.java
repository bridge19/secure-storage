package io.bridge.secure.storage.plugin.sqlparser;

import io.bridge.secure.storage.indextable.entity.IndexTable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IndexTableValueInfo {
  private String tableName;
  private String columnName;
  private List<IndexTable> values = new ArrayList<>();
}
