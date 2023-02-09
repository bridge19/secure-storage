package io.bridge.secure.storage.plugin;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.plugin.processor.IStatementProcessor;
import io.bridge.secure.storage.plugin.processor.StatementProcessor;
import io.bridge.secure.storage.util.ObjectUtil;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Intercepts(
  {
    @Signature(type = StatementHandler.class, method = "parameterize", args = {Statement.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
  }
)
public class MybatisSelectResultInterceptor implements Interceptor, IStatementChecker {
  private IStatementProcessor statementProcessor = StatementProcessor.getInstance();
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object target = invocation.getTarget();
    Object[] args = invocation.getArgs();
    if(target instanceof StatementHandler){
      StatementHandler sh = (StatementHandler) target;
      PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
      MappedStatement ms = mpSh.mappedStatement();
      String statementId = ms.getId();
      if(needCrypto(statementId) && ms.getSqlCommandType()!=SqlCommandType.SELECT){
        long start = System.currentTimeMillis();
        BoundSql boundSql = mpSh.boundSql();
        Executor executor = mpSh.executor();
        Object parameter = boundSql.getParameterObject();
        statementProcessor.process(executor,ms, parameter, boundSql);
        logger.info("parse sql time cost: " + (System.currentTimeMillis() - start));
      }
    }
    Object result = invocation.proceed();
    if (target instanceof Executor) {
      MappedStatement ms = (MappedStatement) args[0];
      if(ms.getSqlCommandType() == SqlCommandType.SELECT){
        if(result instanceof List){
            for(Object obj: (List)result){
              if(!decryptObject(obj)){
                break;
              }
            }
        }else {
          decryptObject(result);
        }
      }
    }
    return result;
  }

  private boolean decryptObject(Object result){
    boolean isSuperClass = false;
    Class clazz = result.getClass();
    String className = clazz.getName();
    CryptoTableInfo cryptoTableInfo = CryptoTableInfoRepository.getCryptoClassInfo(clazz);
    if (cryptoTableInfo == null) {
      clazz = clazz.getSuperclass();
      cryptoTableInfo = CryptoTableInfoRepository.getCryptoClassInfo(clazz);
      if (cryptoTableInfo == null) {
        return false;
      }
      isSuperClass = true;
    }

    for(CryptoColumnInfo item :cryptoTableInfo.getCryptoColumnInfoMap().values()){
      ICryptor cryptor = CryptoTableInfoRepository.getCryptor(item.getCryptor());
      try {
        if(isSuperClass){
          ObjectUtil.decryptObjectField(clazz,result, item.getFieldName(),cryptor);
        }else {
          ObjectUtil.decryptObjectField(result, item.getFieldName(),cryptor);
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.warn(String.format("handle result error for class [%s], field [].", className, item.getFieldName()), e);
      }
    }
    return true;
  }
}
