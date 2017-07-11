package com.android.example.architecture.db.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.android.example.architecture.db.dao.RepoDao;
import com.android.example.architecture.db.dao.UserDao;
import com.android.example.architecture.db.entity.Contributor;
import com.android.example.architecture.db.entity.Repo;
import com.android.example.architecture.db.entity.RepoSearchResult;
import com.android.example.architecture.db.entity.User;

/**
 * Main database description.
 */
@Database(entities = {User.class, Repo.class, Contributor.class,
        RepoSearchResult.class}, version = 1)
public abstract class GithubDb extends RoomDatabase {

    abstract public UserDao userDao();

    abstract public RepoDao repoDao();
}
