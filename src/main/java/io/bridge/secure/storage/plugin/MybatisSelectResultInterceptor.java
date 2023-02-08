package io.bridge.secure.storage.plugin;

import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.util.ObjectUtil;
import io.bridge.secure.storage.scanner.CryptoColumnInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfo;
import io.bridge.secure.storage.scanner.CryptoTableInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Slf4j
@Intercepts(
  {
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
  }
)
public class MybatisSelectResultInterceptor implements Interceptor {
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object result = invocation.proceed();
    Object target = invocation.getTarget();
    Object[] args = invocation.getArgs();
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
