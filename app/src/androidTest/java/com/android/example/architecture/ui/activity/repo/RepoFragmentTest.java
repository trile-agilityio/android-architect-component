package com.android.example.architecture.ui.activity.repo;

import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.example.architecture.R;
import com.android.example.architecture.binding.FragmentBindingAdapters;
import com.android.example.architecture.db.entity.Contributor;
import com.android.example.architecture.db.entity.Repo;
import com.android.example.architecture.testing.SingleFragmentActivity;
import com.android.example.architecture.ui.common.NavigationController;
import com.android.example.architecture.util.RecyclerViewMatcher;
import com.android.example.architecture.util.TaskExecutorWithIdlingResourceRule;
import com.android.example.architecture.util.TestUtil;
import com.android.example.architecture.util.ViewModelUtil;
import com.android.example.architecture.util.common.Resource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class RepoFragmentTest {
    @Rule
    public ActivityTestRule<SingleFragmentActivity> activityRule =
            new ActivityTestRule<>(SingleFragmentActivity.class, true, true);
    @Rule
    public TaskExecutorWithIdlingResourceRule executorRule =
            new TaskExecutorWithIdlingResourceRule();
    private MutableLiveData<Resource<Repo>> repo = new MutableLiveData<>();
    private MutableLiveData<Resource<List<Contributor>>> contributors = new MutableLiveData<>();
    private RepoFragment repoFragment;
    private RepoViewModel viewModel;
    private FragmentBindingAdapters fragmentBindingAdapters;
    private NavigationController navigationController;


    @Before
    public void init() {
        repoFragment = RepoFragment.create("a", "b");
        viewModel = mock(RepoViewModel.class);
        fragmentBindingAdapters = mock(FragmentBindingAdapters.class);
        navigationController = mock(NavigationController.class);

        when(viewModel.getRepo()).thenReturn(repo);
        when(viewModel.getContributors()).thenReturn(contributors);

        repoFragment.viewModelFactory = ViewModelUtil.createFor(viewModel);
//        repoFragment.dataBindingComponent = () -> fragmentBindingAdapters;
        repoFragment.navigationController = navigationController;

        activityRule.getActivity().setFragment(repoFragment);
    }

    @Test
    public void testLoading() {
        repo.postValue(Resource.loading(null));
        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_retry)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testValueWhileLoading() {
        Repo repo = TestUtil.createRepo("yigit", "foo", "foo-bar");
        this.repo.postValue(Resource.loading(repo));
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.txt_name)).check(matches(
                withText(getString(R.string.repo_full_name, "yigit", "foo"))));
        onView(withId(R.id.txt_description)).check(matches(withText("foo-bar")));
    }

    @Test
    public void testLoaded() throws InterruptedException {
        Repo repo = TestUtil.createRepo("foo", "bar", "buzz");
        this.repo.postValue(Resource.success(repo));
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.txt_name)).check(matches(
                withText(getString(R.string.repo_full_name, "foo", "bar"))));
        onView(withId(R.id.txt_description)).check(matches(withText("buzz")));
    }

    @Test
    public void testError() throws InterruptedException {
        repo.postValue(Resource.error("foo", null));
        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btn_retry)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_retry)).perform(click());
        verify(viewModel).retry();
        repo.postValue(Resource.loading(null));

        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_retry)).check(matches(not(isDisplayed())));
        Repo repo = TestUtil.createRepo("owner", "name", "desc");
        this.repo.postValue(Resource.success(repo));

        onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btn_retry)).check(matches(not(isDisplayed())));
        onView(withId(R.id.txt_name)).check(matches(
                withText(getString(R.string.repo_full_name, "owner", "name"))));
        onView(withId(R.id.txt_description)).check(matches(withText("desc")));
    }

    @Test
    public void testContributors() {
        setContributors("aa", "bb");
        onView(listMatcher().atPosition(0))
                .check(matches(hasDescendant(withText("aa"))));
        onView(listMatcher().atPosition(1))
                .check(matches(hasDescendant(withText("bb"))));
    }

    @NonNull
    private RecyclerViewMatcher listMatcher() {
        return new RecyclerViewMatcher(R.id.contributor_list);
    }

    @Test
    public void testContributorClick() {
        setContributors("aa", "bb", "cc");
        onView(withText("cc")).perform(click());
        verify(navigationController).navigateToUser("cc");
    }

    @Test
    public void nullRepo() {
        this.repo.postValue(null);
        onView(withId(R.id.txt_name)).check(matches(not(isDisplayed())));
    }

    @Test
    public void nullContributors() {
        setContributors("a", "b", "c");
        onView(listMatcher().atPosition(0)).check(matches(hasDescendant(withText("a"))));
        contributors.postValue(null);
        onView(listMatcher().atPosition(0)).check(doesNotExist());
    }

    private void setContributors(String... names) {
        Repo repo = TestUtil.createRepo("foo", "bar", "desc");
        List<Contributor> contributors = new ArrayList<>();
        int contributionCount = 100;
        for (String name : names) {
            contributors.add(TestUtil.createContributor(repo, name, contributionCount--));
        }
        this.contributors.postValue(Resource.success(contributors));
    }

    private String getString(@StringRes int id, Object... args) {
        return InstrumentationRegistry.getTargetContext().getString(id, args);
    }
}