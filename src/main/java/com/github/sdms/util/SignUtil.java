package com.github.sdms.util;

import com.github.sdms.dto.ThirdConfig;
import com.koalii.svs.client.Svs2ClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignUtil {

    private final ThirdConfig thirdConfig;
    private final RestTemplate restTemplate; // 改为 final，通过构造函数注入

    /**
     * 初始化 Svs2ClientHelper
     */
    public Svs2ClientHelper init() {
        Svs2ClientHelper helper = Svs2ClientHelper.getInstance();
        helper.init(thirdConfig.getSignHost(), thirdConfig.getSignPort(), thirdConfig.getSignTimeOut());
        return helper;
    }

    /**
     * 清理 Svs2ClientHelper 资源
     */
    public void close(Svs2ClientHelper helper) {
        if (helper != null) {
            helper.clean();
        }
    }

    /**
     * 获取签名数据 - 调用 SVS HTTP API
     */
    public String getSignB64SignedDataV2(String originDataStr) {
        if (originDataStr == null || originDataStr.isEmpty()) {
            log.warn("签名数据为空，跳过签名");
            return null;
        }

        try {
            // 1. Base64 编码原始数据
            String b64OriginData = Base64.getEncoder().encodeToString(
                    originDataStr.getBytes(StandardCharsets.UTF_8)
            );

            // 2. 构建请求 URL
            String url = String.format("http://%s:%s/api/svs/bss/signData",
                    thirdConfig.getSignHost(),
                    thirdConfig.getSignPort()
            );

            // 3. 构建请求体
            Map<String, Object> requestBody = buildSignRequestBody(b64OriginData);

            // 4. 发送请求
            ResponseEntity<Map> response = sendPostRequest(url, requestBody);

            // 5. 处理响应
            return handleSignResponse(response, originDataStr, b64OriginData);

        } catch (Exception e) {
            log.error("签名异常 - 原始数据: {}", originDataStr, e);
            return null;
        }
    }

    /**
     * 验证签名 - 调用 SVS HTTP API
     */
    public boolean validateSignB64SignedDataV2(String originDataStr, String b64SignedData) {
        if (originDataStr == null || originDataStr.isEmpty() ||
                b64SignedData == null || b64SignedData.isEmpty()) {
            log.warn("签名或原始数据为空，跳过验签");
            return false;
        }

        try {
            // 1. Base64 编码原始数据（保持和签名时一致）
            String b64OriginData = Base64.getEncoder().encodeToString(
                    originDataStr.getBytes(StandardCharsets.UTF_8)
            );

            // 2. 构建请求 URL
            String url = String.format("http://%s:%s/api/svs/bss/verifySignedData",
                    thirdConfig.getSignHost(),
                    thirdConfig.getSignPort()
            );

            // 3. 构建请求体
            Map<String, Object> requestBody = buildVerifyRequestBody(b64OriginData, b64SignedData);

            // 4. 发送请求
            ResponseEntity<Map> response = sendPostRequest(url, requestBody);

            // 5. 处理响应
            return handleVerifyResponse(response, originDataStr, b64OriginData, b64SignedData);

        } catch (Exception e) {
            log.error("验签异常 - 原始数据: {}, 签名: {}", originDataStr, b64SignedData, e);
            return false;
        }
    }

    /**
     * 尝试验证签名 - 安全包装方法
     */
    public boolean tryValidateSignV2(String origin, String signature) {
        try {
            return validateSignB64SignedDataV2(origin, signature);
        } catch (Exception e) {
            log.error("tryValidateSignV2 异常 - 原始数据: {}, 签名: {}", origin, signature, e);
            return false;
        }
    }

    /**
     * 完整的签名验签流程示例
     */
    public boolean signAndVerifyExample(String originData) {
        try {
            // 1. 签名
            String signature = getSignB64SignedDataV2(originData);
            if (signature == null) {
                log.error("签名失败，终止流程");
                return false;
            }

            // 2. 验签
            boolean verifyResult = validateSignB64SignedDataV2(originData, signature);

            log.info("完整签名验签流程结果 - 原始数据: {}, 签名: {}, 验签结果: {}",
                    originData, signature, verifyResult);

            return verifyResult;
        } catch (Exception e) {
            log.error("签名验签流程异常 - 原始数据: {}", originData, e);
            return false;
        }
    }

    // ===== 私有辅助方法 =====

    /**
     * 构建签名请求体
     */
    private Map<String, Object> buildSignRequestBody(String b64OriginData) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("b64OriginData", b64OriginData);
        requestBody.put("certAlias", thirdConfig.getSignCertId());
        requestBody.put("hashType", thirdConfig.getHashType());
        // 可选：添加 serviceName
        // requestBody.put("serviceName", "default");
        return requestBody;
    }

    /**
     * 构建验签请求体
     */
    private Map<String, Object> buildVerifyRequestBody(String b64OriginData, String b64SignedData) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("b64OriginData", b64OriginData);
        requestBody.put("b64SignedData", b64SignedData);
        requestBody.put("certAlias", thirdConfig.getSignCertId());
        requestBody.put("hashType", thirdConfig.getHashType());

        // 生产环境验证选项（可选，生产环境建议启用所有验证）
        Map<String, Boolean> verifyCertOpt = new HashMap<>();
        verifyCertOpt.put("allowListFlag", true);
        verifyCertOpt.put("certChainFlag", true);
        verifyCertOpt.put("certValidFlag", true);
        verifyCertOpt.put("crlFlag", true);
        verifyCertOpt.put("verifyCertFlag", true);
        requestBody.put("verifyCertOpt", verifyCertOpt);

        return requestBody;
    }

    /**
     * 发送 POST 请求
     */
    private ResponseEntity<Map> sendPostRequest(String url, Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForEntity(url, requestEntity, Map.class);
    }

    /**
     * 处理签名响应
     */
    private String handleSignResponse(ResponseEntity<Map> response, String originDataStr, String b64OriginData) {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String errorCode = String.valueOf(responseBody.get("errorCode"));

            if ("0".equals(errorCode)) {
                String b64SignedData = (String) responseBody.get("b64SignedData");
                log.info("签名成功 ✅ - 原始数据: {}, b64OriginData: {}, b64SignedData: {}",
                        originDataStr, b64OriginData, b64SignedData);
                return b64SignedData;
            } else {
                String message = (String) responseBody.get("message");
                log.error("签名失败 ❌ - 错误码: {}, 错误信息: {}, 原始数据: {}",
                        errorCode, message, originDataStr);
            }
        } else {
            log.error("签名请求失败 - HTTP状态码: {}, 原始数据: {}",
                    response.getStatusCode(), originDataStr);
        }
        return null;
    }

    /**
     * 处理验签响应
     */
    private boolean handleVerifyResponse(ResponseEntity<Map> response, String originDataStr,
                                         String b64OriginData, String b64SignedData) {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String errorCode = String.valueOf(responseBody.get("errorCode"));

            if ("0".equals(errorCode)) {
                log.info("验签成功 ✅ - 原始数据: {}, b64OriginData: {}, 签名: {}",
                        originDataStr, b64OriginData, b64SignedData);
                return true;
            } else {
                String message = (String) responseBody.get("message");
                log.error("验签失败 ❌ - 错误码: {}, 错误信息: {}, 原始数据: {}, 签名: {}",
                        errorCode, message, originDataStr, b64SignedData);
            }
        } else {
            log.error("验签请求失败 - HTTP状态码: {}, 原始数据: {}, 签名: {}",
                    response.getStatusCode(), originDataStr, b64SignedData);
        }
        return false;
    }
}