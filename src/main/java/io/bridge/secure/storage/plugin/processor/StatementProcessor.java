package io.bridge.secure.storage.plugin.processor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.plugin.processor.parser.DefaultStatementParser;
import io.bridge.secure.storage.plugin.processor.parser.IStatementParser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class StatementProcessor implements IStatementProcessor {
  private static Map<String, StatementInfo> cachedStatements = new ConcurrentHashMap<>();

  @Override
  public void process(Executor executor,MappedStatement ms, Object parameter,BoundSql boundSql) {
    String statementId = ms.getId();
    StatementInfo statementInfo = cachedStatements.get(statementId);
    PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
    try {
      if (statementInfo == null) {
        Statement statement = CCJSqlParserUtil.parse(boundSql.getSql());
        IStatementParser parser = new DefaultStatementParser(statementId,statement);
        parser.parse();
        parser.enhanceWhere(ms.getConfiguration());
        statementInfo = parser.getStatementInfo();
      }
      statementInfo.processParameter(boundSql,parameter);
    } catch (JSQLParserException e) {
      throw new RuntimeException(e);
    }
    if(statementInfo.isSelect()) {
      mpBoundSql.sql(statementInfo.getFinalSQL());
    }else if(statementInfo.isInsert()){
      statementInfo.insertIndexTable(executor,ms);
    } else if (statementInfo.isUpdate()) {
      if(!statementInfo.hasIdInWhere()){
        statementInfo.prepareIdValues(executor,ms.getConfiguration(),parameter,boundSql);
      }
      statementInfo.updateIndexTable(executor,ms);
    } else if (statementInfo.isDelete()) {
      if(!statementInfo.hasIdInWhere()){
        statementInfo.prepareIdValues(executor,ms.getConfiguration(),parameter,boundSql);
      }
      statementInfo.deleteIndexTable(executor,ms);
    }
  }


}
