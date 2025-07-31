package com.github.sdms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    public String nameCn;
    public String namePinyin;
    public String vpnType;
    public String identityType;
    public String identityNo;
    public String uid;
    public String userType;
    public String sex;
    public String email;
    public String mobile;
    public int emailIsCheck;
    public int mobileIsCheck;
    public String birthday;
    public String homeTel;
    public String ldapUid;
    public String readerId;
    public String shlibBorrower;
    public String username;
    public String avatar;
    public String country;
    public String degree;
    public String metier;
    public String workCompanyClass;
    public String householdRegister;
    public String permanentAddress;
    public String officeName;
    public String telephoneNumber;
    public List<ShlibCard> shlibCardVoList;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShlibCard {
        public String shlibLibId;
        public String shlibCardNo;
        public String shlibCardSid;
        public String shlibCardStatus;
        public String shlibCardFunction;
        public String shlibCardType;
        private String latestSyncTime;
    }
}
