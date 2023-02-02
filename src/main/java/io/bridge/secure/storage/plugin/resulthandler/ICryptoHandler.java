package io.bridge.secure.storage.plugin.resulthandler;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;

public interface ICryptoHandler {

  SqlCommandType support();
  void beforeProcess(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql);
  void postProcess(Executor executor, MappedStatement ms,Object originalParameter, Object parameter);
  void postProcess(Executor executor, MappedStatement ms,Object originalParameter, Object parameter, ResultHandler resultHandler);
}
