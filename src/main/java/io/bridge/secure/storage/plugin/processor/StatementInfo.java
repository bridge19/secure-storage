package io.bridge.secure.storage.plugin.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import io.bridge.secure.storage.tokenizer.ITokenizer;
import io.bridge.secure.storage.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class StatementInfo {

  private String statementId;
  private Statement statement;
  private SqlCommandType sqlCommandType;
  private static Map<String, List<String>> nonCachedStatementMap = new HashMap<>();
  private String mainTable;
  private Map<String,String> aliasTableRefMap = new HashMap<>();
  private List<FussySearchInfo> fussySearchInfos = new ArrayList<>();

  private boolean logicDelete = false;
  private boolean idInWhere = false;
  private List<String> updatedColumns = new ArrayList<>();
  private List<String> updatedFussyColumns = new ArrayList<>();
  protected static final Map<String, MappedStatement> impactIdQueryMsCache = new ConcurrentHashMap<>();
  public boolean isLogicDelete(){
    return logicDelete;
  }
  public boolean isDelete() {
    return sqlCommandType==SqlCommandType.DELETE;
  }
  public boolean isSelect(){
    return sqlCommandType == SqlCommandType.SELECT;
  }
  public boolean isUpdate(){
    return sqlCommandType == SqlCommandType.UPDATE;
  }
  public boolean isInsert(){
    return sqlCommandType == SqlCommandType.INSERT;
  }

  public void setIdInWhere(boolean idInWhere){
    this.idInWhere = idInWhere;
  }
  public boolean hasIdInWhere(){
    return idInWhere;
  }
  public void setLogicDelete(boolean logicDelete) {
    logicDelete = logicDelete;
  }

  public List<String> getUpdatedColumns() {
    return updatedColumns;
  }

  public void addUpdatedColumns(String columnName) {
    this.updatedColumns.add(columnName);
  }

  public List<String> getUpdatedFussyColumns() {
    return updatedFussyColumns;
  }

  public void addUpdatedFussyColumns(String columnName) {
    this.updatedFussyColumns.add(columnName);
  }

  private Map<String, IndexTableValueInfo> indexTableValues = new HashMap<>();
  public void addIndexTableValueInfo(String idxTableName, IndexTableValueInfo indexTable){
    indexTableValues.put(idxTableName,indexTable);
  }

  public Map<String, IndexTableValueInfo> getIndexTableValues(){
    return indexTableValues;
  }
  public String getMainTable() {
    return mainTable;
  }
  public void setMainTable(String mainTable) {
    this.mainTable=mainTable;
  }
  public String getTableNameByAlias(String alias) {
    return aliasTableRefMap.get(alias);
  }

  public void addAliasTableRef(String alias,String table) {
    this.aliasTableRefMap.put(alias,table);
  }

  public SqlCommandType getSqlCommandType() {
    return sqlCommandType;
  }

  public void setSqlCommandType(SqlCommandType sqlCommandType) {
    this.sqlCommandType = sqlCommandType;
  }

  public String getStatementId() {
    return statementId;
  }

  public void setStatementId(String statementId) {
    this.statementId = statementId;
  }

  public String getFinalSQL() {
    return statement.toString();
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }
  public Statement getStatement() {
    return this.statement;
  }

  public void processParameter(BoundSql boundSql, Object parameter){
    CryptoTableInfo cryptoTableInfo = CryptoTableInfoRepository.getCryptoTableInfo(mainTable);
    if(parameter instanceof MapperMethod.ParamMap){
      List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
      if(CollectionUtils.isEmpty(parameterMappings)){
        return;
      }
      MapperMethod.ParamMap paramMap = (MapperMethod.ParamMap) parameter;
      parameterMappings.stream().forEach(parameterMapping -> {
        String property = parameterMapping.getProperty();
        if(property.startsWith("et")){
          Object obj = paramMap.get("et");
          if(obj!=null){
            encryptParameter(cryptoTableInfo,obj);
          }
        }else if(property.startsWith("ew")){
          LambdaQueryWrapper lambdaQueryWrapper = (LambdaQueryWrapper) paramMap.get("ew");
          String conditions = lambdaQueryWrapper.getExpression().getSqlSegment();
          Pattern pattern = Pattern.compile("([0-9a-zA-Z_]*)\\s*=\\s*#\\{([^\\{\\}#]*)\\}");
          Matcher matcher = pattern.matcher(conditions);
          while (matcher.find()){
            String columnName =matcher.group(1);
            String prop = matcher.group(2);
            if(prop.equals(property)){
              CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(mainTable, columnName);
              if(cryptoColumnInfo!=null){
                String valuePairKey = prop.substring(prop.lastIndexOf(".")+1);
                String valuePairValue = (String)lambdaQueryWrapper.getParamNameValuePairs().get(valuePairKey);
                ICryptor cryptor = CryptoTableInfoRepository.getCryptor(cryptoColumnInfo.getCryptor());
                lambdaQueryWrapper.getParamNameValuePairs().put(valuePairKey, cryptor.encrypt(valuePairValue));
              }
            }
          }
        }else {
          String fieldValue = (String) paramMap.get(property);
          if (fieldValue !=null) {
            String tableName = this.getMainTable();
            CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByFieldName(tableName, property);
            if (cryptoColumnInfo != null) {
              ICryptor cryptor = CryptoTableInfoRepository.getCryptor(cryptoColumnInfo.getCryptor());
              paramMap.put(property, cryptor.encrypt(fieldValue));
            }
          }
        }
      });

    }else if(parameter instanceof List){
      for(Object obj : (List)parameter){
        if(obj.getClass() != cryptoTableInfo.getJavaClass()){
          break;
        }
        encryptParameter(cryptoTableInfo,parameter);
      }
    }else if(parameter instanceof Object){
      encryptParameter(cryptoTableInfo,parameter);
    }
  }

  private void encryptParameter(CryptoTableInfo cryptoTableInfo, Object parameter){
    if(cryptoTableInfo.getJavaClass() == parameter.getClass()){
      cryptoTableInfo.getCryptoColumnInfoMap().values().forEach(item->{
        try {
          if(item.isFuzzy()){
            String idFieldName = cryptoTableInfo.getIdFieldName();
            String value = (String) ObjectUtil.getFieldValue(parameter, item.getFieldName(), null);
            if((isUpdate() || isLogicDelete() || isDelete()) && !hasIdInWhere()) {
              prepareIndexTableValue(mainTable, item.getColumnName(), value);
            }else {
              Long idValue = (Long) ObjectUtil.getFieldValue(parameter, idFieldName, null);
              if (value != null && idValue != null) {
                prepareIndexTableValue(mainTable, item.getColumnName(), value, idValue);
              }
            }
          }
          ICryptor cryptor = CryptoTableInfoRepository.getCryptor(item.getCryptor());
          ObjectUtil.encryptObjectField(parameter,item.getFieldName(),cryptor);
        } catch (NoSuchFieldException | IllegalAccessException e) {
          log.warn(String.format("field [%s] is not found in class [].",item.getFieldName(),parameter.getClass().getName()));
        }
      });
    }
  }
  public void updateIndexTable(Executor executor,MappedStatement ms){
    if(CollectionUtils.isEmpty(getUpdatedFussyColumns())){
      return;
    }
    getIndexTableValues().values().stream()
            .filter(item->{
              String tableName = item.getTableName();
              String columnName = item.getColumnName();
              CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
              return cryptoColumnInfo!=null&&cryptoColumnInfo.isFuzzy();
            })
            .forEach(item -> {
              deleteIndexTable(executor,ms.getConfiguration(),item);
              insertIndexTable(executor,ms.getConfiguration(),item);
            });

  }
  public void deleteIndexTable(Executor executor,MappedStatement ms){
    getIndexTableValues().values().stream()
            .filter(item->{
              String tableName = item.getTableName();
              String columnName = item.getColumnName();
              CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
              return cryptoColumnInfo!=null&&cryptoColumnInfo.isFuzzy();
            })
            .forEach(item -> {
              deleteIndexTable(executor,ms.getConfiguration(),item);
            });
  }

  public void insertIndexTable(Executor executor,MappedStatement ms){
    getIndexTableValues().values().stream()
            .filter(item->{
              String tableName = item.getTableName();
              String columnName = item.getColumnName();
              CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
              return cryptoColumnInfo!=null&&cryptoColumnInfo.isFuzzy();
            })
            .forEach(item -> insertIndexTable(executor,ms.getConfiguration(),item));
  }

  private void prepareIndexTableValue(String tableName, String columnName, String columnValue){
    if(columnValue == null){
      return;
    }
    String indexTableName = IndexTableInfoRepository.getIndexTableName(tableName,columnName);
    IndexTableValueInfo indexTableValueInfo = getIndexTableValues().get(indexTableName);
    if(indexTableValueInfo == null) {
      indexTableValueInfo = new IndexTableValueInfo();
      indexTableValueInfo.setTableName(tableName);
      indexTableValueInfo.setColumnName(columnName);
      addIndexTableValueInfo(indexTableName, indexTableValueInfo);
    }
    indexTableValueInfo.setColumnValue(columnValue);
  }
  private void prepareIndexTableValue(String tableName, String columnName, List<Long> ids){
    if(CollectionUtils.isEmpty(ids)){
      return;
    }
    String indexTableName = IndexTableInfoRepository.getIndexTableName(tableName,columnName);
    IndexTableValueInfo indexTableValueInfo = getIndexTableValues().get(indexTableName);
    if(indexTableValueInfo == null) {
      indexTableValueInfo = new IndexTableValueInfo();
      indexTableValueInfo.setTableName(tableName);
      indexTableValueInfo.setColumnName(columnName);
      addIndexTableValueInfo(indexTableName, indexTableValueInfo);
    }
    indexTableValueInfo.getUpdatedIds().addAll(ids);
    for(Long id: ids){
      IndexTable indexTable = new IndexTable();
      indexTable.setRefTableIdColumnValue(id);
      indexTable.setColumnValue(indexTableValueInfo.getColumnValue());
      indexTableValueInfo.getValues().add(indexTable);
    }
  }
  private void prepareIndexTableValue(String tableName, String columnName, String columnValue,Long idValue){
    String indexTableName = IndexTableInfoRepository.getIndexTableName(tableName,columnName);
    IndexTableValueInfo indexTableValueInfo = getIndexTableValues().get(indexTableName);
    if(indexTableValueInfo == null) {
      indexTableValueInfo = new IndexTableValueInfo();
      indexTableValueInfo.setTableName(tableName);
      indexTableValueInfo.setColumnName(columnName);
      addIndexTableValueInfo(indexTableName, indexTableValueInfo);
    }
    if(columnValue==null || idValue ==null){
      return;
    }
    if((isUpdate() || isInsert()) && (columnValue!=null && idValue !=null)) {
      IndexTable indexTable = new IndexTable();
      indexTable.setRefTableIdColumnValue(idValue);
      indexTable.setColumnValue(columnValue);
      indexTableValueInfo.getValues().add(indexTable);
    }
    if ((isDelete() || isLogicDelete() || isUpdate()) && idValue!=null){
      indexTableValueInfo.getUpdatedIds().add(idValue);
    }
  }

  private void insertIndexTable(Executor executor,Configuration configuration, IndexTableValueInfo item){
    String statementId = "io.bridge.secure.storage.indextable.mapper.IndexTableMapper.batchInsert";
    MappedStatement batchInsertMs = configuration.getMappedStatement(statementId);
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
  }
  private void deleteIndexTable(Executor executor,Configuration configuration, IndexTableValueInfo item){
    String statementId = "io.bridge.secure.storage.indextable.mapper.IndexTableMapper.logicDeleteId";
    MappedStatement batchInsertMs = configuration.getMappedStatement(statementId);
    MapperMethod.ParamMap paramMap = new MapperMethod.ParamMap();
    String tableName = item.getTableName();
    String columnName = item.getColumnName();
    List<Long> ids = item.getUpdatedIds();
    paramMap.put("tableName", tableName);
    paramMap.put("columnName", columnName);
    paramMap.put("list", ids);
    try {
      executor.update(batchInsertMs,paramMap);
    } catch (SQLException e) {
      log.warn(String.format("error where batch insert table %s", IndexTableInfoRepository.getIndexTableName(item.getTableName(),item.getColumnName())),e);
    }
  }

  public void prepareIdValues(Executor executor, Configuration configuration, Object parameter, BoundSql boundSql){

    String queryIdStatementId = getStatementId()+"_QUERY_ID";
    MappedStatement queryIdStatementMs = impactIdQueryMsCache.get(queryIdStatementId);
    PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
    BoundSql queryIdBoundSql;
    if(queryIdStatementMs == null) {
      SqlSourceBuilder sqlSourceBuilder = new SqlSourceBuilder(configuration);
      String queryIdSqlStr = buildQueryIdSql(configuration);
      SqlSource sqlSource = sqlSourceBuilder.parse(queryIdSqlStr,parameter.getClass(),mpBoundSql.additionalParameters());
      queryIdStatementMs = buildQueryIdMs(configuration,sqlSource,buildParameterMapping(mpBoundSql.parameterMappings()));
    }
    queryIdBoundSql = queryIdStatementMs.getBoundSql(parameter);
    try {

      CacheKey cacheKey = executor.createCacheKey(queryIdStatementMs, parameter, RowBounds.DEFAULT, queryIdBoundSql);
      List<Long> ids = executor.query(queryIdStatementMs,parameter,RowBounds.DEFAULT,null,cacheKey,queryIdBoundSql);
      CryptoTableInfo cryptoTableInfo = CryptoTableInfoRepository.getCryptoTableInfo(mainTable);
      if(cryptoTableInfo!=null) {
        cryptoTableInfo.getCryptoColumnInfoMap()
                .entrySet().stream()
                        .forEach(entry ->{
                          CryptoColumnInfo cryptoColumnInfo = entry.getValue();
                          if(cryptoColumnInfo.isFuzzy()){
                            prepareIndexTableValue(this.mainTable, cryptoColumnInfo.getColumnName(),ids);
                          }
                        });
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private MappedStatement buildQueryIdMs(Configuration configuration,SqlSource sqlSource,List<ParameterMapping> parameterMappings){
    String statementId = "io.bridge.secure.storage.indextable.mapper.OriginalTableMapper.selectImpactIds";
    MappedStatement ms = configuration.getMappedStatement(statementId);
    String impactIdQueryId = getStatementId()+"_QUERY_ID";
    return com.baomidou.mybatisplus.core.toolkit.CollectionUtils.computeIfAbsent(impactIdQueryMsCache, impactIdQueryId, key -> {
      MappedStatement.Builder builder = new MappedStatement.Builder(configuration, key, sqlSource, ms.getSqlCommandType());
      builder.resource(ms.getResource());
      builder.fetchSize(ms.getFetchSize());
      builder.statementType(ms.getStatementType());
      builder.timeout(ms.getTimeout());
      ParameterMap parameterMap = new ParameterMap.Builder(configuration,ms.getParameterMap().getId(),ms.getParameterMap().getClass(),parameterMappings).build();
      builder.parameterMap(parameterMap);
      builder.resultMaps(ms.getResultMaps());
      builder.resultSetType(ms.getResultSetType());
      builder.cache(ms.getCache());
      builder.flushCacheRequired(ms.isFlushCacheRequired());
      builder.useCache(ms.isUseCache());
      return builder.build();
    });
  }

  private String buildQueryIdSql(Configuration configuration){
    String statementId = "io.bridge.secure.storage.indextable.mapper.OriginalTableMapper.selectImpactIds";
    MappedStatement ms = configuration.getMappedStatement(statementId);
    MapperMethod.ParamMap paramMap = new MapperMethod.ParamMap();
    paramMap.put("tableName",mainTable);
    paramMap.put("idColumnName",CryptoTableInfoRepository.getCryptoTableInfo(mainTable).getIdColumnName());
    String selectPart = ms.getBoundSql(paramMap).getSql();
    String wherePart=null;
    if(isUpdate()){
      wherePart = ((Update)getStatement()).getWhere().toString();
    }else if(isDelete()){
      wherePart = ((Delete)getStatement()).getWhere().toString();
    }
    return selectPart +" where " + wherePart;
  }
  private List<ParameterMapping> buildParameterMapping(List<ParameterMapping> original){
    int omitSize = updatedColumns.size();
    List<ParameterMapping> target = new ArrayList<>();
    for(int i=0,len=original.size();i<len;i++){
      if(i>=omitSize){
        target.add(original.get(i));
      }
    }
    return target;
  }
}
