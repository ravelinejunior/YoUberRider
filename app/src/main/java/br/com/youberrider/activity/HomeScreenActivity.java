package br.com.youberrider.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import br.com.youberrider.R;
import br.com.youberrider.model.Rider;
import br.com.youberrider.utils.Common;
import br.com.youberrider.utils.UserUtils;

public class HomeScreenActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 7071 ;

    //navigation
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavController navController;
    private NavigationView navigationView;

    //ui
    private AlertDialog alertDialog;
    private ImageView imageProfile;

    //firebase
    private StorageReference storageRef;
    private FirebaseAuth auth;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_screen, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void init() {

        //photo
        alertDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Waiting...")
                .create();

        storageRef = FirebaseStorage.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        //set data for user
        View headerView = navigationView.getHeaderView(0);
        TextView txt_name = headerView.findViewById(R.id.text_header_name_id_drawer);
        TextView txt_phone = headerView.findViewById(R.id.text_phone_header_id_drawer);
        imageProfile = headerView.findViewById(R.id.profile_image_header);

        txt_name.setText(Common.buildWelcomeMessage());
        txt_phone.setText(Common.currentRider != null ? Common.currentRider.getPhoneNumber() : "");

        if (Common.currentRider != null && Common.currentRider.getUrlProfileImage() != null
                && !TextUtils.isEmpty(Common.currentRider.getUrlProfileImage())) {
            Glide.with(this)
                    .load(Common.currentRider.getUrlProfileImage())
                    .placeholder(R.drawable.fui_ic_github_white_24dp)
                    .into(imageProfile);
        }

        //signout set
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_signout) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeScreenActivity.this);
                builder.setTitle("Sign Out")
                        .setMessage("Você realmente deseja realmente sair?")
                        .setNegativeButton("Não", (dialogInterface, i) ->
                                dialogInterface.dismiss())
                        .setPositiveButton("Sim", (dialogInterface, i) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(HomeScreenActivity.this, SplashScreen.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialogInterface -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(ContextCompat.getColor(this,android.R.color.holo_red_dark));

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
                });
                dialog.show();
            }
            return false;
        });

        //change image photo
        imageProfile.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);

        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data.getData();
            imageProfile.setImageURI(imageUri);
            showDialogUp();
        }
    }

    private void showDialogUp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeScreenActivity.this);

        Rider rider = new Rider();
        rider.setFirstName(Common.currentRider.getFirstName());
        rider.setLastName(Common.currentRider.getLastName());
        rider.setPhoneNumber(Common.currentRider.getPhoneNumber());

        builder.setTitle("Photo")
                .setMessage("Deseja alterar imagem de perfil?")
                .setNegativeButton("Não", (dialogInterface, i) ->
                        dialogInterface.dismiss())
                .setPositiveButton("Sim", (dialogInterface, i) -> {
                    if (imageUri != null) {
                        alertDialog.setMessage("Uploading...");
                        alertDialog.show();

                        String uid_name = auth.getCurrentUser().getUid();
                        StorageReference imageFolder = storageRef.child("Profile_Images_Riders/" + uid_name);

                        imageFolder.putFile(imageUri)
                                .addOnFailureListener(e -> {
                                    alertDialog.dismiss();
                                    Snackbar.make(drawer,e.getMessage(),Snackbar.LENGTH_SHORT).
                                            setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show();
                                })
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                            Map<String,Object> updateData = new HashMap();
                                            updateData.put("profileImage",uri.toString());
                                            rider.setUrlProfileImage(uri.toString());
                                            Common.currentRider = rider;
                                            UserUtils.updateUser(drawer,updateData);
                                        });
                                    }
                                    alertDialog.dismiss();
                                }).addOnProgressListener(snapshot -> {
                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            alertDialog.setMessage(
                                    new StringBuilder("Uploading: ")
                                            .append(progress)
                                            .append("%")
                            );
                        });

                    }

                });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(R.color.colorAccent));
        });
        dialog.show();
    }
}