package io.bridge.secure.storage.plugin.statementhandler;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import io.bridge.secure.storage.indextable.mapper.IndexTableMapper;
import io.bridge.secure.storage.plugin.sqlparser.StatementInfo;
import io.bridge.secure.storage.plugin.sqlparser.StatementParser;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import io.bridge.secure.storage.tokenizer.ITokenizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("insertHandler")
public class CryptoInsertHandler implements ICryptoHandler{

  @Autowired
  private IndexTableMapper indexTableMapper;

  @Autowired
  private SqlSessionFactory sqlSessionFactory;
  private static final Map<String, StatementInfo> cachedStatements = new ConcurrentHashMap<>();
  @Override
  public SqlCommandType support() {
    return SqlCommandType.INSERT;
  }

  @Override
  public void beforeProcess(Executor executor,MappedStatement ms, Object parameter,BoundSql boundSql) {
    String statementId = ms.getId();
    StatementInfo statementInfo = cachedStatements.get(statementId);
    PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
    if (statementInfo == null) {
      statementInfo = new StatementInfo();
      statementInfo.setStatementId(statementId);
      StatementParser.processSQL(boundSql,ms,parameter,statementInfo);
    }
    StatementParser.processParameter(ms.getSqlCommandType(),boundSql,parameter,statementInfo);
    insertIndexTable(executor,ms,statementInfo);
  }

  private void insertIndexTable(Executor executor,MappedStatement ms, StatementInfo statementInfo){

    statementInfo.getIndexTableValues().values().stream()
      .filter(item->{
          String tableName = item.getTableName();
          String columnName = item.getColumnName();
          CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
          return cryptoColumnInfo!=null&&cryptoColumnInfo.isFuzzy();
        })
      .forEach(item -> {
        String statementId = "io.bridge.secure.storage.indextable.mapper.IndexTableMapper.batchInsert";
        MappedStatement batchInsertMs = ms.getConfiguration().getMappedStatement(statementId);
        MapperMethod.ParamMap paramMap = new MapperMethod.ParamMap();
        String tableName = item.getTableName();
        String columnName = item.getColumnName();
        CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
        List<IndexTable> newIndexTables = new ArrayList<>();
        for(IndexTable original: item.getValues()){
          ITokenizer tokenizer = CryptoTableInfoRepository.getTokenizer(cryptoColumnInfo.getTokenizer());
          ICryptor cryptor = CryptoTableInfoRepository.getCryptor(cryptoColumnInfo.getCryptor());
          List<String> parseResult = tokenizer.parse(original.getColumnValue());
          if(CollectionUtils.isNotEmpty(parseResult)){
            for(String str : parseResult){
              IndexTable indexTableValue = new IndexTable();
              indexTableValue.setColumnValue(cryptor.encrypt(str));
              indexTableValue.setRefTableIdColumnValue(original.getRefTableIdColumnValue());
              newIndexTables.add(indexTableValue);
            }
          }else {
            original.setColumnValue(cryptor.encrypt(original.getColumnValue()));
            newIndexTables.add(original);
          }
        }
        paramMap.put("tableName", tableName);
        paramMap.put("columnName", columnName);
        paramMap.put("list", newIndexTables);
        try {
          executor.update(batchInsertMs,paramMap);
        } catch (SQLException e) {
          log.warn(String.format("error where batch insert table %s", IndexTableInfoRepository.getIndexTableName(item.getTableName(),item.getColumnName())),e);
        }
      });
  }
}
