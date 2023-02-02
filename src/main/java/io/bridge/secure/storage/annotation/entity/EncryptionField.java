package io.bridge.secure.storage.annotation.entity;

import io.bridge.secure.storage.cryptor.SM4Cryptor;
import io.bridge.secure.storage.tokenizer.DefaultTokenizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptionField {
  String columnName();
  boolean fuzzySearch() default false;
  Class<?> getTokenizer() default DefaultTokenizer.class;
  Class<?> getCryptor() default SM4Cryptor.class;
}
