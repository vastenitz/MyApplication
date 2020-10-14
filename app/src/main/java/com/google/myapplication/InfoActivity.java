package com.google.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class InfoActivity extends AppCompatActivity {
    private TextView tvName, tvBirthday, tvClass, tvSchool, tvPhoneNumber, tvEmail, tvLocation;
    private DatabaseReference mDatabaseReference;
    private String formatDate;
    private ImageView imgProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Date dateObj = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
        formatDate = df.format(dateObj);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("RFID");

        Intent infoIntent = getIntent();
        String cardId = infoIntent.getStringExtra("cardID");


        tvName = findViewById(R.id.tv_name);
        tvBirthday = findViewById(R.id.tv_birthday);
        tvSchool = findViewById(R.id.tv_school);
        tvClass = findViewById(R.id.tv_class);
        tvPhoneNumber = findViewById(R.id.tv_phone_number);
        tvEmail = findViewById(R.id.tv_email);
        tvLocation = findViewById(R.id.tv_location);
        imgProfile = findViewById(R.id.img_profile);

        mDatabaseReference.child(cardId).child("student").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserInfo mUser = dataSnapshot.getValue(UserInfo.class);
                if (mUser == null) {
                    return;
                }

                tvName.setText(mUser.getName());
                tvBirthday.setText(mUser.getBirthday());
                tvClass.setText(mUser.getClassroom());
                tvSchool.setText(mUser.getSchool());
                tvPhoneNumber.setText(mUser.getPhoneNumber());
                tvEmail.setText(mUser.getEmail());
                tvLocation.setText(mUser.getLocation());
                Glide.with(InfoActivity.this)
                        .load(mUser.getAvatar())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(imgProfile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(InfoActivity.this, "Unable to load data", Toast.LENGTH_LONG).show();
            }
        });
    }
}
