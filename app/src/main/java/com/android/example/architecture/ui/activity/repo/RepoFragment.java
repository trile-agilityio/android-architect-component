package com.android.example.architecture.ui.activity.repo;

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingComponent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.example.architecture.R;
import com.android.example.architecture.binding.FragmentDataBindingComponent;
import com.android.example.architecture.databinding.RepoFragmentBinding;
import com.android.example.architecture.db.entity.Repo;
import com.android.example.architecture.di.Injectable;
import com.android.example.architecture.ui.adapter.ContributorAdapter;
import com.android.example.architecture.ui.common.NavigationController;
import com.android.example.architecture.util.common.AutoClearedValue;
import com.android.example.architecture.util.common.Resource;

import java.util.Collections;

import javax.inject.Inject;

public class RepoFragment extends Fragment implements LifecycleRegistryOwner, Injectable {

    private static final String REPO_OWNER_KEY = "repo_owner";
    private static final String REPO_NAME_KEY = "repo_name";

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    @Inject
    public
    ViewModelProvider.Factory viewModelFactory;

    private RepoViewModel repoViewModel;

    @Inject
    public
    NavigationController navigationController;

    public DataBindingComponent dataBindingComponent = new FragmentDataBindingComponent(this);
    private AutoClearedValue<RepoFragmentBinding> binding;
    private AutoClearedValue<ContributorAdapter> adapter;

    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        RepoFragmentBinding dataBinding = DataBindingUtil
                .inflate(inflater, R.layout.repo_fragment, container, false);
        dataBinding.setRetryCallback(()->repoViewModel.retry());
        binding = new AutoClearedValue<>(this, dataBinding);

        return dataBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Init RepoViewModel
        repoViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(RepoViewModel.class);
        Bundle args = getArguments();

        if (args != null && args.containsKey(REPO_OWNER_KEY) &&
                args.containsKey(REPO_NAME_KEY)) {
            repoViewModel.setId(args.getString(REPO_OWNER_KEY),
                    args.getString(REPO_NAME_KEY));
        } else {
            repoViewModel.setId(null, null);
        }

        // Load list Repositories
        LiveData<Resource<Repo>> repo = repoViewModel.getRepo();
        repo.observe(this, resource -> {
            binding.get().setRepo(resource == null ? null : resource.data);
            binding.get().setRepoResource(resource);
            binding.get().executePendingBindings();
        });

        // Contributor adapter
        ContributorAdapter adapter = new ContributorAdapter(dataBindingComponent,
                contributor -> navigationController.navigateToUser(contributor.getLogin()));
        this.adapter = new AutoClearedValue<>(this, adapter);
        binding.get().contributorList.setAdapter(adapter);

        // Contributors list
        initContributorList(repoViewModel);
    }

    /**
     * Initialize Contributors list.
     *
     * @param viewModel The {@link RepoViewModel}
     */
    private void initContributorList(RepoViewModel viewModel) {
        viewModel.getContributors().observe(this, listResource->{
            // don't need any null checks here for the adapter since LiveData guarantees that
            // it won't call if fragment is stopped or not started.
            if (listResource != null && listResource.data != null) {
                adapter.get().replace(listResource.data);
            } else {
                //noinspection ConstantConditions
                adapter.get().replace(Collections.emptyList());
            }
        });
    }

    /**
     * Create fragment.
     *
     * @param owner The {@link String}
     * @param name The {@link String}
     * @return
     */
    public static RepoFragment create(String owner, String name) {
        RepoFragment repoFragment = new RepoFragment();
        Bundle args = new Bundle();
        args.putString(REPO_OWNER_KEY, owner);
        args.putString(REPO_NAME_KEY, name);
        repoFragment.setArguments(args);
        return repoFragment;
    }
}
