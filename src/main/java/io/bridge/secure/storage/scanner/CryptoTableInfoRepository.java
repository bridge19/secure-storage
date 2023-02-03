package io.bridge.secure.storage.scanner;

import io.bridge.secure.storage.cryptor.ICryptor;
import io.bridge.secure.storage.cryptor.SM4Cryptor;
import io.bridge.secure.storage.tokenizer.DefaultTokenizer;
import io.bridge.secure.storage.tokenizer.ITokenizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CryptoTableInfoRepository {

  private static Map<String, CryptoTableInfo> cryptoTableInfoMap = new ConcurrentHashMap<>();
  private static Map<Class, CryptoTableInfo> cryptoClassInfoMap = new ConcurrentHashMap<>();

  private static Map<String, ICryptor> cryptorMap = new ConcurrentHashMap<>();
  private static Map<String, ITokenizer> tokenizerMap = new ConcurrentHashMap<>();

  static {
    cryptorMap.put(SM4Cryptor.class.getName(),new SM4Cryptor());
    tokenizerMap.put(DefaultTokenizer.class.getName(),new DefaultTokenizer());
  }

  public static ICryptor getCryptor(Class cryptorClass){
    String clazzName = cryptorClass.getName();
    ICryptor cryptor = cryptorMap.get(clazzName);
    if(cryptor ==null){
      try {
        cryptor = (ICryptor)cryptorClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        log.warn(String.format("error for construct Cryptor [%s], use SM4Cryptor",clazzName),e);
        return cryptorMap.get(SM4Cryptor.class.getName());
      }
    }
    return cryptor;
  }
  public static ITokenizer getTokenizer(Class tokenizerClass){
    String clazzName = tokenizerClass.getName();
    ITokenizer tokenizer = tokenizerMap.get(clazzName);
    if(tokenizer ==null){
      try {
        tokenizer = (ITokenizer)tokenizerClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        log.warn(String.format("error for construct Tokenizer [%s], use DefaultTokenizer",clazzName),e);
        return tokenizerMap.get(DefaultTokenizer.class.getName());
      }
    }
    return tokenizer;
  }
  public static void storeCryptoTableInfo(CryptoTableInfo tableCryptoInfo){
    cryptoTableInfoMap.putIfAbsent(tableCryptoInfo.getTableName(),tableCryptoInfo);
    cryptoClassInfoMap.putIfAbsent(tableCryptoInfo.getJavaClass(),tableCryptoInfo);
  }
  public static CryptoTableInfo getCryptoTableInfo(String tableName){
    return cryptoTableInfoMap.get(tableName);
  }
  public static CryptoTableInfo getCryptoClassInfo(Class className){
    return cryptoClassInfoMap.get(className);
  }
  public static CryptoColumnInfo getCryptoColumnByFieldName(String tableName, String fieldName){
    CryptoTableInfo cryptoTableInfo = cryptoTableInfoMap.get(tableName);
    if(cryptoTableInfo==null)
      return null;
    Optional<CryptoColumnInfo> optional = cryptoTableInfo.getCryptoColumnInfoMap().values().stream().filter(item -> item.getFieldName().equals(fieldName)).findFirst();
    if(optional.isPresent()){
      return optional.get();
    }
    return null;
  }
  public static CryptoColumnInfo getCryptoColumnByColumnName(String tableName, String columnName){
    CryptoTableInfo cryptoTableInfo = cryptoTableInfoMap.get(tableName);
    if(cryptoTableInfo==null)
      return null;
    return cryptoTableInfo.getCryptoColumnInfoMap().get(columnName);
  }

}
