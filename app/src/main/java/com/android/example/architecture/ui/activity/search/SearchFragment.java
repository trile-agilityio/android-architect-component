package com.android.example.architecture.ui.activity.search;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingComponent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import com.android.example.architecture.R;
import com.android.example.architecture.binding.FragmentDataBindingComponent;
import com.android.example.architecture.databinding.SearchFragmentBinding;
import com.android.example.architecture.di.Injectable;
import com.android.example.architecture.ui.adapter.RepoListAdapter;
import com.android.example.architecture.ui.common.NavigationController;
import com.android.example.architecture.util.common.AutoClearedValue;
import com.android.example.architecture.util.view.ViewUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class SearchFragment extends LifecycleFragment implements Injectable {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    NavigationController navigationController;

    DataBindingComponent dataBindingComponent = new FragmentDataBindingComponent(this);

    AutoClearedValue<SearchFragmentBinding> binding;

    AutoClearedValue<RepoListAdapter> adapter;

    private SearchViewModel searchViewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        SearchFragmentBinding dataBinding = DataBindingUtil
                .inflate(inflater, R.layout.search_fragment, container, false,
                        dataBindingComponent);
        binding = new AutoClearedValue<>(this, dataBinding);

        return dataBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Search view model
        searchViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SearchViewModel.class);

        initRecyclerView();

        // Repositories adapter
        RepoListAdapter rvAdapter = new RepoListAdapter(dataBindingComponent, true,
                repo -> navigationController.navigateToRepo(repo.owner.login, repo.name));
        binding.get().repoList.setAdapter(rvAdapter);
        adapter = new AutoClearedValue<>(this, rvAdapter);

        initSearchInputListener();

        binding.get().setCallback(() -> searchViewModel.refresh());
    }

    /**
     * Initialize search input listener.
     */
    private void initSearchInputListener() {
        // set editor action listener
        binding.get().edtInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(v);
                return true;
            }
            return false;
        });

        // set on key listener
        binding.get().edtInput.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN)
                    && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                doSearch(v);
                return true;
            }
            return false;
        });
    }

    /**
     * Search Repositories.
     *
     * @param v The {@link View}
     */
    private void doSearch(View v) {
        // Get query string
        String query = binding.get().edtInput.getText().toString();

        // Dismiss keyboard
        ViewUtils.dismissKeyboard(getActivity(), v.getWindowToken());

        binding.get().setQuery(query);
        searchViewModel.setQuery(query);
    }

    /**
     * Initialize Recycler View.
     */
    private void initRecyclerView() {

        // Add scroll listener
        binding.get().repoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                int lastPosition = layoutManager.findLastVisibleItemPosition();
                if (lastPosition == adapter.get().getItemCount() - 1) {
                    searchViewModel.loadNextPage();
                }
            }
        });

        // SearchViewModel observe
        searchViewModel.getResults().observe(this, result -> {
            Timber.d("update ui");

            binding.get().setSearchResource(result);
            binding.get().setResultCount((result == null || result.data == null)
                    ? 0 : result.data.size());
            adapter.get().replace(result == null ? null : result.data);
            binding.get().executePendingBindings();
        });

        // Load more
        searchViewModel.getLoadMoreStatus().observe(this, loadingMore -> {
            if (loadingMore == null) {
                binding.get().setLoadingMore(false);
            } else {
                binding.get().setLoadingMore(loadingMore.isRunning());
                String error = loadingMore.getErrorMessageIfNotHandled();
                if (error != null) {
                    Snackbar.make(binding.get().loadMoreBar, error, Snackbar.LENGTH_LONG).show();
                }
            }

            binding.get().executePendingBindings();
        });
    }
}