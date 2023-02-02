package io.bridge.secure.storage.indextable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import io.bridge.secure.storage.indextable.entity.OriginalTable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@IgnoreEncryption
public interface OriginalTableMapper extends BaseMapper<OriginalTable> {

  @Select("SELECT ${IdColumnName} as id, ${columnName} as columnValue from ${tableName} order by id limit 1000")
  List<OriginalTable> selectOriginalTableValues(@Param("tableName") String tableName,
                                                @Param("IdColumnName") String IdColumnName,
                                                @Param("columnName") String columnName);

  @Select("SELECT ${IdColumnName} ad id, ${columnName} as columnValue from ${tableName} where id>#{id} order by id limit 1000")
  List<OriginalTable> selectOriginalTableValuesById(@Param("tableName") String tableName,
                                                    @Param("IdColumnName") String IdColumnName,
                                                    @Param("columnName") String columnName,
                                                    @Param("id") Long id);

}
