package com.bookmind.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthUserInfo {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
    private String provider; // "GOOGLE" or "FACEBOOK"

    public OAuthUserInfo() {
    }

    public OAuthUserInfo(String id, String email, String name, String avatarUrl, String provider) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
