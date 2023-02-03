package io.bridge.secure.storage.plugin.processor;

import io.bridge.secure.storage.plugin.processor.statement.InsertProcessor;
import io.bridge.secure.storage.plugin.processor.statement.SelectProcessor;
import io.bridge.secure.storage.plugin.processor.statement.UpdateProcessor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatementProcessorFactory implements IStatementProcessor, ApplicationContextAware{

  private ApplicationContext applicationContext;
  private Map<SqlCommandType, IStatementProcessor> cryptoHandlerMap = new ConcurrentHashMap<>();

  public StatementProcessorFactory(){
    cryptoHandlerMap.put(SqlCommandType.SELECT,new SelectProcessor());
    cryptoHandlerMap.put(SqlCommandType.UPDATE,new UpdateProcessor());
    cryptoHandlerMap.put(SqlCommandType.INSERT,new InsertProcessor());
  }
  private IStatementProcessor instance(SqlCommandType sqlCommandType){
    IStatementProcessor handler = cryptoHandlerMap.get(sqlCommandType);
    if(handler == null){
      throw new RuntimeException("statement is not support: "+ sqlCommandType.name());
    }
    return handler;
  }

  @Override
  public SqlCommandType support() {
    return SqlCommandType.UNKNOWN;
  }

  @Override
  public void process(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql) {
    instance(ms.getSqlCommandType()).process(executor,ms,parameter,boundSql);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
