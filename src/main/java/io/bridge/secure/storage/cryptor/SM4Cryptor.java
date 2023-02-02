package io.bridge.secure.storage.cryptor;

import io.bridge.secure.storage.util.sm.SM4Utils;

public class SM4Cryptor implements ICryptor{

  private SM4Utils sm4Utils;

  public SM4Cryptor(){
    sm4Utils = new SM4Utils();
    sm4Utils.secretKey="xxx0123456789xxx";   //16字符 128字节
  }

  @Override
  public String encrypt(String value) {
    return sm4Utils.encryptData_ECB(value);
  }

  @Override
  public String decrypt(String value) {
    return sm4Utils.decryptData_ECB(value);
  }
}
