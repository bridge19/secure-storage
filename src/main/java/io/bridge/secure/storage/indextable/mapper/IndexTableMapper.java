package io.bridge.secure.storage.indextable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import org.apache.ibatis.annotations.*;

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

  @Select("SELECT ${tableName}_id from idx_${tableName}_${columnName}"+
          " where ${columnName} = #{columnValue} and deleted=0")
  int selectId(@Param("tableName") String tableName,
                  @Param("columnName") String columnName,
                  @Param("columnValue") String columnValue);
  @Update("<script>"+
          "update idx_${tableName}_${columnName} set deleted=1"+
          " where ${tableName}_id in"+
          "<foreach collection='list' item='item' separator=','  open='(' close=')' >" +
          "#{item}" +
          "</foreach></script>")
  int logicDeleteId(@Param("tableName") String tableName,
               @Param("columnName") String columnName,
               @Param("list") List<Long> ids);

  @Delete("delete idx_${tableName}_${columnName}"+
          " where deleted=1")
  int deleteId(@Param("tableName") String tableName,
                    @Param("columnName") String columnName);
}
