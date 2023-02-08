package io.bridge.secure.storage.plugin;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import io.bridge.secure.storage.annotation.statement.EnableEncryption;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import io.bridge.secure.storage.plugin.processor.IStatementProcessor;
import io.bridge.secure.storage.plugin.processor.StatementProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MybatisCryptoInnerInterceptor implements InnerInterceptor {

  private IStatementProcessor statementProcessor = new StatementProcessor();
  private Set<String> ignoreStatements = new HashSet<>();
  private Set<String> parsedMappers = new HashSet<>();
  @Override
  public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
    PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
    MappedStatement ms = mpSh.mappedStatement();
    String statementId = ms.getId();
    if(!needCrypto(statementId) || ms.getSqlCommandType()==SqlCommandType.SELECT){
      return;
    }
    long start = System.currentTimeMillis();
    BoundSql boundSql = mpSh.boundSql();
    Executor executor = mpSh.executor();
    Object parameter = boundSql.getParameterObject();
    statementProcessor.process(executor,ms, parameter, boundSql);
    log.info("parse sql time cost: " + (System.currentTimeMillis() - start));
  }
  @Override
  public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    String statementId = ms.getId();
    if(statementId.endsWith("_QUERY_ID") || !needCrypto(statementId)){
      return;
    }
    long start = System.currentTimeMillis();
    statementProcessor.process(executor,ms, parameter, boundSql);
    log.info("parse sql time cost: " + (System.currentTimeMillis() - start));
  }

  private boolean needCrypto(String statementId){
    String className = statementId.substring(0, statementId.lastIndexOf('.'));
    if(!parsedMappers.contains(className)) {
      try {
        Class tClass = MybatisCryptoInnerInterceptor.class.getClassLoader().loadClass(className);
        String[] ignoreMethods = new String[0];
        EnableEncryption enableEncryption = (EnableEncryption) tClass.getAnnotation(EnableEncryption.class);
        if(enableEncryption!=null){
          ignoreMethods = enableEncryption.ignoreMethods();
        }
        IgnoreEncryption ignoreEncryption = (IgnoreEncryption) tClass.getAnnotation(IgnoreEncryption.class);
        boolean globalIgnore = false;
        if(ignoreEncryption!=null){
          globalIgnore = true;
        }
        Method[] methods = tClass.getMethods();
        for (Method method : methods) {
          if(globalIgnore || method.getAnnotation(IgnoreEncryption.class) != null || (ignoreMethods.length>0 && Arrays.stream(ignoreMethods).anyMatch(item->item.equals(method.getName())))){
            ignoreStatements.add(className + "." + method.getName());
          }
        }
      } catch (ClassNotFoundException e) {
        log.info("no class found for statement: " + statementId);
        return false;
      }
      parsedMappers.add(className);
    }
    return !ignoreStatements.contains(statementId);
  }
}
