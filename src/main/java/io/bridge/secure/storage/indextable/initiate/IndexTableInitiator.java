package io.bridge.secure.storage.indextable.initiate;

import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.indextable.IndexTableInfo;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import io.bridge.secure.storage.indextable.entity.OriginalTable;
import io.bridge.secure.storage.indextable.mapper.IndexTableMapper;
import io.bridge.secure.storage.indextable.mapper.OriginalTableMapper;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import io.bridge.secure.storage.tokenizer.ITokenizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class IndexTableInitiator implements Initiator {

  @Value("${spring.datasource.url}")
  private String url;
  @Autowired
  private SqlSessionFactory sqlSessionFactory;

  @Autowired
  @Qualifier("indexTableValueInitiator")
  private Initiator indexTableValueInitiator;

  @Value("${secure.storage.init.index-tables:true}")
  private boolean initIndexTables;

  @Value("${secure.storage.init.index-table-values:true}")
  private boolean initIndexTableValues;
  @Resource
  private IndexTableMapper indexTableMapper;

  @Resource
  private OriginalTableMapper originalTableMapper;
  public void process(){
    if(!initIndexTables){
      log.info("Ignore initiating index table. Turn on checking switcher, configure [secure.storage.init.index-table=ture].");
      return;
    }
    List<String> createdTables = checkAndCreateIdxTable();
    if(!initIndexTableValues && CollectionUtils.isEmpty(createdTables)){
      log.info("Ignore initiating index table value. Turn on checking switcher, configure [secure.storage.init.index-table-values=ture].");
      return;
    }
    initiateIndexTableValue(createdTables);
  }

  private void initiateIndexTableValue(List<String> createdTables) {
    for(String idxTableName : createdTables){
      IndexTableInfo item = IndexTableInfoRepository.getIndexTableInfo(idxTableName);
      Long lastRowId = -1L;
      String refTableName = item.getRefTableName();
      String refTableIdColumnName = item.getRefTableIdColumnName();
      String columnName = item.getColumnName();
      CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(refTableName,columnName);
      while(true) {
        List<OriginalTable> originalTableValues = null;
        if(lastRowId==-1L) {
          originalTableValues = originalTableMapper.selectOriginalTableValues(refTableName, refTableIdColumnName, columnName);
        } else {
          originalTableValues = originalTableMapper.selectOriginalTableValuesById(refTableName, refTableIdColumnName, columnName,lastRowId);
        }
        if (CollectionUtils.isEmpty(originalTableValues)) {
          break;
        }
        List<IndexTable> indexTableValues = new ArrayList<>();
        for(OriginalTable originalTableValue : originalTableValues){
          ITokenizer tokenizer = CryptoTableInfoRepository.getTokenizer(cryptoColumnInfo.getTokenizer());
          ICryptor cryptor = CryptoTableInfoRepository.getCryptor(cryptoColumnInfo.getCryptor());
          String originalValue = originalTableValue.getColumnValue();
          String decryptedValue = cryptor.decrypt(originalValue);
          if(decryptedValue == null){ //无法解析，可能非密文
            decryptedValue = originalValue;
          }
          List<String> parseResult = tokenizer.parse(decryptedValue);
          if(CollectionUtils.isNotEmpty(parseResult)){
            for(String str : parseResult){
              IndexTable indexTableValue = new IndexTable();
              indexTableValue.setColumnValue(cryptor.encrypt(str));
              indexTableValue.setRefTableIdColumnValue(originalTableValue.getId());
              indexTableValues.add(indexTableValue);
            }
          }else {
            IndexTable indexTableValue = new IndexTable();
            indexTableValue.setColumnValue(cryptor.encrypt(decryptedValue));
            indexTableValue.setRefTableIdColumnValue(originalTableValue.getId());
            indexTableValues.add(indexTableValue);
          }
        }
        indexTableMapper.batchInsert(item.getRefTableName(),item.getColumnName(),indexTableValues);
        if(originalTableValues.size()<1000){
          break;
        }
      }

    }
  }

  private List<String> checkAndCreateIdxTable() {
    List<String> createdTables = new ArrayList<>();
    Set<String> indexTableNameList = IndexTableInfoRepository.allIndexTableName();
    List<String> allTables = queryAllTableNames();
    if(CollectionUtils.isNotEmpty(indexTableNameList)&&CollectionUtils.isNotEmpty(allTables)){
      for(String indexTableName : indexTableNameList){
        if(!allTables.contains(indexTableName)){
          createIdxTable(IndexTableInfoRepository.getIndexTableInfo(indexTableName));
          createdTables.add(indexTableName);
        }
      }
    }
    return createdTables;
  }

  private void createIdxTable(IndexTableInfo indexTableDesc){
    indexTableMapper.createTable(indexTableDesc.getRefTableName(),indexTableDesc.getColumnName());
  }

  //考虑数据查询权限
  private List<String> queryAllTableNames() {
    log.debug("fetch all tables..");
    return originalTableMapper.showAllTables();
  }
}
