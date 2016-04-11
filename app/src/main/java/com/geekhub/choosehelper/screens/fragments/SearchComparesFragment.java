package com.geekhub.choosehelper.screens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.geekhub.choosehelper.R;
import com.geekhub.choosehelper.models.db.Compare;
import com.geekhub.choosehelper.models.network.NetworkCompare;
import com.geekhub.choosehelper.models.network.NetworkLike;
import com.geekhub.choosehelper.models.network.NetworkUser;
import com.geekhub.choosehelper.models.network.NetworkVariant;
import com.geekhub.choosehelper.screens.activities.DetailsActivity;
import com.geekhub.choosehelper.screens.activities.ProfileActivity;
import com.geekhub.choosehelper.ui.adapters.ComparesAdapter;
import com.geekhub.choosehelper.utils.ModelConverter;
import com.geekhub.choosehelper.utils.Prefs;
import com.geekhub.choosehelper.utils.Utils;
import com.geekhub.choosehelper.utils.firebase.FirebaseConstants;
import com.geekhub.choosehelper.utils.firebase.FirebaseLikesManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class SearchComparesFragment extends BaseFragment {

    @Bind(R.id.recycler_view_search_fragment)
    RecyclerView mRecyclerView;

    // firebase references and queries
    private Firebase mFirebaseCompares;
    private Firebase mFirebaseLikes;

    public SearchComparesFragment() {
    }

    public static SearchComparesFragment newInstance() {
        return new SearchComparesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // firebase references
        mFirebaseCompares = new Firebase(FirebaseConstants.FB_REF_MAIN)
                .child(FirebaseConstants.FB_REF_COMPARES);

        mFirebaseLikes = new Firebase(FirebaseConstants.FB_REF_MAIN)
                .child(FirebaseConstants.FB_REF_LIKES);
    }

    // get information about compares from firebase
    public void searchCompares(String query) {
        mFirebaseCompares.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Compare> compares = new ArrayList<>();
                int snapshotSize = 0;
                for (DataSnapshot compareSnapshot : dataSnapshot.getChildren()) {
                    NetworkCompare networkCompare = compareSnapshot.getValue(NetworkCompare.class);
                    List<NetworkVariant> networkVariants = networkCompare.getVariants();
                    if (networkVariants.get(0).getDescription().contains(query) ||
                            networkVariants.get(1).getDescription().contains(query)) {
                        snapshotSize++;
                        fetchDetailsFromNetwork(compares, networkCompare, compareSnapshot.getKey(),
                                snapshotSize);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //hideRefreshing();
                Utils.showMessage(getContext(), getString(R.string.toast_error_message));
            }
        });
    }

    // get details information about compares from firebase
    private void fetchDetailsFromNetwork(List<Compare> compares, NetworkCompare networkCompare,
                                         String compareId, int size) {
        // liked variant
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
                // compare author
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
                                    updateUi(compares);
                                    //hideRefreshing();
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                //hideRefreshing();
                                Utils.showMessage(getContext(),
                                        getString(R.string.toast_error_message));
                            }
                        });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Utils.showMessage(getContext(), getString(R.string.toast_error_message));
            }
        });
    }

    // update UI method
    private void updateUi(List<Compare> compares) {
        //setProgressVisibility(false);

        ComparesAdapter adapter;
        if (mRecyclerView.getAdapter() == null) {
            adapter = new ComparesAdapter(compares.subList(0, compares.size() < 19
                    ? compares.size() : 19));
            mRecyclerView.setAdapter(adapter);

            // click listener for details
            adapter.setOnItemClickListener((view, position) -> {
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.INTENT_KEY_COMPARE_ID,
                        compares.get(position).getId());
                startActivity(intent);
            });

            // click listener for likes
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

            // click listener for popup menu
            adapter.setOnItemClickListenerPopup((view, position) -> {
                String compareId = compares.get(position).getId();
                if (compares.get(position).getAuthor().getId().equals(Prefs.getUserId())) {
                    Utils.showOwnerPopupMenu(getContext(), view, compareId);
                } else {
                    Utils.showUserPopupMenu(getContext(), view, compareId);
                }
            });

            // click listener for author
            adapter.setOnItemClickListenerAuthor((view, position) -> {
                Intent userIntent = new Intent(getActivity(), ProfileActivity.class);
                userIntent.putExtra(ProfileActivity.INTENT_KEY_USER_ID,
                        compares.get(position).getAuthor().getId());
                userIntent.putExtra(ProfileActivity.INTENT_KEY_USER_NAME,
                        compares.get(position).getAuthor().getFullName());
                startActivity(userIntent);
            });
        } else {
            adapter = (ComparesAdapter) mRecyclerView.getAdapter();
            adapter.updateList(compares.subList(0, compares.size() < 19
                    ? compares.size() : 19));
            adapter.notifyDataSetChanged();
        }
    }

}