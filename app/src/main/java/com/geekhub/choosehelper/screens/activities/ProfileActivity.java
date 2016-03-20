package com.geekhub.choosehelper.screens.activities;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.geekhub.choosehelper.R;
import com.geekhub.choosehelper.models.db.User;
import com.geekhub.choosehelper.utils.Prefs;
import com.geekhub.choosehelper.utils.db.DbUsersManager;

import butterknife.Bind;

public class ProfileActivity extends BaseSignInActivity {

    private static final String TAG = "ProfileActivity";

    @Bind(R.id.toolbar_profile)
    Toolbar mToolbar;

    @Bind(R.id.profile_iv_avatar)
    ImageView mIvUserAvatar;

    @Bind(R.id.profile_tv_email)
    TextView mEmail;

    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mUser = DbUsersManager.getUserNotAsync(Prefs.getUserId());
        Glide.with(getApplicationContext())
                .load(mUser.getPhotoUrl())
                .into(mIvUserAvatar);
        mEmail.setText(mUser.getEmail());
        //AuthorizationUtil.getUserAsync(Prefs.getUserId());
        setupToolbar();
        /*User user = DbUsersManager.getUserAsync(Prefs.getUserId());
        if (user != null && user.isLoaded()) {
            Log.i("MainActivityTest", "setupNavHeader: " + user.getId());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getFullName());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getEmail());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getPhotoUrl());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getBirthday());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getPlaceLive());
            Log.i("MainActivityTest", "setupNavHeader: " + user.getAbout());
        }*/
        /*Glide
                .with(this)
                .load(Prefs.getUserAvatarUrl())
                .bitmapTransform(new CropCircleTransformation(getApplicationContext()))
                .into(mIvUserAvatar);*/
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mUser.getFullName());
            getSupportActionBar().setHomeAsUpIndicator(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.icon_arrow_back));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
