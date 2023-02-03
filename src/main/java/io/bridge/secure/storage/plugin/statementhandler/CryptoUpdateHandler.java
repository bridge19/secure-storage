package io.bridge.secure.storage.plugin.statementhandler;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

@Component("updateHandler")
public class CryptoUpdateHandler implements ICryptoHandler{
  @Override
  public SqlCommandType support() {
    return SqlCommandType.UPDATE;
  }

  @Override
  public void beforeProcess(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql) {

  }
}
