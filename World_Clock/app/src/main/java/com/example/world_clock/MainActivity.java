package com.example.world_clock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static java.lang.StrictMath.abs;
import static java.util.Calendar.*;

public class MainActivity extends AppCompatActivity {

    static List<String> city = new ArrayList<String>();          // Array list with city choosen
    static String timeFormat = "12hr";                      // Current time display format
    TimeZone defaultTz = TimeZone.getDefault();             // Default time zone
    Calendar defaultC = Calendar.getInstance(defaultTz);    // Default calendar date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add some city to display them as default
        city.add(getResources().getString(R.string.sydney));
        city.add(getResources().getString(R.string.shanghai));
        city.add(getResources().getString(R.string.kualalumpur));
        city.add(getResources().getString(R.string.auckland));
        city.add(getResources().getString(R.string.newyork));

        // Keep time running by thread (real time update)
        Thread runningClock = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setup();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runningClock.start();

        // Setup button to add city display
        final ImageButton btnAddClock = (ImageButton) findViewById(R.id.btnAddClock);
        final PopupMenu popup = new PopupMenu(MainActivity.this, btnAddClock);
        popup.getMenuInflater().inflate(R.menu.add_clock_menu, popup.getMenu());
        btnAddClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        // Check duplicate before add city to display
                        if(duplicateCheck(item) == false){
                            Toast.makeText(MainActivity.this, item.getTitle() + " is already showing.", Toast.LENGTH_SHORT).show();
                            return false;
                        } else {
                            city.add(item.getTitle().toString());
                            Toast.makeText(MainActivity.this, item.getTitle() + " was added.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        // Setup button to switch time format
        Button btnSwitcher = (Button) findViewById(R.id.formatSwitcher);
        btnSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timeFormat.equals("12hr")){
                    timeFormat = "24hr";
                    Toast.makeText(MainActivity.this, "Current Time Format: 24HR", Toast.LENGTH_SHORT).show();
                } else {
                    timeFormat = "12hr";
                    Toast.makeText(MainActivity.this, "Current Time Format: 12HR", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Clock setup
    public void setup(){
        TimeZone tz;    // TimeZone of city choose
        Calendar c;     // Calendar date of city choose

        // declare the day of week list
        String[] strDays = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thusday",
                "Friday", "Saturday" };

        // Set up linear layout to insert into scroll view
        final LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.removeAllViews();

        // Get Scroll view from xml
        final ScrollView sc = (ScrollView) findViewById(R.id.scrollViewClock);
        sc.removeAllViews();

        // Add city according to city array list
        for (int i = 0; i < city.size(); i++) {
            // Remove space and symbol city name
            String plainCityName = city.get(i).replaceAll("\\s+","").toLowerCase();

            // Turn the city name string to android id
            int cityId = getResources().getIdentifier(plainCityName,"id", getPackageName());

            // Get layout from clock.xml and use inflater to put them into linear layout
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View clockView = inflater.inflate(R.layout.clock, null);
            clockView.setId(cityId);

            // Set id for remove clock button
            ImageButton btnRemove = (ImageButton) clockView.findViewById(R.id.btnRemove);
            btnRemove.setId(cityId);

            // Set city name display for clock
            TextView cityName = (TextView) clockView.findViewById(R.id.cityName);
            cityName.setText(city.get(i));

            // Get image drawable id and set city image
            int imageId = getResources().getIdentifier(plainCityName, "drawable", getPackageName());
            ImageView cityImage = (ImageView) clockView.findViewById((R.id.cityImage));
            cityImage.setImageResource(imageId);

            // Get timezone id and set timezone according to city
            String timeZoneStr = "timezone_" + plainCityName;
            int timeZoneIdFromStr = getResources().getIdentifier(timeZoneStr, "string", getPackageName());
            String timeZoneId = getResources().getString(timeZoneIdFromStr);

            // Get the current time of city according to timezone
            tz = TimeZone.getTimeZone(timeZoneId);
            c = getInstance(tz);

            // Set format of time display: 12HR and 24HR
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss aa");
            if(timeFormat.equals("24hr")){
                sdf = new SimpleDateFormat("HH:mm:ss");
            }
            sdf.setTimeZone(tz);

            // Set the time display to user
            TextView time = (TextView) clockView.findViewById(R.id.timeDisplay);
            time.setText(sdf.format(c.getTime()));

            // Set the date and their details
            TextView dateDetails = (TextView) clockView.findViewById(R.id.dateDetails);
            sdf = new SimpleDateFormat("dd/MM/YYYY");
            sdf.setTimeZone(tz);

            // Calculate the different of hours between city display and current location's time (default time)
            long currentTime = System.currentTimeMillis();
            int defaultTime = defaultTz.getOffset(currentTime);
            int cityTime = TimeZone.getTimeZone(timeZoneId).getOffset(currentTime);
            int hourDifference = (cityTime - defaultTime) / (1000 * 60 * 60);
            String diffDay = "Today";
            String diffHour = hourDifference + "HRS";
            if(hourDifference >= 0){
                diffHour = "+" +  diffHour;
            }

            if(c.get(DATE) - defaultC.get(DATE) == 1){
                diffDay = "Tomorrow";
            } else if(c.get(DATE) - defaultC.get(DATE) == -1){
                diffDay = "Yesterday";
            }

            // Set date details to display
            String currTime = sdf.format(c.getTime());
            String dayOfWeek = strDays[c.get(Calendar.DAY_OF_WEEK) - 1];
            dateDetails.setText(currTime +", "+ dayOfWeek + ", " +diffDay+", " + diffHour);

            // Add the finish setup view into linear layout
            ll.addView(clockView);
        }
        // Add the linear layout into scroll view
        sc.addView(ll);
    }

    // Check duplicate of clock show
    public boolean duplicateCheck(MenuItem item){
        for(int i = 0; i < city.size(); i++){
            if(item.getTitle().equals(city.get(i))){
                return false;
            }
        }
        return true;
    }

    // Button method to hide clock
    public void removeClock(View view){
        String cityName = "";
        switch (view.getId()){
            case R.id.sydney:
                cityName = getResources().getString(R.string.sydney);
                break;
            case R.id.london:
                cityName = getResources().getString(R.string.london);
                break;
            case R.id.kualalumpur:
                cityName = getResources().getString(R.string.kualalumpur);
                break;
            case R.id.paris:
                cityName = getResources().getString(R.string.paris);
                break;
            case R.id.newyork:
                cityName = getResources().getString(R.string.newyork);
                break;
            case R.id.shanghai:
                cityName = getResources().getString(R.string.shanghai);
                break;
            case R.id.auckland:
                cityName = getResources().getString(R.string.auckland);
                break;
            case R.id.dubai:
                cityName = getResources().getString(R.string.dubai);
                break;
            case R.id.berlin:
                cityName = getResources().getString(R.string.berlin);
                break;
            case R.id.tokyo:
                cityName = getResources().getString(R.string.tokyo);
                break;
            case R.id.toronto:
                cityName = getResources().getString(R.string.toronto);
                break;
            case R.id.singapore:
                cityName = getResources().getString(R.string.singapore);
                break;
            case R.id.rome:
                cityName = getResources().getString(R.string.rome);
                break;
            case R.id.losangeles:
                cityName = getResources().getString(R.string.losangeles);
                break;
            case R.id.taipei:
                cityName = getResources().getString(R.string.taipei);
                break;
        }
        city.remove(cityName);
        Toast.makeText(MainActivity.this, cityName +" was removed.", Toast.LENGTH_SHORT).show();
    }
}
