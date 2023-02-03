package io.bridge.secure.storage.indextable.delete;

import io.bridge.secure.storage.indextable.entity.IndexTableDeletedValue;
import io.bridge.secure.storage.indextable.mapper.IndexTableDeletedValueMapper;
import io.bridge.secure.storage.indextable.mapper.IndexTableMapper;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdxTableValueDeleter implements IDeleter{

  @Resource
  private IndexTableDeletedValueMapper deletedValueMapper;

  @Resource
  private IndexTableMapper indexTableMapper;

  @Transactional
  @Override
  public void deleteValue() {
    List<IndexTableDeletedValue> deletedValues;
    Long lastRowId = -1L;
    while (true){
      if(lastRowId==-1L) {
        deletedValues = deletedValueMapper.selectDeletedTableValues();
      }else {
        deletedValues = deletedValueMapper.selectDeletedTableValuesById(lastRowId);
      }
      if(CollectionUtils.isEmpty(deletedValues)){
        break;
      }
      Map<String,List<Long>> deletedTableValueInfoMap = new HashMap<>();
      deletedValues.stream().forEach(item->{
        List<Long> idList = deletedTableValueInfoMap.get(item.getTableName());
        if(idList==null){
          idList = new ArrayList<>();
          deletedTableValueInfoMap.put(item.getTableName(),idList);
        }
        idList.add(item.getIdValue());
      });
      deletedTableValueInfoMap.entrySet().forEach(item->{
        String tableName = item.getKey();
        List<Long> ids = item.getValue();
        deleteIndexTableValue(tableName,ids);
      });
      if(deletedValues.size()<1000){
        break;
      }
      lastRowId = deletedValues.get(999).getId();
    }
  }

  @Transactional
  @Override
  public void deleteIndexTableValue(String tableName, List<Long> ids){
      CryptoTableInfo cryptoTableInfo = CryptoTableInfoRepository.getCryptoTableInfo(tableName);
      cryptoTableInfo.getCryptoColumnInfoMap().values()
              .stream()
              .filter(column->column.isFuzzy())
              .forEach(column -> deleteIndexTableValue(tableName, column.getColumnName(), ids));
  }

  @Transactional
  @Override
  public void deleteIndexTableValue(String tableName, String columnName, List<Long> ids) {
    indexTableMapper.deleteId(tableName, columnName, ids);
  }
}
