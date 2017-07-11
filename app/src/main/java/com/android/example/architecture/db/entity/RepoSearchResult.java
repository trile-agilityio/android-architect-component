package com.android.example.architecture.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.Nullable;

import com.android.example.architecture.db.GithubTypeConverters;

import java.util.List;

@Entity(primaryKeys = {"query"})
@TypeConverters(GithubTypeConverters.class)
public class RepoSearchResult {

    public final String query;
    public final List<Integer> repoIds;
    public final int totalCount;

    @Nullable
    public final Integer next;

    public RepoSearchResult(String query, List<Integer> repoIds, int totalCount,
                            Integer next) {
        this.query = query;
        this.repoIds = repoIds;
        this.totalCount = totalCount;
        this.next = next;
    }
}
