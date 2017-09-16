package com.duyp.architecture.mvp.ui.user_repositories;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.duyp.architecture.mvp.base.fragment.BaseSwipeRecyclerViewFragment;
import com.duyp.architecture.mvp.data.model.User;
import com.duyp.architecture.mvp.ui.repositories.RepositoryAdapter;
import com.duyp.architecture.mvp.ui.repositories.RepositoryView;
import com.duyp.architecture.mvp.utils.NavigatorHelper;

import org.parceler.Parcels;

/**
 * Created by duypham on 9/16/17.
 * This Fragment shows list of current user repositories
 */

public class UserRepositoryFragment extends BaseSwipeRecyclerViewFragment<RepositoryAdapter, RepositoryView, UserRepositoryPresenter>
        implements RepositoryView {

    public static UserRepositoryFragment createInstance(@Nullable User user) {
        return NavigatorHelper.createFragmentWithArguments(new UserRepositoryFragment(), bundle -> {
            bundle.putParcelable("user", Parcels.wrap(user));
        });
    }

    @Override
    protected void initialize(View view) {

        Bundle bundle = getArguments();
        ensureInUserScope(userFragmentComponent -> {
            userFragmentComponent.inject(this);
            getPresenter().initTargetUser(null);
        }, () -> {
            fragmentComponent().inject(this);
            getPresenter().initTargetUser(Parcels.unwrap(bundle.getParcelable("user")));
        });

        super.initialize(view);
        refresh();
    }

    @NonNull
    @Override
    protected RepositoryAdapter createAdapter() {
        return getPresenter().getAdapter();
    }
}