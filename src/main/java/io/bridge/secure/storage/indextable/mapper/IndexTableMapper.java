package io.bridge.secure.storage.indextable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@IgnoreEncryption
public interface IndexTableMapper extends BaseMapper<IndexTable> {

  @Update("CREATE TABLE `idx_${tableName}_${columnName}` (" +
        "  `id` bigint NOT NULL AUTO_INCREMENT," +
        "  `${columnName}` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL," +
        "  `${tableName}_id` bigint DEFAULT NULL," +
        "  `deleted` tinyint DEFAULT '0'," +
        "  `create_time` datetime DEFAULT CURRENT_TIMESTAMP," +
        "  PRIMARY KEY (`id`)," +
        "  UNIQUE KEY `IDX_${columnName}_${tableName}_id`(`${columnName}`,`${tableName}_id`)," +
        "  UNIQUE KEY `IDX_${tableName}_id_${columnName}`(`${tableName}_id`,`${columnName}`)" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci")
  void createTable(@Param("tableName") String tableName,
                   @Param("columnName") String columnName);

  @Insert("<script>" +
          "INSERT INTO idx_${tableName}_${columnName}(${columnName},${tableName}_id) value"+
          "<foreach collection='list' item='item' separator=',' >" +
          "(#{item.columnValue},#{item.refTableIdColumnValue})" +
          "</foreach></script>")
  int batchInsert(@Param("tableName") String tableName,
                         @Param("columnName") String columnName,
                         @Param("list") List<IndexTable> values);

  @Insert("SELECT ${tableName}_id from idx_${tableName}_${columnName}"+
          " where ${columnName} = #{columnValue}")
  int selectId(@Param("tableName") String tableName,
                  @Param("columnName") String columnName,
                  @Param("columnValue") String columnValue);



//  public ProcessValueResult constructBatchInsertSQL(ResultSet resultSet, IndexTableDesc indexTableDesc) throws SQLException,Exception{
//    int length = indexTableDesc.getScope().size();
//    IndexTableDesc.IndexTableAndColumnInfo[] valueHandlingIndexTableAndColumnInfo = new IndexTableDesc.IndexTableAndColumnInfo[length];
//    StringBuilder[] stringBuilders = new StringBuilder[length];
//    List<IndexTableDesc.IndexTableAndColumnInfo> indexTableAndColumnInfoList = indexTableDesc.getIndexTableAndColumnInfoList();
//    List<String> initiateIndexTables = indexTableDesc.getScope();
//    int index=0;
//    for (int i = 0, len = indexTableAndColumnInfoList.size(); i < len; i++) {
//      IndexTableDesc.IndexTableAndColumnInfo info = indexTableAndColumnInfoList.get(0);
//      if(initiateIndexTables.contains(info.getTableName())) {
//        valueHandlingIndexTableAndColumnInfo[index] = info;
//        stringBuilders[index++] = new StringBuilder();
//      }
//    }
//
//    for(int i=0;i<length;i++){
//      StringBuilder sb = stringBuilders[i];
//      IndexTableDesc.IndexTableAndColumnInfo info = valueHandlingIndexTableAndColumnInfo[i];
//      sb.append(String.format(insertIdxTableSQL,info.getTableName(),info.getColumnName(),indexTableDesc.getRefTableName()));
//    }
//
//    Long lastRowId = 0L;
//    int rowHandled = 0;
//    ProcessValueResult processValueResult = new ProcessValueResult();
//    while(resultSet.next()){
//      rowHandled++;
//      lastRowId = resultSet.getLong(1);
//      for(int i=0;i<length;i++){
//        StringBuilder sb = stringBuilders[i];
//        IndexTableDesc.IndexTableAndColumnInfo info = valueHandlingIndexTableAndColumnInfo[i];
//        String value = resultSet.getString(i+2);
//        List<String> result = IKTokenizerUtil.parse(value,true);
//        for(String item:result) {
//          sb.append("('").append(item).append("',").append(lastRowId).append("),");
//        }
//      }
//    }
//    processValueResult.setRowHandled(rowHandled);
//    processValueResult.setLastRowId(lastRowId);
//    List<String> insertSQLs = new ArrayList<>();
//    for(int i=0;i<length;i++){
//      insertSQLs.add(stringBuilders[i].substring(0,stringBuilders[i].length()-1));
//    }
//    processValueResult.setInsertSQLs(insertSQLs);
//    return processValueResult;
//  }
//  public ProcessValueResult constructBatchInsertSQL(List<Map<String,Object>> valueList, IndexTableDesc indexTableDesc) throws SQLException,Exception{
//    int length = indexTableDesc.getScope().size();
//    IndexTableDesc.IndexTableAndColumnInfo[] valueHandlingIndexTableAndColumnInfo = new IndexTableDesc.IndexTableAndColumnInfo[length];
//    StringBuilder[] stringBuilders = new StringBuilder[length];
//    List<IndexTableDesc.IndexTableAndColumnInfo> indexTableAndColumnInfoList = indexTableDesc.getIndexTableAndColumnInfoList();
//    List<String> initiateIndexTables = indexTableDesc.getScope();
//    int index=0;
//    for (int i = 0, len = indexTableAndColumnInfoList.size(); i < len; i++) {
//      IndexTableDesc.IndexTableAndColumnInfo info = indexTableAndColumnInfoList.get(0);
//      if(initiateIndexTables.contains(info.getTableName())) {
//        valueHandlingIndexTableAndColumnInfo[index] = info;
//        stringBuilders[index++] = new StringBuilder();
//      }
//    }
//
//    for(int i=0;i<length;i++){
//      StringBuilder sb = stringBuilders[i];
//      IndexTableDesc.IndexTableAndColumnInfo info = valueHandlingIndexTableAndColumnInfo[i];
//      sb.append(String.format(insertIdxTableSQL,info.getTableName(),info.getColumnName(),indexTableDesc.getRefTableName()));
//    }
//
//    Long lastRowId = 0L;
//    int rowHandled = 0;
//    ProcessValueResult processValueResult = new ProcessValueResult();
//    Iterator<Map<String,Object>> valueIt = valueList.iterator();
//    while(valueIt.hasNext()){
//      rowHandled++;
//      Map<String,Object> valueMap = valueIt.next();
//      lastRowId = (Long) valueMap.get(indexTableDesc.getRefTableIdColumnName());
//      for(int i=0;i<length;i++){
//        StringBuilder sb = stringBuilders[i];
//        IndexTableDesc.IndexTableAndColumnInfo info = valueHandlingIndexTableAndColumnInfo[i];
//        String value = (String) valueMap.get(info.getColumnName());
//        List<String> result = IKTokenizerUtil.parse(value,true);
//        for(String item:result) {
//          sb.append("('").append(ObjectUtil.encryptString(item)).append("',").append(lastRowId).append("),");
//        }
//      }
//    }
//    processValueResult.setRowHandled(rowHandled);
//    processValueResult.setLastRowId(lastRowId);
//    List<String> insertSQLs = new ArrayList<>();
//    for(int i=0;i<length;i++){
//      insertSQLs.add(stringBuilders[i].substring(0,stringBuilders[i].length()-1));
//    }
//    processValueResult.setInsertSQLs(insertSQLs);
//    return processValueResult;
//  }
//  public String constructSelectItems(IndexTableDesc indexTableDesc) {
//    List<IndexTableDesc.IndexTableAndColumnInfo> indexTableAndColumnInfoList = indexTableDesc.getIndexTableAndColumnInfoList();
//    List<String> initiateIndexTables = indexTableDesc.getScope();
//    log.info(String.format("======= %d columns value to be indexed =============", initiateIndexTables.size()));
//    StringBuilder sb = new StringBuilder("id");
//    for (int i = 0, len = indexTableAndColumnInfoList.size(); i < len; i++) {
//      IndexTableDesc.IndexTableAndColumnInfo info = indexTableAndColumnInfoList.get(0);
//      if(initiateIndexTables.contains(info.getTableName())) {
//        sb.append(",");
//        sb.append(info.getColumnName());
//      }
//    }
//    return sb.toString();
//  }
//
//  public void insertBatch(Connection conn, String sql) throws SQLException{
//    conn.setAutoCommit(false);
//    Statement statement = conn.createStatement();
//    statement.addBatch(sql);
//    statement.executeBatch();
//    conn.commit();
//  }
}
