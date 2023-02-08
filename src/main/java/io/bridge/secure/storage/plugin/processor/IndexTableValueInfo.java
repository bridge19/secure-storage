package io.bridge.secure.storage.plugin.processor;

import io.bridge.secure.storage.indextable.entity.IndexTable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IndexTableValueInfo {
  private String tableName;
  private String columnName;
  private List<Long> updatedIds = new ArrayList<>();
  private String columnValue;
  private List<IndexTable> values = new ArrayList<>();
}
