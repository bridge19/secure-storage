package io.bridge.secure.storage.plugin;

import io.bridge.secure.storage.annotation.statement.EnableEncryption;
import io.bridge.secure.storage.annotation.statement.IgnoreEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

public interface IStatementChecker {

  Logger logger = LoggerFactory.getLogger(IStatementChecker.class);
  Set<String> ignoreStatements = new HashSet<>();
  Set<String> parsedMappers = new HashSet<>();
  default boolean needCrypto(String statementId){
    String className = statementId.substring(0, statementId.lastIndexOf('.'));
    if(!parsedMappers.contains(className)) {
      try {
        Class tClass = MybatisCryptoInnerInterceptor.class.getClassLoader().loadClass(className);
        List<String> ignoreMethods = new ArrayList<>();
        EnableEncryption enableEncryption = (EnableEncryption) tClass.getAnnotation(EnableEncryption.class);
        if(enableEncryption!=null && enableEncryption.ignoreMethods().length>0){
          ignoreMethods = Arrays.asList(enableEncryption.ignoreMethods());
        }
        boolean globalIgnore = false;
        List<String> enableMethods = new ArrayList<>();
        IgnoreEncryption ignoreEncryption = (IgnoreEncryption) tClass.getAnnotation(IgnoreEncryption.class);
        if(ignoreEncryption!=null){
          globalIgnore = true;
          enableMethods = Arrays.asList(ignoreEncryption.enableMethods());
        }
        Method[] methods = tClass.getMethods();
        for (Method method : methods) {
          if((globalIgnore && (method.getAnnotation(EnableEncryption.class) == null || enableMethods.contains(method.getName())))
                  || (!globalIgnore &&( method.getAnnotation(IgnoreEncryption.class) != null || ignoreMethods.contains(method.getName())))){
            ignoreStatements.add(className + "." + method.getName());
          }
        }
      } catch (ClassNotFoundException e) {
        logger.info("no class found for statement: " + statementId);
        return false;
      }
      parsedMappers.add(className);
    }
    return !ignoreStatements.contains(statementId);
  }
}
