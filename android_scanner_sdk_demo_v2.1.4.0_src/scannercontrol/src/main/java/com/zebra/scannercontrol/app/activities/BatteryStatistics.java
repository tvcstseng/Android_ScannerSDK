package com.zebra.scannercontrol.app.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.Constants;
import com.zebra.scannercontrol.app.helpers.CustomProgressDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static com.zebra.scannercontrol.RMDAttributes.*;

public class BatteryStatistics extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
    private NavigationView navigationView;
    int scannerID;
    static ArrayList<String> attrs = new ArrayList<String>();

    static MyAsyncTask cmdExecTask=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_statistics);

        Configuration configuration = getResources().getConfiguration();
        if(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(configuration.smallestScreenWidthDp<Application.minScreenWidth){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }else{
            if(configuration.screenWidthDp<Application.minScreenWidth){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Toolbar subActionBar = (Toolbar) findViewById(R.id.sub_actionbar);
        setSupportActionBar(subActionBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Active Scanner");

        setSupportActionBar(subActionBar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        scannerID = getIntent().getIntExtra(Constants.SCANNER_ID, -1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeDevConnectiosDelegate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        addDevConnectionsDelegate(this);
        boolean isBatteryAvailable = getIntent().getBooleanExtra(Constants.BATTERY_STATUS, false);
        TextView textView = (TextView)findViewById(R.id.txt_no_battery_statistics);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.layout_battery_statistics);
        if(isBatteryAvailable){
            linearLayout.setVisibility(View.VISIBLE);
            textView.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.GONE);
            fetchBatteryStats();
        }else{
            textView.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.INVISIBLE);
            linearLayout.setVisibility(View.GONE);
        }
    }

    private void fetchBatteryStats() {
        int scannerID = getIntent().getIntExtra(Constants.SCANNER_ID, -1);

        if (scannerID != -1) {

            String in_xml = "<inArgs><scannerID>" + scannerID + " </scannerID><cmdArgs><arg-xml><attrib_list>";
            in_xml+=RMD_ATTR_BAT_MANUFACTURE_DATE;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_SERIAL_NUMBER;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_MODEL_NUMBER;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_FIRMWARE_VERSION;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_DESIGN_CAPACITY;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_STATE_OF_HEALTH_METER;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_CHARGE_CYCLES_CONSUMED;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_FULL_CHARGE_CAP;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_STATE_OF_CHARGE;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_REMAINING_CAP;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_CHARGE_STATUS;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_REMAINING_TIME_TO_COMPLETE_CHARGING;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_VOLTAGE;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_CURRENT;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_TEMP_PRESENT;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_TEMP_HIGHEST;
            in_xml+=",";
            in_xml+=RMD_ATTR_BAT_TEMP_LOWEST;

            in_xml += "</attrib_list></arg-xml></cmdArgs></inArgs>";

            new MyAsyncTask(scannerID, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET).execute(new String[]{in_xml});
        } else {
            Toast.makeText(this, Constants.INVALID_SCANNER_ID_MSG, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isBatteryAvailable() {
        boolean isFound = false;
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GETALL);
        cmdExecTask.execute(new String[]{in_xml});
        try {
            cmdExecTask.get();
            Log.i("Battery","Array length = "+attrs.size());
            for(int i=0;i<attrs.size();i++){
                Log.i("Battery","i = "+i);
                if(attrs.get(i).equals("30018")){
                    isFound = true;
                    break;
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return isFound;
    }


    private class MyAsyncTask extends AsyncTask<String,Integer,Boolean> {
        int scannerId;
        private CustomProgressDialog progressDialog;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        public MyAsyncTask(int scannerId,  DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode){
            this.scannerId=scannerId;
            this.opcode=opcode;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new CustomProgressDialog(BatteryStatistics.this, "Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = false;
            StringBuilder sb = new StringBuilder();
            result = executeCommand(opcode, strings[0], sb, scannerId);
            if (opcode == DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET) {
                if (!result) {
                    return result;
                } else {
                    try {
                        Log.i(TAG, sb.toString());
                        int i = 0;
                        int attr_id = -1;
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                        parser.setInput(new StringReader(sb.toString()));
                        int event = parser.getEventType();
                        String text = null;
                        while (event != XmlPullParser.END_DOCUMENT) {
                            String name = parser.getName();
                            switch (event) {
                                case XmlPullParser.START_TAG:
                                    break;
                                case XmlPullParser.TEXT:
                                    text = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:
                                    Log.i(TAG, "Name of the end tag: " + name);
                                    if (name.equals("id")) {
                                        attr_id = Integer.parseInt(text.trim());
                                        Log.i(TAG, "ID tag found: ID: " + attr_id);
                                    } else if (name.equals("value")) {
                                        final String attr_val = text.trim();
                                        Log.i(TAG, "Value tag found: Value: " + attr_val);
                                        if (RMD_ATTR_BAT_MANUFACTURE_DATE == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_manufacture_date)).setText(attr_val);
                                                }
                                            });

                                        } else if (RMD_ATTR_BAT_SERIAL_NUMBER == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_serial)).setText(attr_val);
                                                }
                                            });

                                        } else if (RMD_ATTR_BAT_MODEL_NUMBER == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_model)).setText(attr_val);
                                                }
                                            });

                                        } else if (RMD_ATTR_BAT_FIRMWARE_VERSION == attr_id) {

//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    ((TextView) findViewById(R.id.txt_bat_firmware_version)).setText(attr_val);
//                                                }
//                                            });

                                        } else if (RMD_ATTR_BAT_DESIGN_CAPACITY == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_design_cap)).setText(attr_val+" mAhr");
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_STATE_OF_HEALTH_METER == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_state_of_health)).setText(attr_val+"%");
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_CHARGE_CYCLES_CONSUMED == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_charge_cycles_consumed)).setText(attr_val);
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_FULL_CHARGE_CAP == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_full_charge_cap)).setText(attr_val+" mAhr");
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_STATE_OF_CHARGE == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_state_of_charge)).setText(attr_val+"%");
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_REMAINING_CAP == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_remaining_cap)).setText(attr_val+" mAhr");
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_CHARGE_STATUS == attr_id) {

//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    ((TextView) findViewById(R.id.txt_bat_charge_status)).setText(attr_val);
//                                                }
//                                            });

                                        }else if (RMD_ATTR_BAT_REMAINING_TIME_TO_COMPLETE_CHARGING == attr_id) {

//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    ((TextView) findViewById(R.id.txt_bat_remaining_time_to_complete_charging)).setText(attr_val+" Minutes");
//                                                }
//                                            });

                                        }else if (RMD_ATTR_BAT_VOLTAGE == attr_id) {

//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    ((TextView) findViewById(R.id.txt_bat_voltage)).setText(attr_val+" mV");
//                                                }
//                                            });

                                        }else if (RMD_ATTR_BAT_CURRENT == attr_id) {

//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    ((TextView) findViewById(R.id.txt_bat_current)).setText(attr_val+" mA");
//                                                }
//                                            });

                                        }else if (RMD_ATTR_BAT_TEMP_PRESENT == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_present)).setText(getTempString(attr_val));
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_TEMP_HIGHEST == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_highest)).setText(getTempString(attr_val));
                                                }
                                            });

                                        }else if (RMD_ATTR_BAT_TEMP_LOWEST == attr_id) {

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.txt_bat_lowest)).setText(getTempString(attr_val));
                                                }
                                            });

                                        }

                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }

                }
            } else if (opcode == DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GETALL) {
                if (!result) {
                    return result;
                } else {
                    try {
                        int i = 0;
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                        parser.setInput(new StringReader(sb.toString()));
                        int event = parser.getEventType();
                        String text = null;
                        while (event != XmlPullParser.END_DOCUMENT) {
                            String name = parser.getName();
                            switch (event) {
                                case XmlPullParser.START_TAG:
                                    break;
                                case XmlPullParser.TEXT:
                                    text = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:
                                    if (name.equals("attribute")) {
                                        attrs.add(text.trim());
                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            return result;
        }

        private String getTempString(String attr_val) {
            String ret = "";
            int f;
            int c;
            c=Integer.parseInt(attr_val);
            f=((c*9)/5)+32;
            ret = f+"\u2109 / "+c+"\u2103";
            return  ret;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

        }


    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        if (id == R.id.nav_pair_device) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("This will disconnect your current scanner");
            //dlg.setIcon(android.R.drawable.ic_dialog_alert);
            dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                    disconnect(scannerID);
                    Application.barcodeData.clear();
                    Application.CurScannerId = Application.SCANNER_ID_NONE;
                    finish();
                    Intent intent = new Intent(BatteryStatistics.this, HomeActivity.class);
                    startActivity(intent);
                }
            });

            dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                }
            });
            dlg.show();

        } else if (id == R.id.nav_devices) {
            intent = new Intent(this, ScannersActivity.class);

            startActivity(intent);
        }else if (id == R.id.nav_find_cabled_scanner) {
            AlertDialog.Builder dlg = new  AlertDialog.Builder(this);
            dlg.setTitle("This will disconnect your current scanner");
            //dlg.setIcon(android.R.drawable.ic_dialog_alert);
            dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                    disconnect(scannerID);
                    Application.barcodeData.clear();
                    Application.CurScannerId = Application.SCANNER_ID_NONE;
                    finish();
                    Intent intent = new Intent(BatteryStatistics.this, FindCabledScanner.class);
                    startActivity(intent);
                }
            });

            dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                }
            });
            dlg.show();
        }else if (id == R.id.nav_connection_help) {
            intent = new Intent(this, ConnectionHelpActivity2.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        drawer.setSelected(true);
        return true;
    }

    @Override
    public boolean scannerHasAppeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisappeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasConnected(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        Application.barcodeData.clear();
        this.finish();
        return true;
    }

}
