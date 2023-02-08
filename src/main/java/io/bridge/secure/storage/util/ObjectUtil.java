package io.bridge.secure.storage.util;

import io.bridge.secure.storage.cryptor.ICryptor;

import java.lang.reflect.Field;
import java.util.Date;

public class ObjectUtil {

  public static void encryptObjectField(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> targetClass = obj.getClass();
    Field field = targetClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    if(iCryptor != null && fieldValue!=null) {
      String encryptValue = iCryptor.encrypt(fieldValue);
      field.set(obj,encryptValue);
    }
  }
  public static String encryptString(String targetStr, ICryptor iCryptor){
    return iCryptor.encrypt(targetStr);
  }
  public static Object getFieldValue(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> targetClass = obj.getClass();
    Field field = targetClass.getDeclaredField(targetField);
    field.setAccessible(true);
    Object fieldValue = (Object) field.get(obj);
    if(iCryptor != null && fieldValue!=null && fieldValue instanceof String) {
      fieldValue = iCryptor.encrypt((String)fieldValue);
    }
    return fieldValue;
  }
  public static void decryptObjectField(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> objClass = obj.getClass();
    Field field = objClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    if(fieldValue!=null) {
      fieldValue = iCryptor.decrypt(fieldValue);
    }
    field.set(obj,fieldValue);
  }
  public static void decryptObjectField(Class objClass, Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Field field = objClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    if(fieldValue!=null) {
      fieldValue = iCryptor.decrypt(fieldValue);
    }
    field.set(obj,fieldValue);
  }

  public static boolean isWrapperType(Object obj){
    return obj instanceof Integer
            || obj instanceof Double
            || obj instanceof Float
            || obj instanceof Long
            || obj instanceof Short
            || obj instanceof Boolean
            || obj instanceof Byte
            || obj instanceof String
            || obj instanceof Date;
  }
  public static boolean isPrimitive(Object obj){
    return obj.getClass().isPrimitive();
  }
  public static boolean isSimpleType(Object obj){
    return isPrimitive(obj) || isWrapperType(obj);
  }
}
