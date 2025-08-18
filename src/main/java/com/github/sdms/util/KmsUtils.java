package com.github.sdms.util;


import com.koal.kms.sdk.ed.KmsSdkException;
import com.koal.kms.sdk.ed.KmsUtil;
import com.koal.kms.sdk.ed.Mode;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

@Slf4j
public class KmsUtils {
    private final static  byte[] iv = "SCZgvdgVOwhwxRIg".getBytes();

    public static String encrypt(String plainTxt) throws KmsSdkException {
        byte[] kmsEncrypt =  KmsUtil.encrypt(plainTxt.getBytes(), Mode.CFB, iv);

        String cipherTxt = Base64.getEncoder().encodeToString(kmsEncrypt);
        return  cipherTxt;
    }


    public static String decrypt(String cipherTxt) throws KmsSdkException {

        byte[] encodeStr = java.util.Base64.getDecoder().decode(cipherTxt);

        byte[] kmsDecrypt = KmsUtil.decrypt(encodeStr, Mode.CFB, iv);

        String plainTxt = new String(kmsDecrypt);
        return plainTxt;
    }

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 16;


    public static String generateRandomString() {
        StringBuilder sb = new StringBuilder();
        Random random = new SecureRandom();
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }


    public static void main(String[] args) {
        // 制作一批加密的用户数据存储数据库
        try {

            byte[] nameEncrypt = KmsUtil.encrypt("test".getBytes(), Mode.CFB, iv);
            byte[] mobileEncrypt = KmsUtil.encrypt("15618XXX573".getBytes(), Mode.CFB, iv);
            byte[] passwordEncrypt = KmsUtil.encrypt("heqm123.".getBytes(), Mode.CFB, iv);
            log.info("贺俏妹\nnameEncrypt:{}\nmobileEncrypt:{}\npasswordEncrypt:{}",
                    Base64.getEncoder().encodeToString(nameEncrypt), Base64.getEncoder().encodeToString(mobileEncrypt),
                    Base64.getEncoder().encodeToString(passwordEncrypt));
            byte[] encodeStr = java.util.Base64.getDecoder().decode(Base64.getEncoder().encodeToString(nameEncrypt));

            byte[] kmsDecrypt = KmsUtil.decrypt(encodeStr, Mode.CFB, iv);

            String plainTxt = new String(kmsDecrypt);
            log.info("plainTxt:{}",plainTxt);
        } catch (KmsSdkException e) {
            e.printStackTrace();
        }

    }
}
