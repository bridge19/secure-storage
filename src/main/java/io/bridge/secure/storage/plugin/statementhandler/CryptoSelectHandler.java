package io.bridge.secure.storage.plugin.statementhandler;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.annotation.statement.NonCached;
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
public class CryptoSelectHandler implements ICryptoHandler {
  private static final Map<String, StatementInfo> cachedStatements = new ConcurrentHashMap<>();

  @Override
  public SqlCommandType support() {
    return SqlCommandType.SELECT;
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
      boolean cachingSQL = checkCachingSQL(statementId);
      if(cachingSQL) {
        cachedStatements.putIfAbsent(statementId, statementInfo);
      }
    }
    mpBoundSql.sql(statementInfo.getUpdatedSQL());
    StatementParser.processParameter(ms.getSqlCommandType(),boundSql,parameter,statementInfo);
  }

  private boolean checkCachingSQL(String statementId){
    String className = statementId.substring(0, statementId.lastIndexOf('.'));
    String methodName = statementId.substring(statementId.lastIndexOf('.') + 1);
    boolean cachingSQL = true;
    try {
      Class tClass = CryptoSelectHandler.class.getClassLoader().loadClass(className);
      Method[] methods = tClass.getMethods();
      for (Method method : methods) {
        if (methodName.equals(method.getName())) {
          cachingSQL = method.getAnnotation(NonCached.class) == null;
        }
      }
    } catch (ClassNotFoundException e) {
      log.warn(String.format("Class is not found when parse select statement: %s",statementId),e);
      return false;
    }
    return cachingSQL;
  }
}
