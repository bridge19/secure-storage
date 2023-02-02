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
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
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

  @Value("${secure.storage.init.index-tables:false}")
  private boolean initIndexTables;

  @Resource
  private IndexTableMapper indexTableMapper;

  @Resource
  private OriginalTableMapper originalTableMapper;
  public void process(){
    List<String> createdTables = checkAndCreateIdxTable();
    if(!initIndexTables && CollectionUtils.isEmpty(createdTables)){
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
    try {
      List<String> allTables = queryAllTableNames();
      if(CollectionUtils.isNotEmpty(indexTableNameList)&&CollectionUtils.isNotEmpty(allTables)){
        for(String indexTableName : indexTableNameList){
          if(!allTables.contains(indexTableName)){
            createIdxTable(IndexTableInfoRepository.getIndexTableInfo(indexTableName));
            createdTables.add(indexTableName);
          }
        }
      }
    }catch (SQLException e){
      log.error("init index table error.",e);
    }
    return createdTables;
  }

  private void createIdxTable(IndexTableInfo indexTableDesc){
    indexTableMapper.createTable(indexTableDesc.getRefTableName(),indexTableDesc.getColumnName());
  }
  private List<String> queryAllTableNames() throws SQLException {
    SqlSession session = sqlSessionFactory.openSession();
    try {
      Connection conn = session.getConnection();
      conn.setAutoCommit(false);
      String url = this.url.indexOf('?')>0?this.url.substring(0,this.url.indexOf('?')):this.url;
      String schemaName = url.substring(url.lastIndexOf("/")+1);
      ResultSet rs = null;
      DatabaseMetaData meta = conn.getMetaData();
      rs = meta.getTables(schemaName, null, null, new String[] {
              "TABLE"
      });
      List<String> tableNames = new ArrayList<>();
      while (rs.next()) {
        String tblName = rs.getString("TABLE_NAME");
        tableNames.add(tblName);
      }
      return tableNames;
    }finally {
      session.close();
    }
  }
}
