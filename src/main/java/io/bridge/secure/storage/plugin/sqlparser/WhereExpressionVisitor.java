package io.bridge.secure.storage.plugin.sqlparser;

import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

@Slf4j
public class WhereExpressionVisitor {

  private StatementInfo tableAliasInfo;
  private MappedStatement ms;
  public WhereExpressionVisitor(StatementInfo tableAliasInfo, MappedStatement ms){
    this.tableAliasInfo = tableAliasInfo;
    this.ms = ms;
  }

  public Expression accept(Expression expression){
    if(expression instanceof AndExpression){
      AndExpression andExpression = (AndExpression) expression;
      andExpression.setLeftExpression(accept(andExpression.getLeftExpression()));
      andExpression.setRightExpression(accept(andExpression.getRightExpression()));
    }else if(expression instanceof OrExpression){
      OrExpression orExpression = (OrExpression) expression;
      orExpression.setLeftExpression(accept(orExpression.getLeftExpression()));
      orExpression.setRightExpression(accept(orExpression.getRightExpression()));
    }else if(expression instanceof LikeExpression){
      LikeExpression likeExpression = (LikeExpression)expression;
      Expression leftExpression = likeExpression.getLeftExpression();
      if(leftExpression instanceof Column) {
        Column column = (Column) leftExpression;
        Table table = column.getTable();
        String tableName = null;
        String alias = null;
        if(table == null){
          tableName = tableAliasInfo.getDefaultTable();
        }else {
          alias = table.getFullyQualifiedName();
          tableName = tableAliasInfo.getTableNameByAlias(alias);
        }
        String columnName = column.getColumnName();
        CryptoColumnInfo cryptoColumnInfo = CryptoTableInfoRepository
                                                      .getCryptoTableInfo(tableName)
                                                      .getCryptoColumnInfoMap()
                                                      .get(columnName);

        try { boolean isFussyColumn = cryptoColumnInfo.isFuzzy();
          if(isFussyColumn){
            return createInExpression(tableName,alias,columnName);
          }
        }catch (JSQLParserException e) {
          log.warn("parse like condition error.",e);
        }
      }
    }
    return expression;
  }

  private Expression createInExpression(String tableName, String tableAliasName,String columnName) throws JSQLParserException {
    Configuration configuration = ms.getConfiguration();
    String selectIdStatement = "io.bridge.secure.storage.indextable.mapper.IndexTableMapper.selectId";
    MappedStatement selectIdMs = configuration.getMappedStatement(selectIdStatement,false);
    MapperMethod.ParamMap paramMap = new MapperMethod.ParamMap();
    paramMap.put("tableName", tableName);
    paramMap.put("columnName", columnName);
    paramMap.put("columnValue", "");
    String subSelectSQL = selectIdMs.getSqlSource().getBoundSql(paramMap).getSql();
    Select subSelect = (Select)CCJSqlParserUtil.parse(subSelectSQL);

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
