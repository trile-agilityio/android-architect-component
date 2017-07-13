package com.android.example.architecture.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.example.architecture.service.networking.api.IGithubApi;
import com.android.example.architecture.util.common.AppExecutors;
import com.android.example.architecture.db.dao.RepoDao;
import com.android.example.architecture.db.database.GithubDb;
import com.android.example.architecture.db.entity.Contributor;
import com.android.example.architecture.db.entity.Repo;
import com.android.example.architecture.db.entity.RepoSearchResult;
import com.android.example.architecture.service.networking.base.ApiResponse;
import com.android.example.architecture.service.networking.response.RepoSearchResponse;
import com.android.example.architecture.util.common.AbsentLiveData;
import com.android.example.architecture.util.common.RateLimiter;
import com.android.example.architecture.util.common.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class RepoRepository {

    private final GithubDb db;

    private final RepoDao repoDao;

    private final IGithubApi githubService;

    private final AppExecutors appExecutors;

    private RateLimiter<String> repoListRateLimit = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public RepoRepository(AppExecutors appExecutors, GithubDb db, RepoDao repoDao,
                          IGithubApi githubService) {
        this.db = db;
        this.repoDao = repoDao;
        this.githubService = githubService;
        this.appExecutors = appExecutors;
    }

    /**
     * Load list Repositories.
     *
     * @param owner {@link String}
     * @return {@link List<Repo>}
     */
    public LiveData<Resource<List<Repo>>> loadRepos(String owner) {
        return new NetworkBoundResource<List<Repo>, List<Repo>>(appExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Repo> item) {
                Timber.d("save list Repos");
                repoDao.insertRepos(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Repo> data) {
                final boolean result = data == null || data.isEmpty()
                        || repoListRateLimit.shouldFetch(owner);

                Timber.d("should fetch Repos data : " + result);
                return result;
            }

            @NonNull
            @Override
            protected LiveData<List<Repo>> loadFromDb() {
                Timber.d("load list repos from DB");
                return repoDao.loadRepositories(owner);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Repo>>> createCall() {
                Timber.d("load list Repos from api");
                return githubService.getRepos(owner);
            }

            @Override
            protected void onFetchFailed() {
                repoListRateLimit.reset(owner);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Repo>> loadRepo(String owner, String name) {
        return new NetworkBoundResource<Repo, Repo>(appExecutors) {
            @Override
            protected void saveCallResult(@NonNull Repo item) {
                Timber.d("save Repo item");
                repoDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable Repo data) {
                final boolean result = data == null;

                Timber.d("should fetch Repo data : " + result);
                return result;
            }

            @NonNull
            @Override
            protected LiveData<Repo> loadFromDb() {
                Timber.d("load Repo from local");
                return repoDao.load(owner, name);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<Repo>> createCall() {
                Timber.d("load Repo data from api");
                return githubService.getRepo(owner, name);
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<Contributor>>> loadContributors(String owner, String name) {
        return new NetworkBoundResource<List<Contributor>, List<Contributor>>(appExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Contributor> contributors) {
                for (Contributor contributor : contributors) {
                    contributor.setRepoName(name);
                    contributor.setRepoOwner(owner);
                }

                db.beginTransaction();
                try {
                    repoDao.createRepoIfNotExists(new Repo(Repo.UNKNOWN_ID,
                            name, owner + "/" + name, "",
                            new Repo.Owner(owner, null), 0));
                    repoDao.insertContributors(contributors);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                Timber.d("received saved contributors to db");
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Contributor> data) {
                Timber.d("received contributor list from db: %s", data);
                final boolean result = data == null || data.isEmpty();

                Timber.d("should fetch Repo data : " + result);
                return result;
            }

            @NonNull
            @Override
            protected LiveData<List<Contributor>> loadFromDb() {
                Timber.d("load contributors from local");
                return repoDao.loadContributors(owner, name);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Contributor>>> createCall() {
                Timber.d("load contributors from server");
                return githubService.getContributors(owner, name);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Boolean>> searchNextPage(String query) {
        FetchNextSearchPageTask fetchNextSearchPageTask = new FetchNextSearchPageTask(
                query, githubService, db);
        appExecutors.networkIO().execute(fetchNextSearchPageTask);
        return fetchNextSearchPageTask.getLiveData();
    }

    public LiveData<Resource<List<Repo>>> search(String query) {
        return new NetworkBoundResource<List<Repo>, RepoSearchResponse>(appExecutors) {

            @Override
            protected void saveCallResult(@NonNull RepoSearchResponse item) {
                Timber.d("saveCallResult");

                List<Integer> repoIds = item.getRepoIds();
                RepoSearchResult repoSearchResult = new RepoSearchResult(
                        query, repoIds, item.getTotal(), item.getNextPage());
                db.beginTransaction();
                try {
                    repoDao.insertRepos(item.getItems());
                    repoDao.insert(repoSearchResult);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Repo> data) {
                Timber.d("shouldFetch :", String.valueOf(data == null));
                return data == null;
            }

            @NonNull
            @Override
            protected LiveData<List<Repo>> loadFromDb() {
                Timber.d("loadFromDb");

                return Transformations.switchMap(repoDao.search(query), searchData->{
                    if (searchData == null) {
                        return AbsentLiveData.create();
                    } else {
                        return repoDao.loadOrdered(searchData.repoIds);
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RepoSearchResponse>> createCall() {
                Timber.d("createCall");

                return githubService.searchRepos(query);
            }

            @Override
            protected RepoSearchResponse processResponse(ApiResponse<RepoSearchResponse> response) {
                Timber.d("processResponse");

                RepoSearchResponse body = response.body;
                if (body != null) {
                    body.setNextPage(response.getNextPage());
                }
                return body;
            }
        }.asLiveData();
    }
}