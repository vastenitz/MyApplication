package com.google.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.IDNA;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.threeten.bp.LocalDate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference myRef, myStudentRef;
    private FirebaseDatabase mFirebase;
    private Button btnUpdateData, btnInfo;
    private CardAutoCompleteTextView inputCardID;
    private MaterialCalendarView cldView;
    private RecyclerView rcvCheckInOut, rcvWater;
    private CardAdapter mCheckInOutAdapter, mWaterAdapter;
    private Query query, studentQuery;
    private ArrayList<CardDate> arrAllCardTime = new ArrayList<>();
    private ArrayList<String> arrChooseCardTime = new ArrayList<>();
    private TextView tvNotFound, tvNotInputCardID, tvCardNotExist, tvNumberOfCup;
    private SharedPreferences mSharedPreferences;
    private ArrayList<String> arrSuggestCardID = new ArrayList<>();
    private ArrayAdapter suggestAdapter;
    private HashSet<CalendarDay> mCldDays = new HashSet<>();
    private RadioGroup optionRdg;
    private TextView tvName;
    private LinearLayout lnlNumberOfCup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("RFIDPreference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        String json = mSharedPreferences.getString("suggest", "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            arrSuggestCardID = gson.fromJson(json, type);
        }

        // Write a message to the database
        mFirebase = FirebaseDatabase.getInstance();
        rcvCheckInOut = findViewById(R.id.rcv_check_in_out);
        rcvWater = findViewById(R.id.rcv_water);
        mCheckInOutAdapter = new CardAdapter(arrChooseCardTime, 0);
        mWaterAdapter = new CardAdapter(arrChooseCardTime, 1);
        tvNotFound = findViewById(R.id.tv_not_found);
        tvNotInputCardID = findViewById(R.id.tv_not_input_card);
        tvCardNotExist = findViewById(R.id.tv_card_not_exist);
        optionRdg = findViewById(R.id.option_rg);
        tvNumberOfCup = findViewById(R.id.tv_number_of_cups);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(RecyclerView.VERTICAL);

        rcvCheckInOut.setLayoutManager(mLayoutManager);
        rcvCheckInOut.setAdapter(mCheckInOutAdapter);

        LinearLayoutManager mWaterLayoutManager = new LinearLayoutManager(this);
        mWaterLayoutManager.setOrientation(RecyclerView.VERTICAL);

        rcvWater.setLayoutManager(mWaterLayoutManager);
        rcvWater.setAdapter(mWaterAdapter);

        tvNotFound.setVisibility(GONE);
        rcvCheckInOut.setVisibility(GONE);
        rcvWater.setVisibility(GONE);
        tvNotInputCardID.setVisibility(GONE);
        tvCardNotExist.setVisibility(GONE);

        //myRef.setValue();
        btnUpdateData = findViewById(R.id.btn_get_data);
        btnInfo = findViewById(R.id.btn_get_info);
        inputCardID = findViewById(R.id.input_card);
        cldView = findViewById(R.id.card_calendar);

        lnlNumberOfCup = findViewById(R.id.lnl_number_of_cup);

        tvName = findViewById(R.id.tv_student_name);
        tvName.setText("");

        cldView.setSelectedDate(CalendarDay.today());

        if (arrSuggestCardID != null && arrSuggestCardID.size() > 0) {
            inputCardID.setText(arrSuggestCardID.get(arrSuggestCardID.size() - 1));
        }

        suggestAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrSuggestCardID);
        inputCardID.setThreshold(0);
        inputCardID.setAdapter(suggestAdapter);

        inputCardID.setImeOptions(EditorInfo.IME_ACTION_DONE);

        ((RadioButton) optionRdg.getChildAt(0)).setChecked(true);

        getCurrentData(0);
        getStudentName();
        cldView.setSelectedDate(CalendarDay.today());

        btnUpdateData.setOnClickListener(view -> {
            cldView.setSelectedDate(CalendarDay.today());
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            getStudentName();
            getCurrentData(getCurrentSelectedRadioButtonIndex());
        });

        btnInfo.setOnClickListener(view -> {
            Intent mIntent = new Intent(MainActivity.this, InfoActivity.class);
            mIntent.putExtra("cardID", inputCardID.getText().toString());
            startActivity(mIntent);
        });

        cldView.setOnDateChangedListener((widget, date, selected) -> updateList(CommonUtils.getDateFromCalendar(date)));

        optionRdg.setOnCheckedChangeListener((group, checkedId) -> {
            getCurrentData(getCurrentSelectedRadioButtonIndex());
        });
    }

    private void getStudentName() {
        if (!inputCardID.getText().toString().equals("")) {
            myStudentRef = mFirebase.getReference("students");
            studentQuery = myStudentRef.child(inputCardID.getText().toString());
            studentQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                    if (dataSnapshot.getChildrenCount() == 0) {
                        tvName.setText("Card doesn't match to any student");
                    } else {
                        Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
                        while(iterator.hasNext()) {
                            DataSnapshot next = iterator.next();
                            if (next.getKey().equals("name")) {
                                tvName.setText("Học sinh: " + next.getValue().toString());
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            tvName.setText("");
        }
    }

    private void getCurrentData(int index) {
        if (!inputCardID.getText().toString().equals("")) {
            String referenceString = "RFID";
            myRef = mFirebase.getReference(referenceString);
            query = myRef.child(inputCardID.getText().toString()).orderByValue();
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                    if (dataSnapshot.getChildrenCount() == 0) {
                        tvNotFound.setVisibility(GONE);
                        rcvCheckInOut.setVisibility(GONE);
                        rcvWater.setVisibility(GONE);
                        tvNotInputCardID.setVisibility(GONE);
                        tvCardNotExist.setVisibility(View.VISIBLE);
                        lnlNumberOfCup.setVisibility(GONE);
                    } else {
                        Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
                        arrAllCardTime.clear();
                        if (!arrSuggestCardID.contains(inputCardID.getText().toString())) {
                            arrSuggestCardID.add(inputCardID.getText().toString());
                            suggestAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, arrSuggestCardID);
                            inputCardID.setAdapter(suggestAdapter);
                            Gson gson = new Gson();
                            String json = gson.toJson(arrSuggestCardID);
                            SharedPreferences.Editor editor = mSharedPreferences.edit();
                            editor.putString("suggest", json);
                            editor.commit();
                        }
                        mCldDays.clear();

                        while (iterator.hasNext()) {
                            DataSnapshot next = iterator.next();
                            String cardDate = CommonUtils.getDate((Long) next.getValue());
                            String cardTime = CommonUtils.getTime((Long) next.getValue());
                            mCldDays.add(CalendarDay.from(Integer.parseInt(cardDate.substring(4, 8)), Integer.parseInt(cardDate.substring(2, 4)),
                                    Integer.parseInt(cardDate.substring(0, 2))));
                            arrAllCardTime.add(new CardDate(cardDate, cardTime));
                        }
                        cldView.removeDecorators();
                        cldView.addDecorator(new EventDecorator(MainActivity.this, mCldDays));
                        updateList(CommonUtils.getDateFromCalendar(cldView.getSelectedDate()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        } else {
            tvNotFound.setVisibility(GONE);
            rcvCheckInOut.setVisibility(GONE);
            tvNotInputCardID.setVisibility(View.VISIBLE);
            tvCardNotExist.setVisibility(GONE);
            rcvWater.setVisibility(GONE);
            lnlNumberOfCup.setVisibility(GONE);
        }
    }

    void updateList(String daySelected) {
        arrChooseCardTime.clear();
        boolean isFound = false;
        int currentSelectedIndex = getCurrentSelectedRadioButtonIndex();
        for (int i = 0; i < arrAllCardTime.size(); i++) {
            if (arrAllCardTime.get(i).getCardDay().equals(daySelected)) {
                isFound = true;
                arrChooseCardTime.add(arrAllCardTime.get(i).getCardTime());
            } else if (isFound) {
                break;
            }
        }

        if (arrChooseCardTime.size() > 0) {
            tvNotFound.setVisibility(GONE);
            if (currentSelectedIndex == 0) {
                rcvCheckInOut.setVisibility(View.VISIBLE);
                rcvWater.setVisibility(GONE);
                lnlNumberOfCup.setVisibility(GONE);
            } else if (currentSelectedIndex == 1) {
                tvNumberOfCup.setText(arrChooseCardTime.size() + "");
                rcvCheckInOut.setVisibility(GONE);
                rcvWater.setVisibility(View.VISIBLE);
                lnlNumberOfCup.setVisibility(View.VISIBLE);
            } else {
                rcvWater.setVisibility(GONE);
                rcvCheckInOut.setVisibility(GONE);
                lnlNumberOfCup.setVisibility(GONE);
            }
            tvNotInputCardID.setVisibility(GONE);
            tvCardNotExist.setVisibility(GONE);
        } else {
            tvNotFound.setVisibility(View.VISIBLE);
            rcvCheckInOut.setVisibility(GONE);
            rcvWater.setVisibility(GONE);
            lnlNumberOfCup.setVisibility(GONE);
            tvNotInputCardID.setVisibility(GONE);
            tvCardNotExist.setVisibility(GONE);
        }
        mCheckInOutAdapter.notifyDataSetChanged();
        mWaterAdapter.notifyDataSetChanged();
    }

    private int getCurrentSelectedRadioButtonIndex() {
        int radioButtonID = optionRdg.getCheckedRadioButtonId();
        View radioButton = optionRdg.findViewById(radioButtonID);
        return optionRdg.indexOfChild(radioButton);
    }
}