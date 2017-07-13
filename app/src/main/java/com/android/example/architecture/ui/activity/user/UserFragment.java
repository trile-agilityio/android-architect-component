package com.android.example.architecture.ui.activity.user;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingComponent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.example.architecture.R;
import com.android.example.architecture.binding.FragmentDataBindingComponent;
import com.android.example.architecture.databinding.UserFragmentBinding;
import com.android.example.architecture.di.Injectable;
import com.android.example.architecture.ui.adapter.RepoListAdapter;
import com.android.example.architecture.ui.common.NavigationController;
import com.android.example.architecture.util.common.AutoClearedValue;

import javax.inject.Inject;

public class UserFragment extends LifecycleFragment implements Injectable {
    private static final String LOGIN_KEY = "login";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    NavigationController navigationController;

    public DataBindingComponent dataBindingComponent = new FragmentDataBindingComponent(this);
    private UserViewModel userViewModel;
    private AutoClearedValue<UserFragmentBinding> binding;
    private AutoClearedValue<RepoListAdapter> adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        UserFragmentBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.user_fragment,
                container, false, dataBindingComponent);
        dataBinding.setRetryCallback(() -> userViewModel.retry());
        binding = new AutoClearedValue<>(this, dataBinding);

        return dataBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // View model
        userViewModel = ViewModelProviders.of(this, viewModelFactory).get(UserViewModel.class);
        userViewModel.setLogin(getArguments().getString(LOGIN_KEY));

        // User data observe
        userViewModel.getUser().observe(this, userResource -> {
            // update ui
            binding.get().setUser(userResource == null ? null : userResource.data);
            binding.get().setUserResource(userResource);
            binding.get().executePendingBindings();
        });

        // Repositories adapter
        RepoListAdapter rvAdapter = new RepoListAdapter(dataBindingComponent, false,
                repo->navigationController.navigateToRepo(repo.owner.login, repo.name));
        binding.get().repoList.setAdapter(rvAdapter);
        this.adapter = new AutoClearedValue<>(this, rvAdapter);

        initRepoList();
    }

    /**
     * Initialize list Repositories
     */
    private void initRepoList() {
        userViewModel.getRepositories().observe(this, repos -> {
            if (repos == null) {
                adapter.get().replace(null);
            } else {
                adapter.get().replace(repos.data);
            }
        });
    }

    /**
     * Create UserFragment.
     *
     * @param login {@link String}
     * @return UserFragment
     */
    public static UserFragment create(String login) {
        UserFragment userFragment = new UserFragment();
        Bundle bundle = new Bundle();
        bundle.putString(LOGIN_KEY, login);
        userFragment.setArguments(bundle);
        return userFragment;
    }
}