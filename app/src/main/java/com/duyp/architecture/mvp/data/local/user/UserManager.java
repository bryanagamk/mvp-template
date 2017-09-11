package com.duyp.architecture.mvp.data.local.user;

import android.app.NotificationManager;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.duyp.androidutils.functions.PlainAction;
import com.duyp.androidutils.functions.PlainConsumer;
import com.duyp.architecture.mvp.dagger.InjectionHelper;
import com.duyp.architecture.mvp.dagger.component.UserComponent;
import com.duyp.architecture.mvp.dagger.module.UserModule;
import com.duyp.architecture.mvp.dagger.qualifier.ApplicationContext;
import com.duyp.architecture.mvp.data.model.User;
import com.duyp.architecture.mvp.data.remote.GithubService;
import com.duyp.architecture.mvp.utils.Events;
import com.duyp.architecture.mvp.utils.api.ApiUtils;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Duy Pham on 5/19/17.
 * Injected class for managing User by using User Component
 */

@Singleton
public class UserManager {

    // debug tag
    private static final String TAG = "UserManager";

    protected Context mContext;

    // user data store
    protected final UserRepo mUserRepo;

    // user component
    protected UserComponent mUserComponent;

    protected EventBus mEventBus;

    protected GithubService mGithubService;

    @Inject
    public UserManager(@ApplicationContext Context context, UserRepo userRepo, EventBus eventBus, GithubService service) {
        this.mContext = context;
        this.mUserRepo = userRepo;
        this.mEventBus = eventBus;
        this.mGithubService = service;
    }

    protected UserComponent createUserComponent(@NonNull String token) {
        return InjectionHelper.getAppComponent(mContext).getUserComponent(new UserModule(token));
    }

    @Nullable
    public UserComponent getUserComponent() {
        return mUserComponent;
    }

    @NonNull
    public GithubService getGithubService() {
        return mGithubService;
    }

    @NonNull
    public UserRepo getUserRepo() {
        return mUserRepo;
    }

    /**
     * Save user to shared preference and start user session
     * @param user respond user after logging in
     * @param token user auth token
     */
    public void doAfterLogin(@NonNull User user, @NonNull String token, @Nullable PlainConsumer<LiveData<User>> consumer) {
        mUserRepo.setUser(user, consumer);
        mUserRepo.setUserToken(token);
        startUserSession(token);
    }

    /**
     * Start user session in test environment
     * @param token user token
     */
    private void startUserSession(String token) {
        if (mUserComponent != null) {
            stopUserSession();
        }
        mUserComponent = createUserComponent(token);
        Log.d(TAG, "User session started!");
    }

    /**
     * Stop user session (clear user data from both memory and shared pref)
     */
    private void stopUserSession() {
        mUserComponent = null;
        mUserRepo.clearUser();
        Log.d(TAG, "User session stopped!");
    }

    public boolean isUserSessionStarted() {
        return mUserComponent != null;
    }

    /**
     *
     * check if has saved user -> start new session
     * @return true if has saved user
     */
    public boolean checkForSavedUserAndStartSessionIfHas(@Nullable PlainConsumer<LiveData<User>> consumer) {
        boolean pass = true;
        if (mUserComponent == null) {
            if (mUserRepo.isUserExisted()) {
                startUserSession(mUserRepo.getUserToken());
            } else {
                pass = false;
            }
        }
        if (pass && consumer != null) {
            // noinspection ConstantConditions
            consumer.accept(mUserRepo.getUserLiveData());
        }
        return false;
    }

    /**
     * Update user data by new user if are the same
     * @param newUser new User data
     * @return true if user updated
     */
    public boolean updateUserIfEquals(User newUser, PlainConsumer<LiveData<User>> consumer) {
        return mUserRepo.updateUserIfEquals(newUser, consumer);
    }

    /*
    * Refresh current user account from server
    * All errors will be ignored
     */
    public void refreshUser() {
        if (isUserSessionStarted()) {
            Log.d(TAG, "Refreshing user...");
            // noinspection ConstantConditions
            ApiUtils.makeRequest(mUserComponent.getUserService().updateUser(), false, user -> {
                updateUserIfEquals(user, done -> {});
            });
        }
    }

    /**
     * Logout user
     */
    public void logout() {
        reset();

        // send logout broadcast (tutor service will stopSelf)
        mEventBus.post(new Events.LogoutEvent());

        // navigate user to Login Page
    }

    public void reset() {
        logOutApi();

        // stop user session
        stopUserSession();

        // cancel all notification
        NotificationManager notifManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifManager != null) {
            notifManager.cancelAll();
        }

        // clear data (shared preference and realm)
        mUserRepo.clearAll();
    }

    /**
     * Logout by our back-end api
     */
    private void logOutApi() {
        if (mUserComponent != null) {
            // call logout api here
        }
    }
}