package mrsk.pr_911;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Spinner;
import android.widget.ListView;
import android.database.Cursor;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;



public class SecondActivity extends ActionBarActivity implements  LoaderCallbacks<Cursor> {

    String[] data = {"cinema", "Maria-ra", "factory", "bank", "station"};
    ListView lvMain;
    ArrayList<HashMap<String, String>> myArrList = new ArrayList<HashMap<String, String>>();
    HashMap<String, String> map;
    SimpleCursorAdapter scAdapter;
    TextView tvEnabledGPS;
    TextView tvStatusGPS;
    TextView tvLocationGPS;
    TextView tvEnabledNet;
    TextView tvStatusNet;
    TextView tvLocationNet;

    private LocationManager locationManager;




    private static final int CM_DELETE_ID = 1;

    DB db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        tvEnabledGPS = (TextView) findViewById(R.id.tvEnabledGPS);
        tvStatusGPS = (TextView) findViewById(R.id.tvStatusGPS);
        tvLocationGPS = (TextView) findViewById(R.id.tvLocationGPS);
        tvEnabledNet = (TextView) findViewById(R.id.tvEnabledNet);
        tvStatusNet = (TextView) findViewById(R.id.tvStatusNet);
        tvLocationNet = (TextView) findViewById(R.id.tvLocationNet);


        // открываем подключение к БД
        db = new DB(this);
        db.open();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // формируем столбцы сопоставления
        String[] from = new String[] { DB.COLUMN_email, DB.COLUMN_name };
        int[] to = new int[] {R.id.tvText, R.id.tvText2};
        // создааем адаптер и настраиваем список
        scAdapter = new SimpleCursorAdapter(this, R.layout.item, null, from, to, 0);
        lvMain = (ListView) findViewById(R.id.lvMain);
        lvMain.setAdapter(scAdapter);

        // добавляем контекстное меню к списку
        registerForContextMenu(lvMain);

        // создаем лоадер для чтения данных
        getSupportLoaderManager().initLoader(0, null, this);


        // адаптер2
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter2);
        // заголовок
        spinner.setPrompt("Title");
        // выделяем элемент
        spinner.setSelection(0);
    }

    public void onButtonClick(View view) {
        // добавляем запись
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy hh:mm");
        String sDate = sdf.format(new Date(System.currentTimeMillis()));
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        String strcc = spinner.getSelectedItem().toString();
        db.addRec(strcc, sDate);
        // получаем новый курсор с данными
        getSupportLoaderManager().getLoader(0).forceLoad();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_second, menu);
        return true;
    }
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_DELETE_ID, 0, R.string.delete_record);
    }

    public void onButtonClickClear(View view) {
              // удаляем запись в БД
            db.delRec();
        getSupportLoaderManager().getLoader(0).forceLoad();
    }

    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new MyCursorLoader(this, db);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class MyCursorLoader extends CursorLoader {

        DB db;

        public MyCursorLoader(Context context, DB db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor cursor = db.getAllData();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return cursor;
        }

    }





    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(SecondActivity.this, Setting.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.help) {
            Intent intent = new Intent(SecondActivity.this, HelpActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                locationListener);
        checkEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabled();
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkEnabled();
            showLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                tvStatusGPS.setText("Status: " + String.valueOf(status));
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                tvStatusNet.setText("Status: " + String.valueOf(status));
            }
        }
    };

    private void showLocation(Location location) {
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            tvLocationGPS.setText(formatLocation(location));
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            tvLocationNet.setText(formatLocation(location));
        }
    }

    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format(
                "Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(), new Date(
                        location.getTime()));
    }

    private void checkEnabled() {
        tvEnabledGPS.setText("Enabled: "
                + locationManager
    .isProviderEnabled(LocationManager.GPS_PROVIDER));
    tvEnabledNet.setText("Enabled: "
            + locationManager
            .isProviderEnabled(LocationManager.NETWORK_PROVIDER));
}

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }





}
