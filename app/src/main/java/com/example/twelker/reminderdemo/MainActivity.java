package com.example.twelker.reminderdemo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ReminderClickListener, LoaderManager.LoaderCallbacks<Cursor>, LocationListener {

    //Local variables
    private ReminderAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private EditText mNewReminderText;

    //TODO: watch 2: define local variable as an instance of the LocationManager class
    private LocationManager mLocationManager;

    //Database related local variables
    private Cursor mCursor;

    //Constants used when calling the detail activity
    public static final String INTENT_DETAIL_ROW_NUMBER = "Row number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialize the local variables
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mNewReminderText = (EditText) findViewById(R.id.editText_main);

        getSupportLoaderManager().initLoader(0, null, this);

//        //Fill with some reminders
//        for (int i = 0; i < 20; i++) {
//            String temp = "Reminder " + Integer.toString(i);
//            Reminder tempReminder = new Reminder(temp);
//            mReminders.add(tempReminder);
//        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Get the user text from the textfield
                String text = mNewReminderText.getText().toString();

                //Check if some text has been added
                if (!(TextUtils.isEmpty(text))) {

                    //Add the reminder with given text to the database
                //    mDataSource.createReminder(text);
                    ContentValues values = new ContentValues();
                    values.put(RemindersContract.ReminderEntry.COLUMN_NAME_REMINDER, text);
                    getContentResolver().insert(RemindersProvider.CONTENT_URI, values);

                    //Initialize the EditText for the next item
                    mNewReminderText.setText("");
                } else {
                    //Show a message to the user if the text field is empty
                    Snackbar.make(view, "Please enter some text in the textfield", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        /*
Add a touch helper to the RecyclerView to recognize when a user swipes to delete a list entry.
An ItemTouchHelper enables touch behavior (like swipe and move) on each ViewHolder,
and uses callbacks to signal when a user is performing these actions.
*/
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder
                            target) {
                        return false;
                    }

                    //Called when a user swipes left or right on a ViewHolder
                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {

                        //Get the index corresponding to the selected position
                        mCursor.moveToPosition(viewHolder.getAdapterPosition());
                        long index = mCursor.getLong(mCursor.getColumnIndex(RemindersContract.ReminderEntry._ID));

                        //Delete the entry
                        Uri singleUri = ContentUris.withAppendedId(RemindersProvider.CONTENT_URI,index);
                        getContentResolver().delete(singleUri, null, null);
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        //TODO watch 3: initiate the location manager instance
        mLocationManager=(LocationManager) getSystemService(LOCATION_SERVICE);

        //TODO watch 4: request location updates through GPS and the network; they will be deliverd through a callback interface
//Request updates from GPS; every 5000 ms; no distance parameter
//(if > 0, and there is no movement, no updates received)
//“this”: the place where the interface methods are implemented
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
        }
        catch (SecurityException e) {
            Log.d("MainActivity", " onLocationChanged: " + "SecurityException: \n" + e.toString());
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void reminderOnClick(long index) {
        Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
        intent.putExtra(INTENT_DETAIL_ROW_NUMBER, index);
        startActivity(intent);
    }

    @Override
    public void reminderOnLongClick(long id) {

        //Create an instance of the DetailFragment class
        final DetailFragment detailFragment = DetailFragment.newInstance(id);

        //Use a FragmentManager and transaction to add the fragment to the screen
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();

        //and here is the transaction to add the fragment to the screen
        fragmentManager.beginTransaction()
                .replace(R.id.detailFragment, detailFragment, "detailFragment")
                .addToBackStack(null)
                .commit();
    }

    //TODO watch 5: when onPause, unregister from the system service
    protected void onPause() {
        super.onPause();

        mLocationManager.removeUpdates(this);
    }


    //TODO watch 6: when onResume, re-register for the system service
    protected void onResume() {

        super.onResume();

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
        }
        catch (SecurityException e) {
            Log.d("MainActivity", " onLocationChanged: " + "SecurityException: \n" + e.toString());
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        CursorLoader cursorLoader = new CursorLoader(this, RemindersProvider.CONTENT_URI, null,
                null, null, null);
        return cursorLoader;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mCursor = cursor;
        mAdapter = new ReminderAdapter(mCursor, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(mCursor);
    }

    //TODO watch 7: the location change listener interface callback
    @Override
    public void onLocationChanged(Location location) {

        double altitude = location.getAltitude();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double speed = location.getSpeed();
        long time = location.getTime(); //when the update was received
        String providerName = location.getProvider(); //Name of the provider, in case you have registered for multiple

//Convert to a readable date and time
        SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String dateToStr = format.format(time);

//Add the reminder with lat/lon/time to the database
//Lat/long 4th decimal is about 10 metres, fine for GPS.
//final: it will be used later in an inner method.
        ContentValues values = new ContentValues();
        final String data = "provider: " + providerName + "\nlatitude: " + String.format("%.4f", latitude) +
                "\nlongitude: " + String.format("%.4f", longitude) +
                "\ntime: " + dateToStr ;

        //TODO watch 8: put the data into the content provider database; the Loader does its work to update the UI
        values.put(RemindersContract.ReminderEntry.COLUMN_NAME_REMINDER, data);
        getContentResolver().insert(RemindersProvider.CONTENT_URI, values);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
