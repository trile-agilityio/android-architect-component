package com.android.example.architecture.di;

import com.android.example.architecture.ui.activity.repo.RepoFragment;
import com.android.example.architecture.ui.activity.search.SearchFragment;
import com.android.example.architecture.ui.activity.user.UserFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class FragmentBuildersModule {

    @ContributesAndroidInjector
    abstract RepoFragment contributeRepoFragment();

    @ContributesAndroidInjector
    abstract UserFragment contributeUserFragment();

    @ContributesAndroidInjector
    abstract SearchFragment contributeSearchFragment();
}
