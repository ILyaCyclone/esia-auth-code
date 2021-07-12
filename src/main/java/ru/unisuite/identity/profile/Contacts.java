package ru.unisuite.identity.profile;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
public class Contacts {
    private List<String> emails;
    private List<String> mobilePhones;
    private List<String> homePhones;

    public void addEmail(String email) {
        if (emails == null) {
            emails = new ArrayList<>();
        }
        emails.add(email);
    }

    public void addMobilePhone(String mobilePhone) {
        if (mobilePhones == null) {
            mobilePhones = new ArrayList<>();
        }
        mobilePhones.add(mobilePhone);
    }
    public void addHomePhone(String homePhone) {
        if (homePhones == null) {
            homePhones = new ArrayList<>();
        }
        homePhones.add(homePhone);
    }

    public List<String> getEmails() {
        return safeGetter(emails);
    }

    public List<String> getMobilePhones() {
        return safeGetter(mobilePhones);
    }

    public List<String> getHomePhones() {
        return safeGetter(homePhones);
    }

    public List<String> safeGetter(List<String> list) {
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }
}
