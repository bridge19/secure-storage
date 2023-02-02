package io.bridge.secure.storage.cryptor;

public interface ICryptor {
  String encrypt(String value);
  String decrypt(String value);
}
