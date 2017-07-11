package com.android.example.architecture.db.entity;

import android.arch.persistence.room.Entity;

import com.google.gson.annotations.SerializedName;

@Entity(primaryKeys = "login")
public class User {

    @SerializedName("login")
    public final String login;

    @SerializedName("avatar_url")
    public final String avatarUrl;

    @SerializedName("name")
    public final String name;

    @SerializedName("company")
    public final String company;

    @SerializedName("repos_url")
    public final String reposUrl;

    @SerializedName("blog")
    public final String blog;

    public User(String login, String avatarUrl, String name, String company,
                String reposUrl, String blog) {
        this.login = login;
        this.avatarUrl = avatarUrl;
        this.name = name;
        this.company = company;
        this.reposUrl = reposUrl;
        this.blog = blog;
    }
}
