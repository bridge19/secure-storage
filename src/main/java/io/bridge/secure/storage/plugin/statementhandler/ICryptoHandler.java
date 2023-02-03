package io.bridge.secure.storage.plugin.statementhandler;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

public interface ICryptoHandler {

  SqlCommandType support();
  void beforeProcess(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql);
}
