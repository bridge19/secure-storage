package io.bridge.secure.storage.plugin.processor.statement;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.annotation.statement.NonCached;
import io.bridge.secure.storage.plugin.processor.IStatementProcessor;
import io.bridge.secure.storage.plugin.sqlparser.StatementInfo;
import io.bridge.secure.storage.plugin.sqlparser.StatementParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("selectHandler")
public class SelectProcessor implements IStatementProcessor {
  private static Map<String, StatementInfo> cachedStatements = new ConcurrentHashMap<>();

  @Override
  public SqlCommandType support() {
    return SqlCommandType.SELECT;
  }

  @Override
  public void process(Executor executor,MappedStatement ms, Object parameter,BoundSql boundSql) {

    String statementId = ms.getId();
    StatementInfo statementInfo = cachedStatements.get(statementId);
    PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
    boolean nonCaching = nonCachingStatement(statementId);
    if (statementInfo == null && nonCaching) {
      statementInfo = new StatementInfo();
      statementInfo.setStatementId(statementId);
      StatementParser.processSQL(boundSql,ms,parameter,statementInfo);
      if(!nonCaching) {
        cachedStatements.putIfAbsent(statementId, statementInfo);
      }
    }
    mpBoundSql.sql(statementInfo.getUpdatedSQL());
    StatementParser.processParameter(ms.getSqlCommandType(),boundSql,parameter,statementInfo);
  }


}
