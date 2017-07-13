package com.android.example.architecture.ui.activity.repo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.VisibleForTesting;

import com.android.example.architecture.db.entity.Contributor;
import com.android.example.architecture.db.entity.Repo;
import com.android.example.architecture.repository.RepoRepository;
import com.android.example.architecture.util.common.AbsentLiveData;
import com.android.example.architecture.util.common.Objects;
import com.android.example.architecture.util.common.Resource;

import java.util.List;

import javax.inject.Inject;

public class RepoViewModel extends ViewModel {

    @VisibleForTesting
    final MutableLiveData<RepoId> repoId;
    private final LiveData<Resource<Repo>> repo;
    private final LiveData<Resource<List<Contributor>>> contributors;

    @Inject
    public RepoViewModel(RepoRepository repository) {
        this.repoId = new MutableLiveData<>();

        // Load repositories
        repo = Transformations.switchMap(repoId, input->{
            if (input.isEmpty()) {
                return AbsentLiveData.create();
            }
            return repository.loadRepo(input.owner, input.name);
        });

        // Load Contributors
        contributors = Transformations.switchMap(repoId, input->{
            if (input.isEmpty()) {
                return AbsentLiveData.create();
            }

            return repository.loadContributors(input.owner, input.name);
        });
    }

    /**
     * Get Repository data.
     */
    public LiveData<Resource<Repo>> getRepo() {
        return repo;
    }

    /**
     * Get list Contributors.
     */
    public LiveData<Resource<List<Contributor>>> getContributors() {
        return contributors;
    }

    /**
     * Set current data.
     */
    public void retry() {
        RepoId current = repoId.getValue();
        if (current != null && !current.isEmpty()) {
            repoId.setValue(current);
        }
    }

    /**
     * Set Repository Id.
     *
     * @param owner The {@link String}
     * @param name  The {@link String}
     */
    void setId(String owner, String name) {
        RepoId update = new RepoId(owner, name);
        if (Objects.equals(repoId.getValue(), update)) {
            return;
        }
        repoId.setValue(update);
    }

    @VisibleForTesting
    static class RepoId {
        public final String owner;
        public final String name;

        RepoId(String owner, String name) {
            this.owner = owner == null ? null : owner.trim();
            this.name = name == null ? null : name.trim();
        }

        boolean isEmpty() {
            return owner == null || name == null || owner.length() == 0 || name.length() == 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RepoId repoId = (RepoId) o;

            if (owner != null ? !owner.equals(repoId.owner) : repoId.owner != null) {
                return false;
            }
            return name != null ? name.equals(repoId.name) : repoId.name == null;
        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }
}
