package com.google.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.TextView;

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
import java.util.HashSet;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference myRef;
    private Button btnGetCheck;
    private CardAutoCompleteTextView inputCardID;
    private MaterialCalendarView cldView;
    private RecyclerView rcvCard;
    private CardAdapter mCardAdapter;
    private Query query;
    private ArrayList<CardDate> arrAllCardTime = new ArrayList<>();
    private ArrayList<String> arrChooseCardTime = new ArrayList<>();
    private TextView tvNotFound, tvNotInputCardID, tvCardNotExist;
    private SharedPreferences mSharedPreferences;
    private ArrayList<String> arrSuggestCardID = new ArrayList<>();
    private ArrayAdapter suggestAdapter;
    private HashSet<CalendarDay> mCldDays = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("RFIDPreference",  Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        String json = mSharedPreferences.getString("suggest", "");
        if(!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<String>>(){}.getType();
            arrSuggestCardID = gson.fromJson(json, type);
        }

        // Write a message to the database
        myRef = FirebaseDatabase.getInstance().getReference("RFID");
        rcvCard = findViewById(R.id.rcv_data);
        mCardAdapter = new CardAdapter(arrChooseCardTime);
        tvNotFound = findViewById(R.id.tv_not_found);
        tvNotInputCardID = findViewById(R.id.tv_not_input_card);
        tvCardNotExist = findViewById(R.id.tv_card_not_exist);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(RecyclerView.VERTICAL);

        rcvCard.setLayoutManager(mLayoutManager);
        rcvCard.setAdapter(mCardAdapter);

        tvNotFound.setVisibility(View.GONE);
        rcvCard.setVisibility(View.GONE);
        tvNotInputCardID.setVisibility(View.GONE);
        tvCardNotExist.setVisibility(View.GONE);

        //myRef.setValue();
        btnGetCheck =  findViewById(R.id.btn_get_card);
        inputCardID =  findViewById(R.id.input_card);
        cldView = findViewById(R.id.card_calendar);

        if (arrSuggestCardID != null && arrSuggestCardID.size() > 0) {
            inputCardID.setText(arrSuggestCardID.get(arrSuggestCardID.size() - 1));
        }

        suggestAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrSuggestCardID);
        inputCardID.setThreshold(0);
        inputCardID.setAdapter(suggestAdapter);

        inputCardID.setImeOptions(EditorInfo.IME_ACTION_DONE);

        btnGetCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                if (!inputCardID.getText().toString().equals("")) {
                    query = myRef.child(inputCardID.getText().toString()).orderByValue();
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                            if (dataSnapshot.getChildrenCount() == 0) {
                                tvNotFound.setVisibility(View.GONE);
                                rcvCard.setVisibility(View.GONE);
                                tvNotInputCardID.setVisibility(View.GONE);
                                tvCardNotExist.setVisibility(View.VISIBLE);
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
                                    mCldDays.add(CalendarDay.from(Integer.parseInt(cardDate.substring(4,8)), Integer.parseInt(cardDate.substring(2,4)),
                                            Integer.parseInt(cardDate.substring(0,2))));
                                    arrAllCardTime.add(new CardDate(cardDate, cardTime));
                                }
                                cldView.addDecorator(new EventDecorator(MainActivity.this,  mCldDays));
                                updateList(CommonUtils.getDateFromCalendar(cldView.getSelectedDate()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                } else {
                    tvNotFound.setVisibility(View.GONE);
                    rcvCard.setVisibility(View.GONE);
                    tvNotInputCardID.setVisibility(View.VISIBLE);
                    tvCardNotExist.setVisibility(View.GONE);
                }
            }
        });

        cldView.setOnDateChangedListener((widget, date, selected) -> updateList(CommonUtils.getDateFromCalendar(date)));
    }

    void updateList(String daySelected) {
        arrChooseCardTime.clear();
        boolean isFound = false;
        for (int i = 0; i < arrAllCardTime.size(); i++) {
            if (arrAllCardTime.get(i).getCardDay().equals(daySelected)) {
                isFound = true;
                arrChooseCardTime.add(arrAllCardTime.get(i).getCardTime());
            } else if (isFound) {
                break;
            }
        }
        if (arrChooseCardTime.size() > 0) {
            tvNotFound.setVisibility(View.GONE);
            rcvCard.setVisibility(View.VISIBLE);
            tvNotInputCardID.setVisibility(View.GONE);
            tvCardNotExist.setVisibility(View.GONE);
        } else {
            tvNotFound.setVisibility(View.VISIBLE);
            rcvCard.setVisibility(View.GONE);
            tvNotInputCardID.setVisibility(View.GONE);
            tvCardNotExist.setVisibility(View.GONE);
        }
        mCardAdapter.notifyDataSetChanged();

    }
}