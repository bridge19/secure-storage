package io.bridge.secure.storage.util.sm;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SM4Utils {
//	private String secretKey = "";
//    private String iv = "";
//    private boolean hexString = false;

    public String secretKey = "";
    private String iv = "";
    public boolean hexString = false;

    public SM4Utils()
    {
    }
//
//    public byte encryptData_ECB1(byte[] inputStream){
//        try
//        {
//            SM4_Context ctx = new SM4_Context();
//            ctx.isPadding = true;
//            ctx.mode = SM4.SM4_ENCRYPT;
//
//            byte[] keyBytes;
//            if (hexString)
//            {
//                keyBytes = Util.hexStringToBytes(secretKey);
//            }
//            else
//            {
//                keyBytes = secretKey.getBytes();
//            }
//
//            SM4 sm4 = new SM4();
//            sm4.sm4_setkey_enc(ctx, keyBytes);
//            String cipherText = new BASE64Encoder().encode(inputStream);
//            if (cipherText != null && cipherText.trim().length() > 0)
//            {
//                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
//                Matcher m = p.matcher(cipherText);
//                cipherText = m.replaceAll("");
//            }
//            return cipherText;
//        }
//        catch (Exception e)
//        {
//            return (Byte) null;
//        }
//    }

    public String encryptData_ECB(String plainText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_ENCRYPT;

            byte[] keyBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
            }
            else
            {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_enc(ctx, keyBytes);
            byte[] encrypted = sm4.sm4_crypt_ecb(ctx, plainText.getBytes("GB18030"));//--------------------------原来为GBK，修改为GB18030 GB18030收录的字符信息比GBK要多很多，避免GBK生僻字导致无法解析问题
            //String cipherText = new BASE64Encoder().encode(encrypted);
            String cipherText = Base64.getEncoder().encodeToString(encrypted);

            if (cipherText != null && cipherText.trim().length() > 0)
            {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(cipherText);
                cipherText = m.replaceAll("");
            }
            return cipherText;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String decryptData_ECB(String cipherText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_DECRYPT;

            byte[] keyBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
            }
            else
            {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_dec(ctx, keyBytes);
            //byte[] decrypted = sm4.sm4_crypt_ecb(ctx, new BASE64Decoder().decodeBuffer(cipherText));
            byte[] decrypted = sm4.sm4_crypt_ecb(ctx, Base64.getDecoder().decode(cipherText));
            return new String(decrypted, "GB18030");//--------------------------原来为GBK，修改为GB18030 GB18030收录的字符信息比GBK要多很多，避免GBK生僻字导致无法解析问题
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String encryptData_CBC(String plainText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_ENCRYPT;

            byte[] keyBytes;
            byte[] ivBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
                ivBytes = Util.hexStringToBytes(iv);
            }
            else
            {
                keyBytes = secretKey.getBytes();
                ivBytes = iv.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_enc(ctx, keyBytes);
            byte[] encrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, plainText.getBytes("GB18030"));//--------------------------原来为GBK，修改为GB18030 GB18030收录的字符信息比GBK要多很多，避免GBK生僻字导致无法解析问题
            //String cipherText = new BASE64Encoder().encode(encrypted);
            String cipherText = Base64.getEncoder().encodeToString(encrypted);
            if (cipherText != null && cipherText.trim().length() > 0)
            {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(cipherText);
                cipherText = m.replaceAll("");
            }
            return cipherText;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String decryptData_CBC(String cipherText)
    {
        try
        {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_DECRYPT;

            byte[] keyBytes;
            byte[] ivBytes;
            if (hexString)
            {
                keyBytes = Util.hexStringToBytes(secretKey);
                ivBytes = Util.hexStringToBytes(iv);
            }
            else
            {
                keyBytes = secretKey.getBytes();
                ivBytes = iv.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_dec(ctx, keyBytes);
            //byte[] decrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, new BASE64Decoder().decodeBuffer(cipherText));
            byte[] decrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, Base64.getDecoder().decode(cipherText));
            return new String(decrypted, "GB18030");//--------------------------原来为GBK，修改为GB18030 GB18030收录的字符信息比GBK要多很多，避免GBK生僻字导致无法解析问题
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static void main(String[] args) throws IOException
    {

        //String plainText =  "{\"vehicleQueryInfo\": {\"businessSn\": \"800002201906270010000001\", \"busiType\": \"1001\",\"vehicleNo\": \"冀F716AD\",\"plateColorCode\": \"1\", \"transCertificateCode\": \"130682003533\",\"applyName\": \"测试\",\"idType\": \"1\",\"idCard\": \"372922198623057516\", \"applyDate\": \"20190627121030\",\"base64Binary\": \"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\"}}";
        String plainText =  "{\"crcCode\": \"928607C10BB78054A1E838A999600B04D478AD088900A28EEC59ED09B4BD122A\",{\"vehicleCheckInfo\": {\"applyName\": \"李伟\",\"applyDate\": \"20200709130642\",\"businessSn\": \"80002001202007090000001\", \"creditCode\" : \"123456789012345678\", \"licenseCode\" : \"530000001609\",\"vehicleNo\": \"云G42827\",\"plateColorCode\": \"2\",\"transCertificateCode\": \"532522008623\",\"ownerName\": \"李伟\",\"vinNo\": \"015097\",\"vehicleZoneCode\": \"530000\",\"contactName\": \"联众\",\"telephone\": \"18600011112\"},\"certificateInfo\": [{\"imageType\": \"3\",\"base64Binary\": \"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\"},{\"imageType\": \"21\",\"base64Binary\": \"\"}]}";

        String plainText2 =   "AQO5JRn63qgmzFdnFtr4l7MpaogA1X098sUsyfztZ5a5PZcG1uIf914bAutl6d8DuXPy21p4w8iq93z750d9SRld3mlq+aNqCmCvua9E6xmbLrJE9MYp3aGIu/7hkk/1LqJPHvLPMDL+eNVLC3PqTrXQY7T8dYIOCE+LQtvmlmGx7flxwKLlBlW44NgnPovLa6bag6C5rDT2t0Tfi6c6kQ==";

        SM4Utils sm4 = new SM4Utils();
        sm4.secretKey = "GPv2dLUnDIMhZZ4N";
        sm4.hexString = false;

        System.out.println("ECB模式加密");
        String cipherText = sm4.encryptData_ECB(plainText);
        System.out.println("密文: " + cipherText);
        System.out.println("");

        plainText = sm4.decryptData_ECB(plainText2);
        System.out.println("明文: " + plainText);
        System.out.println("");

        System.out.println("CBC模式加密");
        sm4.iv = "JzcSAOlhMFD3EHpC";
        cipherText = sm4.encryptData_CBC(plainText);
        System.out.println("密文: " + cipherText);
        System.out.println("");

        plainText = sm4.decryptData_CBC(cipherText);
        System.out.println("明文: " + plainText);

    }
}

