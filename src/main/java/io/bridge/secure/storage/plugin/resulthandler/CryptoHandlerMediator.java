package io.bridge.secure.storage.plugin.resulthandler;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CryptoHandlerMediator implements ICryptoHandler, ApplicationContextAware{

  private ApplicationContext applicationContext;
  private Map<SqlCommandType, ICryptoHandler> cryptoHandlerMap = new ConcurrentHashMap<>();

  private ICryptoHandler instance(SqlCommandType sqlCommandType){
    ICryptoHandler handler = cryptoHandlerMap.get(sqlCommandType);
    if(handler == null){
      Map<String,ICryptoHandler> handlerMap = applicationContext.getBeansOfType(ICryptoHandler.class);
      handlerMap.values().stream().forEach(item->cryptoHandlerMap.putIfAbsent(item.support(),item));
      handler = cryptoHandlerMap.get(sqlCommandType);
    }
    return handler;
  }

  @Override
  public SqlCommandType support() {
    return SqlCommandType.UNKNOWN;
  }

  @Override
  public void beforeProcess(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql) {
    instance(ms.getSqlCommandType()).beforeProcess(executor,ms,parameter,boundSql);
  }
  @Override
  public void postProcess(Executor executor, MappedStatement ms, Object originalParameter, Object parameter) {
    instance(ms.getSqlCommandType()).postProcess(executor,ms,originalParameter,parameter);
  }

  @Override
  public void postProcess(Executor executor, MappedStatement ms, Object originalParameter, Object parameter, ResultHandler resultHandler) {
    instance(ms.getSqlCommandType()).postProcess(executor,ms,originalParameter,parameter,resultHandler);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
