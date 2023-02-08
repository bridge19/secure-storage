package io.bridge.secure.storage.plugin.processor.parser;

import io.bridge.secure.storage.plugin.processor.StatementInfo;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

@Slf4j
public class DefaultStatementParser implements IStatementParser,IStatementVisitor,StatementInfoAccessor {
  private StatementInfo statementInfo = new StatementInfo();
  public DefaultStatementParser(String statementId, Statement statement){
    statementInfo.setStatementId(statementId);
    statementInfo.setStatement(statement);
  }
  @Override
  public StatementInfo getStatementInfo() {
    return statementInfo;
  }

  @Override
  public void parse() {
    if(statementInfo.getStatement() instanceof Select) {
      getStatementInfo().setSqlCommandType(SqlCommandType.SELECT);
      visit((Select)statementInfo.getStatement());
    }else if(statementInfo.getStatement() instanceof Update) {
      getStatementInfo().setSqlCommandType(SqlCommandType.UPDATE);
      visit((Update) statementInfo.getStatement());
    }else if(statementInfo.getStatement() instanceof Delete) {
      getStatementInfo().setSqlCommandType(SqlCommandType.DELETE);
      visit((Delete) statementInfo.getStatement());
    }else if(statementInfo.getStatement() instanceof Insert) {
      getStatementInfo().setSqlCommandType(SqlCommandType.INSERT);
      visit((Insert) statementInfo.getStatement());
    }
  }

  @Override
  public void enhanceWhere(Configuration configuration) {
    if(!getStatementInfo().isInsert()){
      if(getStatementInfo().isSelect()){
        Select select = (Select) getStatementInfo().getStatement();
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        plainSelect.setWhere(accept(configuration,plainSelect.getWhere()));
      } else if (getStatementInfo().isUpdate()){
        Update update = (Update) getStatementInfo().getStatement();
        update.setWhere(accept(configuration,update.getWhere()));
      }else if (getStatementInfo().isDelete()){
        Delete delete = (Delete) getStatementInfo().getStatement();
        delete.setWhere(accept(configuration,delete.getWhere()));
      }
    }
  }

  @Override
  public void visit(Select select) {
    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
    parseFromItem(plainSelect.getFromItem());
    parseJoins(plainSelect.getJoins());
  }
  @Override
  public void visit(Delete delete) {
    parseMainTable(delete.getTable());
    checkIdInWhere(delete.getWhere());
  }

  @Override
  public void visit(Update update) {
    parseMainTable(update.getTable());
    updateColumns(update.getColumns());
    checkIdInWhere(update.getWhere());
  }

  @Override
  public void visit(Insert insert) {
    parseMainTable(insert.getTable());
  }

  private void checkIdInWhere(Expression where){
    if(where instanceof AndExpression){
      AndExpression andExpression = (AndExpression) where;
      checkIdInWhere(andExpression.getLeftExpression());
      checkIdInWhere(andExpression.getRightExpression());
    }else if(where instanceof OrExpression){
      OrExpression orExpression = (OrExpression) where;
      checkIdInWhere(orExpression.getLeftExpression());
      checkIdInWhere(orExpression.getRightExpression());
    }else if(where instanceof EqualsTo){
      EqualsTo equalsTo = (EqualsTo)where;
      Expression leftExpression = equalsTo.getLeftExpression();
      if(leftExpression instanceof Column) {
        Column column = (Column) leftExpression;
        String  tableName = getStatementInfo().getMainTable();
        String columnName = column.getColumnName();
        CryptoTableInfo cryptoTableInfo = CryptoTableInfoRepository.getCryptoTableInfo(tableName);
        if(cryptoTableInfo!=null && columnName.equals(cryptoTableInfo.getIdColumnName())){
          getStatementInfo().setIdInWhere(true);
        }
      }
    }
  }

  private void prepareIdValues(){

  }
}
