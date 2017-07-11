package com.android.example.architecture.di;

import android.app.Application;
import android.arch.persistence.room.Room;

import com.android.example.architecture.db.dao.RepoDao;
import com.android.example.architecture.db.dao.UserDao;
import com.android.example.architecture.db.database.GithubDb;
import com.android.example.architecture.service.networking.api.IGithubApi;
import com.android.example.architecture.ui.adapter.LiveDataCallAdapterFactory;
import com.android.example.architecture.util.constant.Config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module(includes = ViewModelModule.class)
class AppModule {

    @Singleton
    @Provides
    IGithubApi provideGithubService() {
        return new Retrofit.Builder()
                .baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(new LiveDataCallAdapterFactory())
                .build()
                .create(IGithubApi.class);
    }

    @Singleton
    @Provides
    GithubDb provideDb(Application app) {
        return Room.databaseBuilder(app, GithubDb.class, Config.GITHUB_DB_NAME).build();
    }

    @Singleton
    @Provides
    UserDao provideUserDao(GithubDb db) {
        return db.userDao();
    }

    @Singleton
    @Provides
    RepoDao provideRepoDao(GithubDb db) {
        return db.repoDao();
    }
}