package com.bookmind.service;

import com.bookmind.dto.OAuthUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @org.springframework.beans.factory.annotation.Autowired
    public OAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${google.oauth2.client-id:}")
    private String googleClientId;

    @Value("${google.oauth2.client-secret:}")
    private String googleClientSecret;

    @Value("${google.oauth2.redirect-uri:http://localhost:8080/auth/google/callback}")
    private String googleRedirectUri;

    @Value("${facebook.oauth2.app-id:}")
    private String facebookAppId;

    @Value("${facebook.oauth2.app-secret:}")
    private String facebookAppSecret;

    @Value("${facebook.oauth2.redirect-uri:http://localhost:8080/auth/facebook/callback}")
    private String facebookRedirectUri;

    public boolean isGoogleConfigured() {
        return googleClientId != null && !googleClientId.isBlank()
                && googleClientSecret != null && !googleClientSecret.isBlank()
                && !googleClientId.contains("YOUR_GOOGLE_CLIENT_ID");
    }

    public boolean isFacebookConfigured() {
        return facebookAppId != null && !facebookAppId.isBlank()
                && facebookAppSecret != null && !facebookAppSecret.isBlank()
                && !facebookAppId.contains("YOUR_FACEBOOK_APP_ID");
    }

    public String buildGoogleAuthUrl(String state) {
        String encodedRedirect = URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8);
        String encodedState = (state != null && !state.isBlank()) ? URLEncoder.encode(state, StandardCharsets.UTF_8) : "";
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid profile email", StandardCharsets.UTF_8)
                + "&state=" + encodedState
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    public OAuthUserInfo processGoogleCallback(String code) throws Exception {
        log.info("Exchanging Google authorization code for access token...");
        String formBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(googleClientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() >= 400) {
            log.error("Google token exchange error {}: {}", tokenResponse.statusCode(), tokenResponse.body());
            throw new RuntimeException("Không thể xác thực mã từ Google: " + tokenResponse.body());
        }

        JsonNode tokenNode = objectMapper.readTree(tokenResponse.body());
        String accessToken = tokenNode.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new RuntimeException("Google không trả về access token hợp lệ");
        }

        // Fetch Google User Profile
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
        if (userResponse.statusCode() >= 400) {
            throw new RuntimeException("Không thể lấy hồ sơ người dùng từ Google");
        }

        JsonNode userNode = objectMapper.readTree(userResponse.body());
        String sub = userNode.path("sub").asText();
        String email = userNode.path("email").asText();
        String name = userNode.path("name").asText();
        String picture = userNode.path("picture").asText();

        log.info("Google user authenticated: email={}, name={}", email, name);
        return new OAuthUserInfo(sub, email, name, picture, "GOOGLE");
    }

    public String buildFacebookAuthUrl(String state) {
        String encodedRedirect = URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8);
        String encodedState = (state != null && !state.isBlank()) ? URLEncoder.encode(state, StandardCharsets.UTF_8) : "";
        return "https://www.facebook.com/v19.0/dialog/oauth"
                + "?client_id=" + facebookAppId
                + "&redirect_uri=" + encodedRedirect
                + "&scope=" + URLEncoder.encode("email,public_profile", StandardCharsets.UTF_8)
                + "&state=" + encodedState;
    }

    public OAuthUserInfo processFacebookCallback(String code) throws Exception {
        log.info("Exchanging Facebook authorization code for access token...");
        String tokenUrl = "https://graph.facebook.com/v19.0/oauth/access_token"
                + "?client_id=" + URLEncoder.encode(facebookAppId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(facebookAppSecret, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() >= 400) {
            log.error("Facebook token error {}: {}", tokenResponse.statusCode(), tokenResponse.body());
            throw new RuntimeException("Không thể nhận access token từ Facebook: " + tokenResponse.body());
        }

        JsonNode tokenNode = objectMapper.readTree(tokenResponse.body());
        String accessToken = tokenNode.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new RuntimeException("Facebook không trả về access token hợp lệ");
        }

        // Fetch Facebook profile & picture
        String userUrl = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token="
                + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(URI.create(userUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
        if (userResponse.statusCode() >= 400) {
            throw new RuntimeException("Không thể lấy hồ sơ người dùng từ Facebook");
        }

        JsonNode userNode = objectMapper.readTree(userResponse.body());
        String fbId = userNode.path("id").asText();
        String name = userNode.path("name").asText();
        String email = userNode.path("email").asText();
        String picture = "";
        if (userNode.has("picture") && userNode.path("picture").has("data")) {
            picture = userNode.path("picture").path("data").path("url").asText();
        }

        if (email.isBlank()) {
            email = "fb_" + fbId + "@facebook.com";
        }

        log.info("Facebook user authenticated: id={}, email={}, name={}", fbId, email, name);
        return new OAuthUserInfo(fbId, email, name, picture, "FACEBOOK");
    }
}
