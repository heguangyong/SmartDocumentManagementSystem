package com.github.sdms.util;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;

public class ApiResponseDecryptor {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        String clientSecret = "bf5a28d7ff094b6ec91cea1b655bf1863e02c2daa140598fcd198bcb0f0349fb";
        // 替换为完整 resultValue 字符串
        String encryptedBase64 = "gw7lENLeUfGwKspx+sU3J7VrlAB9960/PKr9+ZWmzRKDfIB0ILeIQQBDLubJwJKgFXPoFNfYB4ho+OiWyuZRJ3vwhfLBhoaU3Ce5F/epThCJL2ol62UJQZaNiQgskpx8xp/aR3t9ohe/zwaYZfGM/GyZtsPzbvRMymP7j9bABFkMfJ2Rs4Z8ZARecC0sKQ4uBvtsfMfg0VBg51ng57QhoWM6e+SC2p0iwTvn1OuggrUg/obsbHOHrihrhWU7CSMikY98kVKioNi+YQ8NeYQMeizH0mNUW0wpBs36sYLXJq/AXTWbDPgCfI4kxa7ucOC9D3rE8eK/xDynCXN2oP91P5jeTiMm0FScpPQ+rbEgdfkWCNdTNTFochmD5BDYHfQ0xDPTpmkGaHuHVC1U7FMYI44jyG0zwcFxsa2dOCzKy5d4k2d22uNgmcJgEuSe/0d3Hd3VZFNcZxsREXgaH2AQkbJ2DmGFTVU1SvzQb76e//SmZtBJOJcmJyxCWvsmiI1SLEsA7V/upemmy5bRF/mSRqfLCiAUgGMYBmQaHsOfmXkv5sQzo0cTAwnWi+q2q7gX1lSr2KlCEsDs7UE3+K/PEDIFw8XuaWQOyqMRqWhLVsybg2wbWlAoz6PBcQlXhka80SGNH5EUDAj3YxDUukiuUtk6heeL+cIi9B2W2b4qa1+ROaLkZpc52IripUa+siBIODHA9nMMuSeeIoMbpcTV+rVjiLaQiyoo+VamxjtHma1QRuZGj2hniIdi1fKcDADxBHxaXjcQUewEqqSC7NawNU+FFMLRChkeEeDMJzJdtSMRUN2YK6Fn5XprW0ThfyVhCWVr6AD5L7b1UJUQb9rC7KHIYpPsHjfKXI32eqYVtD7JY8R3g2wiJ7Rf3YJ7DwV50RALMj9Edgxd8FH1ILL+Yy13uilmRYh8fO4j8ddDyOM42kANmIeAh9Dc4NTw/Rt/Mf47oLkPA5Su+Ydify4iQBoBRjHR5qoP5jgh2OAXC9Fhug5YoxFjdXaKtwmii7IqRgbrGkEIpGaEMOOEaUH+62xb44i+yuDPql2/eXgnY3PmPveiq+bnRP33oYqEPSh1oDGo/Jt1YB7JDem/O3N3/qoYMfLBIQSj5HhVnfPM9Ue6aCk44VPTQF7mft4OEtHc8zKNg/o8PUqdv0SWPFyjfEFJ30MBs3BL9E3F7CIGCjP89K73Br8PorrtQsqHuWmcdczl3CRKVQxbCUg26PBoaAOzpLJRR+zuODB7ZxiI6hl1ZUp3eVZmtlt5aDKoijdc8WzfN5wRETz196IHZPfEfIt57175nyUs8ATKf67WDSyFzZBwiZ+pdnvM5f6IOYqQL4Mudw3X27XgLeT6u07jDci6xkWwg4QBEw4BiJW5X1m2Qhw9hRDyf+TpeQIE+NSLMV8dqjc5vgdtUEoJ4uV3mw==";

        // Base64 解码
        byte[] encryptedBytes = Base64.decodeBase64(encryptedBase64);

        // 密钥和IV
        String key = clientSecret.substring(0, 32);
        String iv = clientSecret.substring(clientSecret.length() - 16);

        String decrypted = decryptAES(encryptedBytes, key, iv);
        System.out.println("解密后内容:");
        System.out.println(decrypted);
    }

    private static String decryptAES(byte[] encryptedData, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
