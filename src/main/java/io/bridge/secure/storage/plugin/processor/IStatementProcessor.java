package io.bridge.secure.storage.plugin.processor;

import io.bridge.secure.storage.annotation.statement.NonCached;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public interface IStatementProcessor {

  Set<String> nonCachingStatementSet = new HashSet<>();
  Set<String> classCheckedSet = new HashSet<>();
  void process(Executor executor,MappedStatement ms, Object parameter, BoundSql boundSql);

  default boolean nonCachingStatement(String statementId){
    String className = statementId.substring(0, statementId.lastIndexOf('.'));
    if(!classCheckedSet.contains(className)) {
      try {
        Class tClass = StatementProcessor.class.getClassLoader().loadClass(className);
        Method[] methods = tClass.getDeclaredMethods();
        for (Method method : methods) {
          if (method.getAnnotation(NonCached.class) != null) {
            nonCachingStatementSet.add(className+"."+method.getName());
          }
        }
      } catch (ClassNotFoundException e) {
        //do nothing
      }
      classCheckedSet.add(className);
    }
    return nonCachingStatementSet.contains(statementId);
  }
}
