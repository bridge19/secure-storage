package io.bridge.secure.storage.indextable.delete;

import io.bridge.secure.storage.indextable.IndexTableInfo;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.indextable.mapper.IndexTableMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

public class IdxTableValueDeleter implements IDeleter{

  @Resource
  private IndexTableMapper indexTableMapper;

  @Transactional
  @Override
  public void deleteValue() {
    IndexTableInfoRepository.allIndexTableName().stream().forEach(indexTableName -> {
      IndexTableInfo info = IndexTableInfoRepository.getIndexTableInfo(indexTableName);
      String tableName = info.getRefTableName();
      String columnName = info.getColumnName();
      indexTableMapper.deleteId(tableName,columnName);
    });
  }

}
