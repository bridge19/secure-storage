package io.bridge.secure.storage.indextable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.bridge.secure.storage.indextable.entity.IndexTableDeletedValue;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface IndexTableDeletedValueMapper extends BaseMapper<IndexTableDeletedValue> {

  @Update("CREATE TABLE IF NOT EXISTS idx_table_delete_value (" +
          "  `id` bigint NOT NULL AUTO_INCREMENT," +
          "  `table_name` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL," +
          "  `id_value` bigint DEFAULT NULL," +
          "  `create_time` datetime DEFAULT CURRENT_TIMESTAMP," +
          "  PRIMARY KEY (`id`)," +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci")
  void createTable();

  @Select("SELECT table_name, id_value from idx_table_delete_value limit 1000")
  List<IndexTableDeletedValue> selectDeletedTableValues();

  @Select("SELECT table_name, id_value from idx_table_delete_value where id>#{id} limit 1000")
  List<IndexTableDeletedValue> selectDeletedTableValuesById(@Param("id") Long id);
}
