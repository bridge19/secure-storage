package io.bridge.secure.storage.util;

import io.bridge.secure.storage.cryptor.ICryptor;

import java.lang.reflect.Field;

public class ObjectUtil {

  public static void encryptObject(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> targetClass = obj.getClass();
    Field field = targetClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    String encryptValue = iCryptor.encrypt(fieldValue);
    field.set(obj,encryptValue);
  }
  public static String encryptString(String targetStr, ICryptor iCryptor){
    return iCryptor.encrypt(targetStr);
  }
  public static Object fieldValue(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> targetClass = obj.getClass();
    Field field = targetClass.getDeclaredField(targetField);
    field.setAccessible(true);
    Object fieldValue = field.get(obj);
    if(iCryptor != null) {
      fieldValue = iCryptor.decrypt(String.valueOf(fieldValue));
    }
    return fieldValue;
  }
  public static void decryptObject(Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Class<?> objClass = obj.getClass();
    Field field = objClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    fieldValue = iCryptor.decrypt(fieldValue);
    field.set(obj,fieldValue);
  }
  public static void decryptObject(Class objClass, Object obj, String targetField, ICryptor iCryptor) throws NoSuchFieldException, IllegalAccessException {
    Field field = objClass.getDeclaredField(targetField);
    field.setAccessible(true);
    String fieldValue = (String) field.get(obj);
    fieldValue = iCryptor.decrypt(fieldValue);
    field.set(obj,fieldValue);
  }
}
