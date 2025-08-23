package com.github.sdms.util;


import com.github.sdms.dto.ThirdConfig;
import com.koalii.svs.client.Svs2ClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignUtil {

    private final ThirdConfig thirdConfig;

    public Svs2ClientHelper init(){
        Svs2ClientHelper helper = Svs2ClientHelper.getInstance();
        helper.init(thirdConfig.getSignHost(), thirdConfig.getSignPort(),thirdConfig.getSignTimeOut());
        return helper;
    }

    public void close(Svs2ClientHelper helper){
        helper.clean();
    }

    public String getSignB64SignedData(String originDataStr, Svs2ClientHelper helper){
        try {
            String certId = thirdConfig.getSignCertId();
            byte[] originData = originDataStr.getBytes();
            Svs2ClientHelper.SvsResultData result = helper.cdbSignData(originData,certId);
            String b64SignedData = result.m_b64SignedData;
            log.info("签名数据originDataStr:{}, b64SignedData:{} ", originDataStr, b64SignedData);

            return b64SignedData;
        } catch (Exception e){
            log.error("getSignB64SignedData:", e);
        }
        return null;

    }


    public void validateSignB64SignedData(String originDataStr, Svs2ClientHelper helper, String b64SignedData) {
        try {
            byte[] originData = originDataStr.getBytes();
            String certId = thirdConfig.getSignCertId();
            Svs2ClientHelper.SvsResultData 	svsResultData = helper.cdbVerifySign(originData,b64SignedData,certId);
            Boolean flag = (svsResultData.m_errno == 0);
            if (flag){
                log.info("签名验签处理-签名验签通过-数据 originDataStr：{}， 签名：{}，签名验签结果：{}", originDataStr, b64SignedData,  Boolean.TRUE);
            } else {
                log.error("签名验签-数据被篡改-数据 originDataStr：{}， 签名：{}，签名验签结果：{}", originDataStr, b64SignedData, Boolean.FALSE);
            }
        } catch (Exception e) {
            log.error("validateSignB64SignedData", e);
        }
    }

    public boolean tryValidateSign(String origin, Svs2ClientHelper helper, String signature) {
        try {
            validateSignB64SignedData(origin, helper, signature);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static void main(String[] args) {

        // 签名测试
        Svs2ClientHelper helper = Svs2ClientHelper.getInstance();
        helper.init("172.24.88.208", 5001, 60);
        String certId = "SHKXJSQBYJS";
        byte[] originData = "5Tlh4E65zA==".getBytes();
        Svs2ClientHelper.SvsResultData result = helper.cdbSignData(originData,certId);
        String b64SignedData = result.m_b64SignedData;

        log.info("5Tlh4E65zA==:{}", b64SignedData);

        originData = "lXQ2uQrijl6eit8=".getBytes();
        result = helper.cdbSignData(originData,certId);
        b64SignedData = result.m_b64SignedData;

        log.info("lXQ2uQrijl6eit8=:{}", b64SignedData);

        originData = "xTlh4E65zCTI1NmirbU=".getBytes();
        result = helper.cdbSignData(originData,certId);
        b64SignedData = result.m_b64SignedData;

        log.info("xTlh4E65zCTI1NmirbU=:{}", b64SignedData);
//
//        byte[] originData2 = "AUDITOR_LOG".getBytes();
//
//        Svs2ClientHelper.SvsResultData 	svsResultData = helper.cdbVerifySign(originData2,b64SignedData,certId);
//        Boolean flag = (svsResultData.m_errno == 0);
//        if (flag) {
//            log.info("签名验签-通过-数据originData：{}，\n 签名：{}，\n 签名验签结果：{}", originData2.toString(), b64SignedData,  Boolean.TRUE);
//        } else {
//            log.error("签名验签-数据被篡改-数据originData：{}， 签名：{}，签名验签结果：{}", originData2, b64SignedData, Boolean.FALSE);
//        }

        helper.clean();
    }


}
