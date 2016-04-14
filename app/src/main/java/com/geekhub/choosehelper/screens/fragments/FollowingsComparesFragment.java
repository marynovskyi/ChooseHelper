package com.geekhub.choosehelper.screens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.geekhub.choosehelper.R;
import com.geekhub.choosehelper.models.db.Compare;
import com.geekhub.choosehelper.models.db.Following;
import com.geekhub.choosehelper.models.db.User;
import com.geekhub.choosehelper.models.network.NetworkCompare;
import com.geekhub.choosehelper.models.network.NetworkLike;
import com.geekhub.choosehelper.models.network.NetworkUser;
import com.geekhub.choosehelper.screens.activities.DetailsActivity;
import com.geekhub.choosehelper.screens.activities.MainActivity;
import com.geekhub.choosehelper.screens.activities.ProfileActivity;
import com.geekhub.choosehelper.ui.adapters.ComparesAdapter1;
import com.geekhub.choosehelper.utils.ModelConverter;
import com.geekhub.choosehelper.utils.Prefs;
import com.geekhub.choosehelper.utils.Utils;
import com.geekhub.choosehelper.utils.db.DbComparesManager;
import com.geekhub.choosehelper.utils.db.DbFields;
import com.geekhub.choosehelper.utils.firebase.FirebaseConstants;
import com.geekhub.choosehelper.utils.firebase.FirebaseLikesManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class FollowingsComparesFragment extends BaseFragment {

    @Bind(R.id.recycler_view_followings_compares)
    RecyclerView mRecyclerView;

    @Bind(R.id.swipe_to_refresh_follwings_compares)
    SwipeRefreshLayout mSwipeRefreshLayout;

    // firebase references and queries
    private Firebase mFirebaseCompares;
    private Firebase mFirebaseLikes;

    // realm
    private User mCurrentUser;

    private RealmResults<Compare> mCompares;

    private RealmChangeListener mComparesListener = () -> {
        if (mCompares != null && mCompares.isLoaded()) {
            mCurrentUser = Realm.getDefaultInstance()
                    .where(User.class)
                    .equalTo(DbFields.DB_ID, Prefs.getUserId())
                    .findFirst();
            List<Following> followings = mCurrentUser.getFollowings();
            List<Compare> compares = new ArrayList<>();
            List<String> followingIds = new ArrayList<>();
            for (Following following : followings) {
                followingIds.add(following.getUserId());
            }
            for (Compare compare : mCompares) {
                if (followingIds.contains(compare.getAuthor().getId())) {
                    compares.add(compare);
                }
            }
            updateUi(compares);
        }
    };

    public FollowingsComparesFragment() {

    }

    public static FollowingsComparesFragment newInstance(/*ArrayList<String> authorIds*/) {
        return new FollowingsComparesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_followings_compares, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // firebase references
        mFirebaseCompares = new Firebase(FirebaseConstants.FB_REF_MAIN)
                .child(FirebaseConstants.FB_REF_COMPARES);

        mFirebaseLikes = new Firebase(FirebaseConstants.FB_REF_MAIN)
                .child(FirebaseConstants.FB_REF_LIKES);

        // requests
        fetchComparesFromDb();
        if (Utils.hasInternet(getContext())) {
            fetchComparesFromNetwork();
        }// TODO show empty view no followings yet

        // swipe refresh layout
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light); // TODO create colors array

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            if (Utils.hasInternet(getContext())) {
                fetchComparesFromNetwork();
            } else {
                Utils.showMessage(getContext(), getString(R.string.toast_no_internet));
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MainActivity.sIsNeedToAutoUpdate) {
            MainActivity.sIsNeedToAutoUpdate = false;
            fetchComparesFromNetwork();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompares != null && mComparesListener != null) {
            mCompares.removeChangeListener(mComparesListener);
        }
    }

    // get information about compare from local database
    private void fetchComparesFromDb() {
        setProgressVisibility(true);
        mCompares = DbComparesManager.getCompares();
        mCompares.addChangeListener(mComparesListener);
    }

    // get information about compare from firebase
    private void fetchComparesFromNetwork() {
        mFirebaseCompares.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Compare> compares = new ArrayList<>();
                int snapshotSize = (int) dataSnapshot.getChildrenCount();
                if (snapshotSize == 0)
                    hideRefreshing();
                for (DataSnapshot compareSnapshot : dataSnapshot.getChildren()) {
                    NetworkCompare networkCompare = compareSnapshot.getValue(NetworkCompare.class);
                    fetchDetailsFromNetwork(compares, networkCompare,
                            compareSnapshot.getKey(), snapshotSize);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                hideRefreshing();
                Utils.showMessage(getContext(), getString(R.string.toast_error_try_later));
            }
        });
    }

    // get details information about compares from firebase
    private void fetchDetailsFromNetwork(List<Compare> compares, NetworkCompare networkCompare,
                                         String compareId, int size) {

        // likes
        Query queryDetails = mFirebaseLikes.orderByChild(FirebaseConstants.FB_REF_COMPARE_ID)
                .equalTo(compareId);
        queryDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot likeSnapshot) {
                int likedVariant = -1;
                for (DataSnapshot snapshot : likeSnapshot.getChildren()) {
                    if (snapshot.getValue(NetworkLike.class).getUserId().equals(Prefs.getUserId())) {
                        likedVariant = snapshot.getValue(NetworkLike.class).getVariantNumber();
                    }
                }

                // compares author
                final int tempLikedVariant = likedVariant;
                new Firebase(FirebaseConstants.FB_REF_MAIN)
                        .child(FirebaseConstants.FB_REF_USERS)
                        .child(networkCompare.getUserId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot authorSnapshot) {
                                compares.add(ModelConverter.convertToCompare(networkCompare,
                                        compareId,
                                        authorSnapshot.getValue(NetworkUser.class),
                                        networkCompare.getUserId(),
                                        tempLikedVariant));

                                if (compares.size() == size) {
                                    DbComparesManager.saveCompares(compares);
                                    hideRefreshing();
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                hideRefreshing();
                                Utils.showMessage(getContext(),
                                        getString(R.string.toast_error_try_later));
                            }
                        });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Utils.showMessage(getContext(), getString(R.string.toast_error_try_later));
            }
        });
    }

    // update UI method
    private void updateUi(List<Compare> compares) {
        setProgressVisibility(false);

        ComparesAdapter1 adapter;
        if (mRecyclerView.getAdapter() == null) {
            adapter = new ComparesAdapter1(compares);
            mRecyclerView.setAdapter(adapter);

            /** click listener for details **/
            adapter.setOnItemClickListener((view, position) -> {
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.INTENT_KEY_COMPARE_ID, compares.get(position).getId());
                startActivity(intent);
            });

            /** click listener for likes **/
            adapter.setOnLikeClickListener((mainView, clickedCheckBox, otherCheckBox,
                                            position, variantNumber) -> {
                boolean isNeedToUnCheck = false;
                if (!compares.get(position).isOpen()) { // if closed
                    isNeedToUnCheck = true;
                    Utils.showMessage(getContext(), getString(R.string.toast_cannot_like_closed));
                } else if (!Utils.hasInternet(getContext())) { // if no internet
                    isNeedToUnCheck = true;
                    Utils.showMessage(getContext(), getString(R.string.toast_no_internet));
                } else if (compares.get(position).getAuthor().getId().equals(Prefs.getUserId())) { // if user is owner
                    isNeedToUnCheck = true;
                    Utils.showMessage(getContext(), getString(R.string.toast_cannot_like_own));
                } else { // update like
                    Utils.blockViews(mainView, clickedCheckBox, otherCheckBox);
                    FirebaseLikesManager.updateLike(compares.get(position).getId(), variantNumber,
                            mainView, clickedCheckBox, otherCheckBox);
                }
                // unCheck if need
                if (isNeedToUnCheck) {
                    clickedCheckBox.setChecked(false);
                    int newValue = Integer.parseInt(clickedCheckBox.getText().toString()) - 1;
                    clickedCheckBox.setText(String.valueOf(newValue));
                }
            });

            /** click listener for popup menu **/
            adapter.setOnItemClickListenerPopup((view, position) -> {
                String compareId = compares.get(position).getId();
                if (compares.get(position).getAuthor().getId().equals(Prefs.getUserId())) {
                    Utils.showOwnerPopupMenu(getContext(), view, compareId);
                } else {
                    Utils.showUserPopupMenu(getContext(), view, compareId);
                }
            });

            /** click listener for author **/
            adapter.setOnItemClickListenerAuthor((view, position) -> {
                Intent userIntent = new Intent(getActivity(), ProfileActivity.class);
                userIntent.putExtra(ProfileActivity.INTENT_KEY_USER_ID,
                        compares.get(position).getAuthor().getId());
                userIntent.putExtra(ProfileActivity.INTENT_KEY_USER_NAME,
                        compares.get(position).getAuthor().getFullName());
                startActivity(userIntent);
            });
        } else {
            adapter = (ComparesAdapter1) mRecyclerView.getAdapter();
            adapter.updateList(compares);
            adapter.notifyDataSetChanged();
        }
    }

    // methods for show progress
    private void hideRefreshing() {
        if (mSwipeRefreshLayout != null && mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void setProgressVisibility(boolean visible) {
        //mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
