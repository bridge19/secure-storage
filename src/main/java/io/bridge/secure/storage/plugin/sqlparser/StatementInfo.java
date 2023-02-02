package io.bridge.secure.storage.plugin.sqlparser;

import io.bridge.secure.storage.indextable.entity.IndexTable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatementInfo {

  private String statementId;
  private String defaultTable = null;
  private Map<String,String> tableAliasMap = new HashMap<>();

  private Map<String, IndexTableValueInfo> indexTableValues = new HashMap<>();

  private String updatedSQL = null;

  public String getDefaultTable() {
    return defaultTable;
  }

  public void setDefaultTable(String defaultTable) {
    this.defaultTable = defaultTable;
  }

  public String getStatementId() {
    return statementId;
  }

  public void setStatementId(String statementId) {
    this.statementId = statementId;
  }

  public void addTableAlias(String tableName,String alias){
    tableAliasMap.putIfAbsent(alias,tableName);
  }

  public String getTableNameByAlias(String alias){
    return tableAliasMap.get(alias);
  }

  public String getUpdatedSQL() {
    return updatedSQL;
  }

  public void setUpdatedSQL(String updatedSQL) {
    this.updatedSQL = updatedSQL;
  }

  public void addIndexTableValueInfo(String idxTableName, IndexTableValueInfo indexTable){
    indexTableValues.put(idxTableName,indexTable);
  }

  public Map<String, IndexTableValueInfo> getIndexTableValues(){
    return indexTableValues;
  }
}
