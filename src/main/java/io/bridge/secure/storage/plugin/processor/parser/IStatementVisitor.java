package io.bridge.secure.storage.plugin.processor.parser;

import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface IStatementVisitor extends StatementInfoAccessor {
  Logger logger = LoggerFactory.getLogger(IStatementVisitor.class);
  void visit(Select select);
  void visit(Insert insert);
  void visit(Update update);
  void visit(Delete delete);

  default void parseFromItem(FromItem fromItem){
    if (fromItem instanceof Table) {
      Table table = (Table) fromItem;
      parseMainTable(table);
    }
  }
  default void parseMainTable(Table table){
    String tableName = table.getName();
    getStatementInfo().setMainTable(tableName);
    String alias = table.getAlias() == null ? null : table.getAlias().getName();
    if (alias != null) {
      getStatementInfo().addAliasTableRef(alias,tableName);
    }
  }
  default void parseJoins(List<Join> joins){
    if (joins == null) {
      return;
    }
    joins.stream().forEach(join -> {
      FromItem joinRightItem = join.getRightItem();
      if (joinRightItem instanceof Table) {
        Table table = (Table) joinRightItem;
        String tableName = table.getName();
        String alias = table.getAlias() == null ? null : table.getAlias().getName();
        if (alias != null) {
          getStatementInfo().addAliasTableRef(alias,tableName);
        }
      }else if(joinRightItem instanceof SubSelect){
        logger.info(String.format("subSelect is found in statement: %s",getStatementInfo().getStatementId()));
      }
    });
  }

  default void updateColumns(List<Column> columns){
    String mainTable = getStatementInfo().getMainTable();
    CryptoTableInfo tableInfo = CryptoTableInfoRepository.getCryptoTableInfo(mainTable);
    if(tableInfo==null) {
      return;
    }
    for(Column column:columns){
      String columnName = column.getColumnName();
      getStatementInfo().addUpdatedColumns(columnName);
      if(columnName.equals(tableInfo.getDeleteFieldName())){
        getStatementInfo().setLogicDelete(true);
        break;
      }
      CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(mainTable,columnName);
      if(cryptoColumnInfo!=null&&cryptoColumnInfo.isFuzzy()){
        getStatementInfo().addUpdatedFussyColumns(columnName);
      }
    }
  }
  default Expression accept(Configuration configuration, Expression expression){
    if(expression instanceof AndExpression){
      AndExpression andExpression = (AndExpression) expression;
      andExpression.setLeftExpression(accept(configuration,andExpression.getLeftExpression()));
      andExpression.setRightExpression(accept(configuration,andExpression.getRightExpression()));
    }else if(expression instanceof OrExpression){
      OrExpression orExpression = (OrExpression) expression;
      orExpression.setLeftExpression(accept(configuration,orExpression.getLeftExpression()));
      orExpression.setRightExpression(accept(configuration,orExpression.getRightExpression()));
    }else if(expression instanceof LikeExpression){
      LikeExpression likeExpression = (LikeExpression)expression;
      Expression leftExpression = likeExpression.getLeftExpression();
      if(leftExpression instanceof Column) {
        Column column = (Column) leftExpression;
        Table table = column.getTable();
        String tableName = null;
        String alias = null;
        if(table == null){
          tableName = getStatementInfo().getMainTable();
        }else {
          alias = table.getFullyQualifiedName();
          tableName = getStatementInfo().getTableNameByAlias(alias);
        }
        String columnName = column.getColumnName();
        CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository.getCryptoColumnByColumnName(tableName,columnName);
        if(cryptoColumnInfo!=null) {
          if (cryptoColumnInfo.isFuzzy()) {
            try {
              return enhanceFussy(configuration,tableName, alias, columnName);
            } catch (JSQLParserException e) {
              logger.warn(String.format("error when creating in expression for %s, table %s, colun %s",getStatementInfo().getStatementId(),tableName,columnName));
            }
          }
        }
      }
    }
    return expression;
  }
  default Expression enhanceFussy(Configuration configuration,String tableName, String tableAliasName, String columnName) throws JSQLParserException {
    String selectIdStatement = "io.bridge.secure.storage.indextable.mapper.IndexTableMapper.selectId";
    MappedStatement selectIdMs = configuration.getMappedStatement(selectIdStatement,false);
    MapperMethod.ParamMap paramMap = new MapperMethod.ParamMap();
    paramMap.put("tableName", tableName);
    paramMap.put("columnName", columnName);
    paramMap.put("columnValue", "");
    String subSelectSQL = selectIdMs.getSqlSource().getBoundSql(paramMap).getSql();
    Select subSelect = (Select) CCJSqlParserUtil.parse(subSelectSQL);

    InExpression inExpression = new InExpression();
    String idColumnName = CryptoTableInfoRepository.getCryptoTableInfo(tableName).getIdColumnName();
    Column leftExpr = new Column();
    leftExpr.setColumnName(idColumnName);
    if(!StringUtils.isBlank(StringUtils.trim(tableAliasName))){
      Table table = new Table(tableAliasName);
      leftExpr.setTable(table);
    }
    inExpression.setLeftExpression(leftExpr);
    SubSelect rightExpr = new SubSelect();
    rightExpr.setSelectBody(subSelect.getSelectBody());
    inExpression.setRightExpression(rightExpr);
    return inExpression;
  }
}
