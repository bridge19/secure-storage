package io.bridge.secure.storage.indextable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import io.bridge.secure.storage.indextable.entity.OriginalTable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@IgnoreEncryption
public interface OriginalTableMapper extends BaseMapper<OriginalTable> {

  @Select("show tables")
  List<String> showAllTables();
  @Select("SELECT ${idColumnName} as id, ${columnName} as columnValue from ${tableName} order by id limit 1000")
  List<OriginalTable> selectOriginalTableValues(@Param("tableName") String tableName,
                                                @Param("idColumnName") String IdColumnName,
                                                @Param("columnName") String columnName);

  @Select("SELECT ${idColumnName} as id, ${columnName} as columnValue from ${tableName} where id>#{id} order by id limit 1000")
  List<OriginalTable> selectOriginalTableValuesById(@Param("tableName") String tableName,
                                                    @Param("idColumnName") String IdColumnName,
                                                    @Param("columnName") String columnName,
                                                    @Param("id") Long id);

  @Select("SELECT ${idColumnName} as id from ${tableName} ")
  List<Long> selectImpactIds (@Param("tableName") String tableName,
                              @Param("idColumnName") String IdColumnName);

}
