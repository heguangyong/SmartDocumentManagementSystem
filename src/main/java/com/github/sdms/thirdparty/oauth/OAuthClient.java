package com.github.sdms.thirdparty.oauth;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
@Component
public class OAuthClient {

    @Value("${oauth.api_url}")
    private String oauthApiUrl;

    @Value("${oauth.client_id}")
    private String oauthClientId;

    @Value("${oauth.client_secret}")
    private String oauthClientSecret;

    @Value("${oauth.redirect_uri}")
    private String oauthRedirectUri;

    @Value("${oauth.scope}")
    private String oauthScope;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 根据 UID 获取用户信息（基于 GET，使用 WebClient）
     */
    public JSONObject userinfoByUID(String uid) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = DigestUtils.sha256Hex(oauthClientId + timestamp + oauthClientSecret);

        String url = oauthApiUrl + "rs/api/v5/infoByUID";
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("clientId", oauthClientId);
        queryParams.put("timestamp", timestamp);
        queryParams.put("sign", sign);
        queryParams.put("uid", uid);

        // 构造完整查询字符串
        StringBuilder uriBuilder = new StringBuilder(url).append("?");
        queryParams.forEach((k, v) -> uriBuilder.append(k).append("=").append(v).append("&"));
        String uri = uriBuilder.substring(0, uriBuilder.length() - 1);

        HttpHeaders headers = new HttpHeaders();
        headers.set("app_key", "202307210001");

        log.info("userinfoByUID request uri: {}", uri);

        String response = webClient.get()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("userinfoByUID response: {}", response);
        return JSONObject.parse(response);
    }

    /**
     * OAuth 授权重定向 URL 构造
     */
    public String oauthRedirectUri(String state) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", oauthClientId);
        params.put("redirect_uri", oauthRedirectUri);
        params.put("scope", oauthScope);
        params.put("state", System.currentTimeMillis() + "-" + state);
        params.put("user_type", "D0");
        params.put("relogin", "true");
        params.put("login_type", "home");

        StringBuilder sb = new StringBuilder(oauthApiUrl).append("oauth/authorize?");
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * 通过 code 换取 access_token
     */
    public String getOauthToken(String code) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", oauthClientId);
        params.put("client_secret", oauthClientSecret);
        params.put("redirect_uri", oauthRedirectUri);
        params.put("code", code);
        params.put("user_type", "D0");

        HttpHeaders headers = new HttpHeaders();
        headers.set("app_key", "202307210001");
        headers.set("content-type", "application/x-www-form-urlencoded");

        StringBuilder sb = new StringBuilder(oauthApiUrl).append("oauth/token?");
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        String uri = sb.substring(0, sb.length() - 1);

        log.info("getOauthToken request uri: {}", uri);

        String response = webClient.post()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("getOauthToken response: {}", response);
        return Objects.requireNonNull(JSONObject.parse(response)).getString("access_token");
    }

    /**
     * 根据 accessToken 获取用户信息
     */
    public JSONObject userinfoByAccessToken(String accessToken, String version) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = DigestUtils.sha256Hex(oauthClientId + timestamp + oauthClientSecret);
        String url = oauthApiUrl + "rs/api/" + version + "/info";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("clientId", oauthClientId);
        params.put("timestamp", timestamp);
        params.put("sign", sign);
        params.put("accessToken", accessToken);

        StringBuilder sb = new StringBuilder(url).append("?");
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        String uri = sb.substring(0, sb.length() - 1);

        HttpHeaders headers = new HttpHeaders();
        headers.set("app_key", "202307210001");

        log.info("userinfoByAccessToken request uri: {}", uri);

        String response = webClient.get()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("userinfoByAccessToken response: {}", response);
        return JSONObject.parse(response).getJSONObject("resultValue");
    }


    /**
     * 用户登出
     */
    public JSONObject userLogout(String accessToken) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = DigestUtils.sha256Hex(oauthClientId + timestamp + oauthClientSecret);

        String uri = oauthApiUrl + "rs/api/v1/logout?accessToken=" + accessToken
                + "&clientId=" + oauthClientId + "&timestamp=" + timestamp + "&sign=" + sign;

        HttpHeaders headers = new HttpHeaders();
        headers.set("app_key", "202307210001");
        headers.set("content-type", "application/x-www-form-urlencoded");

        log.info("userLogout request uri: {}", uri);

        String response = webClient.post()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("userLogout response: {}", response);
        return Objects.requireNonNull(JSONObject.parse(response)).getJSONObject("resultValue");
    }

    /**
     * （可选）使用 HttpURLConnection 直接调用接口（如有特殊需求）
     */
    public JSONObject userinfoByUIDUsingHttpURLConnection(String uid) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = DigestUtils.sha256Hex(oauthClientId + timestamp + oauthClientSecret);

        String urlStr = oauthApiUrl + "rs/api/v5/infoByUID?clientId=" + oauthClientId
                + "&timestamp=" + timestamp + "&sign=" + sign + "&uid=" + uid;

        log.info("userinfoByUIDUsingHttpURLConnection url: {}", urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("app_key", "202307210001");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.warn("userinfoByUIDUsingHttpURLConnection 请求失败，响应码: {}", responseCode);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            String respStr = responseBuilder.toString();
            log.info("userinfoByUIDUsingHttpURLConnection 响应内容: {}", respStr);
            return JSONObject.parse(respStr);
        }
    }
}
