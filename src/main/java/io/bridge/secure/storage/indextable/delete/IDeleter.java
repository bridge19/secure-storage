package io.bridge.secure.storage.indextable.delete;

import java.util.List;

public interface IDeleter {
  void deleteValue();
  void deleteIndexTableValue(String tableName, List<Long> ids);
  void deleteIndexTableValue(String tableName, String columnName, List<Long> ids);
}
