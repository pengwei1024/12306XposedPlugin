package com.mobileTicket.hello12306.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.joybar.librarycalendar.data.CalendarDate;
import com.joybar.librarycalendar.fragment.CalendarViewFragment;
import com.joybar.librarycalendar.fragment.CalendarViewPagerFragment;
import com.mobileTicket.hello12306.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity implements CalendarViewPagerFragment.OnPageChangeListener,
        CalendarViewFragment.OnDateClickListener,
        CalendarViewFragment.OnDateCancelListener {

    private TextView title;
    private Set<String> stringSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        title = findViewById(R.id.title);
        title.setText(String.format("%s - %s", Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1));
        initFragment();
    }

    private void initFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        Fragment fragment = CalendarViewPagerFragment.newInstance(false);
        tx.replace(R.id.content, fragment);
        tx.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calender, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.sure) {
            Intent intent = new Intent();
            intent.putStringArrayListExtra("data", new ArrayList<>(stringSet));
            setResult(RESULT_OK, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDateCancel(CalendarDate calendarDate) {
        stringSet.remove(calendarDateToString(calendarDate));
    }

    @Override
    public void onDateClick(CalendarDate calendarDate) {
        stringSet.add(calendarDateToString(calendarDate));
    }

    @Override
    public void onPageChange(int year, int month) {
        title.setText(String.format("%s - %s", year, month));
    }

    private String calendarDateToString(CalendarDate calendarDate) {
        int year = calendarDate.getSolar().solarYear;
        int month = calendarDate.getSolar().solarMonth;
        int day = calendarDate.getSolar().solarDay;
        return String.format("%s%s%s", year, doubleChar(month), doubleChar(day));
    }

    private String doubleChar(int num) {
        if (num < 10) {
            return "0" + num;
        }
        return String.valueOf(num);
    }
}
