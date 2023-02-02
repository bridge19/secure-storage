package io.bridge.secure.storage.plugin.sqlparser;


import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.tokenizer.ITokenizer;
import io.bridge.secure.storage.util.ObjectUtil;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.indextable.entity.IndexTable;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.List;

@Slf4j
public class StatementParser {

  public static void processSQL(BoundSql boundSql, MappedStatement ms, Object parameter, StatementInfo statementInfo){
    try {
      SqlCommandType statementType = ms.getSqlCommandType();
      Statement statement = CCJSqlParserUtil.parse(boundSql.getSql());
      processTableAlias(statement,statementType,statementInfo);
      processWhere(ms,statement,statementType,statementInfo);
    } catch (JSQLParserException e) {
      throw new RuntimeException(e);
    }
  }
  public static void processParameter(SqlCommandType statementType,BoundSql boundSql,Object parameter, StatementInfo statementInfo){
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if(CollectionUtils.isEmpty(parameterMappings)){
      return;
    }
    for(int i=0,size=parameterMappings.size();i<size;i++){
      ParameterMapping parameterMapping = parameterMappings.get(i);
      String tableName = statementInfo.getDefaultTable();
      String fieldName = parameterMapping.getProperty();
      CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByFieldName(tableName,fieldName);
      if(cryptoColumnInfo==null){
        continue;
      }
      ICryptor cryptor = CryptoTableInfoRepository.getCryptor(cryptoColumnInfo.getCryptor());
      String columnName = cryptoColumnInfo.getColumnName();
      if(parameter instanceof MapperMethod.ParamMap){
        MapperMethod.ParamMap paramMap = (MapperMethod.ParamMap) parameter;
        String value = (String)paramMap.get(fieldName);
        paramMap.put(fieldName, ObjectUtil.encryptString(value,cryptor));
        if(statementType!=SqlCommandType.SELECT) {
          String idFieldName = CryptoTableInfoRepository.getCryptoTableInfo(tableName).getIdFieldName();
          Long idValue = (Long)paramMap.get(idFieldName);
          if (value != null && idValue != null) {
            prepareIndexTableValue(tableName, columnName, value, idValue, statementInfo);
          }
        }
      }else if(parameter instanceof List){
        for(Object obj : (List)parameter){
          try {
            ObjectUtil.encryptObject(obj,fieldName,cryptor);
            if(statementType!=SqlCommandType.SELECT) {
              String idFieldName = CryptoTableInfoRepository.getCryptoTableInfo(tableName).getIdFieldName();
              String value = (String) ObjectUtil.fieldValue(obj, fieldName, cryptor);
              Long idValue = (Long) ObjectUtil.fieldValue(obj, idFieldName, null);
              if (value != null && idValue != null && statementType != SqlCommandType.SELECT) {
                prepareIndexTableValue(tableName, columnName, value, idValue, statementInfo);
              }
            }
          } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn(String.format("field [%s] is not found in class [].",fieldName,obj.getClass().getName()));
          }
        }
      }else if(parameter instanceof Object){
        try {
          ObjectUtil.encryptObject(parameter,fieldName,cryptor);
          if(statementType!=SqlCommandType.SELECT) {
            String idFieldName = CryptoTableInfoRepository.getCryptoTableInfo(tableName).getIdFieldName();
            String value = (String) ObjectUtil.fieldValue(parameter, fieldName, cryptor);
            Long idValue = (Long) ObjectUtil.fieldValue(parameter, idFieldName, null);
            if (value != null && idValue != null && statementType != SqlCommandType.SELECT) {
              prepareIndexTableValue(tableName, columnName, value, idValue, statementInfo);
            }
          }
        } catch (NoSuchFieldException | IllegalAccessException e) {
          log.warn(String.format("field [%s] is not found in class [].",fieldName,parameter.getClass().getName()));
        }
      }
    }
  }

  private static void prepareIndexTableValue(String tableName, String columnName, String columnValue,Long idValue, StatementInfo statementInfo){
    String indexTableName = IndexTableInfoRepository.getIndexTableName(tableName,columnName);
    IndexTableValueInfo indexTableValueInfo = statementInfo.getIndexTableValues().get(indexTableName);
    if(indexTableValueInfo == null) {
      indexTableValueInfo = new IndexTableValueInfo();
      statementInfo.addIndexTableValueInfo(indexTableName, indexTableValueInfo);
    }
    indexTableValueInfo.setTableName(tableName);
    indexTableValueInfo.setColumnName(columnName);
    IndexTable indexTable = new IndexTable();
    indexTable.setRefTableIdColumnValue(idValue);
    indexTable.setColumnValue(columnValue);
    indexTableValueInfo.getValues().add(indexTable);
  }
  private static void processWhere(MappedStatement ms,Statement statement,SqlCommandType statementType,StatementInfo statementInfo){
    Expression whereExpression = null;
    if(statementType == SqlCommandType.SELECT) {
      Select select = (Select) statement;
      PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
      whereExpression = plainSelect.getWhere();
      WhereExpressionVisitor whereExpressionVisitor = new WhereExpressionVisitor(statementInfo, ms);
      plainSelect.setWhere(whereExpressionVisitor.accept(whereExpression));
      statementInfo.setUpdatedSQL(select.toString());
    }else if(statementType == SqlCommandType.UPDATE){
      Update update = (Update) statement;
      whereExpression = update.getWhere();
      WhereExpressionVisitor whereExpressionVisitor = new WhereExpressionVisitor(statementInfo, ms);
      update.setWhere(whereExpressionVisitor.accept(whereExpression));
      statementInfo.setUpdatedSQL(update.toString());
    }

  }

  private static void processTableAlias(Statement statement,SqlCommandType statementType,StatementInfo statementInfo) {
    if (statementType == SqlCommandType.SELECT) {
      Select select = (Select) statement;
      PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
      FromItem fromItem = plainSelect.getFromItem();
      if (fromItem instanceof Table) {
        Table table = (Table) fromItem;
        String tableName = table.getName();
        statementInfo.setDefaultTable(tableName);
        String alias = table.getAlias() == null ? null : table.getAlias().getName();
        if (alias != null) {
          statementInfo.addTableAlias(tableName, alias);
        }
      }
      List<Join> joins = plainSelect.getJoins();
      if (joins != null) {
        for (Join join : joins) {
          FromItem joinRightItem = join.getRightItem();
          if (joinRightItem instanceof Table) {
            Table table = (Table) joinRightItem;
            String tableName = table.getName();
            String alias = table.getAlias() == null ? null : table.getAlias().getName();
            if (alias != null) {
              statementInfo.addTableAlias(tableName, alias);
            }
          }
        }
      }
    }else if(statementType == SqlCommandType.UPDATE){
      Update update = (Update) statement;
      Table table = update.getTable();
      String tableName = table.getName();
      statementInfo.setDefaultTable(tableName);
      String alias = table.getAlias() == null ? null : table.getAlias().getName();
      if (alias != null) {
        statementInfo.addTableAlias(tableName, alias);
      }
    }else if(statementType == SqlCommandType.INSERT){
      Insert insert = (Insert) statement;
      Table table = insert.getTable();
      String tableName = table.getName();
      statementInfo.setDefaultTable(tableName);
      String alias = table.getAlias() == null ? null : table.getAlias().getName();
      if (alias != null) {
        statementInfo.addTableAlias(tableName, alias);
      }
    }
  }
}
