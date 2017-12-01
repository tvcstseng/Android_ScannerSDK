package com.zebra.scannercontrol.app.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.fragments.BarcodeFargment;
import com.zebra.scannercontrol.app.helpers.ActiveScannerAdapter;
import com.zebra.scannercontrol.app.helpers.Constants;
import com.zebra.scannercontrol.app.helpers.CustomProgressDialog;
import com.zebra.scannercontrol.app.receivers.NotificationsReceiver;


import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.concurrent.ExecutionException;

import static com.zebra.scannercontrol.RMDAttributes.RMD_ATTR_VALUE_ACTION_HIGH_HIGH_LOW_LOW_BEEP;
import static com.zebra.scannercontrol.app.helpers.Constants.DEBUG_TYPE.TYPE_DEBUG;

public class ActiveScannerActivity extends BaseActivity implements  NavigationView.OnNavigationItemSelectedListener,
        ActionBar.TabListener,ScannerAppEngine.IScannerAppEngineDevEventsDelegate,ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate{
    private ViewPager viewPager;
    ActiveScannerAdapter mAdapter;
    TabLayout tabLayout;
    private NavigationView navigationView;
    static int picklistMode;

    public boolean isPagerMotorAvailable() {
        return pagerMotorAvailable;
    }

    static boolean pagerMotorAvailable;
    private int scannerID;
    TextView barcodeCount;
    int iBarcodeCount;
    int  BARCODE_TAB = 1;
    int ADVANCED_TAB = 2;
    static MyAsyncTask cmdExecTask=null;
    Button btnFindScanner = null;
    static final int ENABLE_FIND_NEW_SCANNER =1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_scanner);

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
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        addDevConnectionsDelegate(this);
        scannerID = getIntent().getIntExtra(Constants.SCANNER_ID, -1);
        BaseActivity.lastConnectedScannerID = scannerID;
        String scannerName = getIntent().getStringExtra(Constants.SCANNER_NAME);
        String address = getIntent().getStringExtra(Constants.SCANNER_ADDRESS);

        picklistMode = getIntent().getIntExtra(Constants.PICKLIST_MODE, 0);

        pagerMotorAvailable = getIntent().getBooleanExtra(Constants.PAGER_MOTOR_STATUS,false);

        Application.CurScannerId = scannerID;
        Application.CurScannerName = scannerName;
        Application.CurScannerAddress = address;
        // Initilization
        viewPager = (ViewPager) findViewById(R.id.activeScannerPager);

        mAdapter= new ActiveScannerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        mAdapter.notifyDataSetChanged();
        iBarcodeCount = 0;
        /**          * on swiping the viewpager make respective tab selected          * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Constants.logAsMessage(TYPE_DEBUG, getClass().getSimpleName(), " Position is --- " + position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
        if (getIntent().getBooleanExtra(Constants.SHOW_BARCODE_VIEW,false))
           viewPager.setCurrentItem(BARCODE_TAB);

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);
        nMgr.cancel(NotificationsReceiver.DEFAULT_NOTIFICATION_ID);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.no_items, menu);
        return true;
    }



    @Override
    protected void onResume() {
        super.onResume();
        addDevEventsDelegate(this);
        addDevConnectionsDelegate(this);
        addMissedBarcodes();

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);
        nMgr.cancel(NotificationsReceiver.DEFAULT_NOTIFICATION_ID);
        navigationView.getMenu().findItem(R.id.nav_about).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_pair_device).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_pair_device).setCheckable(false);
        navigationView.getMenu().findItem(R.id.nav_find_cabled_scanner).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_find_cabled_scanner).setCheckable(false);
        navigationView.getMenu().findItem(R.id.nav_devices).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_connection_help).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_settings).setChecked(false);

        if(waitingForFWReboot){
            viewPager.setCurrentItem(ADVANCED_TAB);
            Intent intent = new Intent(this, UpdateFirmware.class);
            intent.putExtra(Constants.SCANNER_ID, scannerID);
            intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
            intent.putExtra(Constants.FW_REBOOT, true);
            startActivity(intent);
            setWaitingForFWReboot(false);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDevEventsDelegate(this);
        removeDevConnectiosDelegate(this);
    }

    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            minimizeApp();
            //super.onBackPressed();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        Constants.logAsMessage(TYPE_DEBUG, getClass().getSimpleName(), "onTabSelected() Position is --- " + tab.getPosition());
        // on tab selected
        // show respected fragment view
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }


    public void startFirmware(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_START_NEW_FIRMWARE,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void abortFirmware(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_ABORT_UPDATE_FIRMWARE,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void loadLedActions(View view) {
        Intent intent = new Intent(this, LEDActivity.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);
    }

    public void loadBeeperActions(View view) {
        Intent intent = new Intent(this, BeeperActionsActivity.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.BEEPER_VOLUME,getIntent().getIntExtra(Constants.BEEPER_VOLUME, 0));
        startActivity(intent);
    }

    public void loadAssert(View view) {
        Intent intent = new Intent(this, AssertActivity.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);
    }
    public void symbologiesClicked(View view) {
        Intent intent = new Intent(this, SymbologiesActivity.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);
    }

    public void enableScanning(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID, DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_ENABLE,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void disableScanning(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_DISABLE,null);
        cmdExecTask.execute(new String[]{in_xml});
    }



    public void aimOn(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_AIM_ON,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void aimOff(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_AIM_OFF,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void vibrationFeedback(View view) {

        Intent intent = new Intent(this, VibrationFeedback.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);

//        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
//        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_VIBRATION_FEEDBACK,null);
//        cmdExecTask.execute(new String[]{in_xml});
    }

    public void pullTrigger(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void releaseTrigger(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_RELEASE_TRIGGER,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public int getPickListMode(){
//        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>402</attrib_list></arg-xml></cmdArgs></inArgs>";
//        StringBuilder outXML = new StringBuilder();
//        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,outXML);
//        cmdExecTask.execute(new String[]{in_xml});
        int attr_val = 0;
//        try {
//            cmdExecTask.get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//        if(outXML !=null) {
//            try {
//                XmlPullParser parser = Xml.newPullParser();
//                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//                parser.setInput(new StringReader(outXML.toString()));
//                int event = parser.getEventType();
//                String text = null;
//                while (event != XmlPullParser.END_DOCUMENT) {
//                    String name = parser.getName();
//                    switch (event) {
//                        case XmlPullParser.START_TAG:
//                            break;
//                        case XmlPullParser.TEXT:
//                            text = parser.getText();
//                            break;
//
//                        case XmlPullParser.END_TAG:
//                           if (name.equals("value")) {
//                                attr_val = Integer.parseInt(text.trim());
//                           }
//                            break;
//                    }
//                    event = parser.next();
//                }
//            } catch (Exception e) {
//                Log.e(TAG, e.toString());
//            }
//        }
        return picklistMode;
    }


    public int getScannerID() {
        return scannerID;
    }

    private void addMissedBarcodes(){
        if(Application.barcodeData.size() !=iBarcodeCount){

            for(int i=iBarcodeCount; i<Application.barcodeData.size();i++){
                scannerBarcodeEvent(Application.barcodeData.get(i).getBarcodeData(),Application.barcodeData.get(i).getBarcodeType(),Application.barcodeData.get(i).getFromScannerID());
            }
        }
    }

    @Override
    public void scannerBarcodeEvent(byte[] barcodeData, int barcodeType, int scannerID) {
        BarcodeFargment barcodeFargment = (BarcodeFargment) mAdapter.getRegisteredFragment(1);
        if (barcodeFargment != null) {
            barcodeFargment.showBarCode(barcodeData, barcodeType, scannerID);
            barcodeCount = (TextView) findViewById(R.id.barcodesListCount);
            barcodeCount.setText("Barcodes Scanned: " + Integer.toString(++iBarcodeCount));
            if (iBarcodeCount > 0) {
                Button btnClear = (Button) findViewById(R.id.btnClearList);
                btnClear.setEnabled(true);
            }
            if (!Application.isFirmwareUpdateInProgress) {
                viewPager.setCurrentItem(BARCODE_TAB);
            }
        }
    }

    @Override
    public void scannerFirmwareUpdateEvent(FirmwareUpdateEvent firmwareUpdateEvent) {

    }
    public void clearList(View view) {
        BarcodeFargment barcodeFargment=(BarcodeFargment)mAdapter.getRegisteredFragment(1);
        if(barcodeFargment!=null ) {
            barcodeFargment.clearList();
            barcodeCount = (TextView)findViewById(R.id.barcodesListCount);
            iBarcodeCount = 0;
            barcodeCount.setText("Barcodes Scanned: " + Integer.toString(iBarcodeCount));
            Button btnClear = (Button)findViewById(R.id.btnClearList);
            btnClear.setEnabled(false);
        }
    }

    public void updateBarcodeCount() {
        if (Application.barcodeData.size() != iBarcodeCount) {
            barcodeCount = (TextView) findViewById(R.id.barcodesListCount);
            iBarcodeCount = Application.barcodeData.size();
            barcodeCount.setText("Barcodes Scanned: " + Integer.toString(iBarcodeCount));
            if(iBarcodeCount>0){
                Button btnClear = (Button)findViewById(R.id.btnClearList);
                btnClear.setEnabled(true);
            }
        }

    }
    @Override
    public boolean scannerHasAppeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisappeared(int scannerID) {
        if(null !=cmdExecTask){
            cmdExecTask.cancel(true);
        }
        Application.barcodeData.clear();
        this.finish();
        return true;
    }

    @Override
    public boolean scannerHasConnected(int scannerID) {
        Application.barcodeData.clear();
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        Application.barcodeData.clear();
        this.finish();
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        if(id==R.id.nav_pair_device){
            AlertDialog.Builder dlg = new  AlertDialog.Builder(this);
            dlg.setTitle("This will disconnect your current scanner");
            //dlg.setIcon(android.R.drawable.ic_dialog_alert);
            dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                    disconnect(scannerID);
                    Application.barcodeData.clear();
                    Application.CurScannerId=Application.SCANNER_ID_NONE;
                    finish();
                   Intent intent = new Intent(ActiveScannerActivity.this,HomeActivity.class);
                    startActivity(intent);
                }
            });

            dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                } });
            dlg.show();

        }else if (id == R.id.nav_devices) {
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
                    Intent intent = new Intent(ActiveScannerActivity.this, FindCabledScanner.class);
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

    public void setPickListMode(int picklistInt) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list><attribute><id>" +402+"</id><datatype>B</datatype><value>" + picklistInt + "</value></attribute></attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_SET,outXML);
        cmdExecTask.execute(new String[]{in_xml});
    }

    public void loadUpdateFirmware(View view) {
        Intent intent = new Intent(this, UpdateFirmware.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);
    }

    public void loadBatteryStatistics(View view) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        new AsyncTaskBatteryAvailable(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GETALL,this,BatteryStatistics.class).execute(new String[]{in_xml});

    }


    private class AsyncTaskBatteryAvailable extends AsyncTask<String,Integer,Boolean> {
        int scannerId;
        Context context;
        Class targetClass;
        private CustomProgressDialog progressDialog;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        public AsyncTaskBatteryAvailable(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, Context context, Class targetClass){
            this.scannerId=scannerId;
            this.opcode=opcode;
            this.context = context;
            this.targetClass = targetClass;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new CustomProgressDialog(ActiveScannerActivity.this, "Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = false;
            StringBuilder sb = new StringBuilder();
            result = executeCommand(opcode, strings[0], sb, scannerId);
            if (opcode == DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GETALL) {
                if (!result) {
                    return result;
                } else {
                    result = false;
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
                                        if(text.trim().equals("30018")){
                                            result = true;
                                            return result;
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
            }
            return result;
        }


        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            Intent intent = new Intent(context, targetClass);
            intent.putExtra(Constants.SCANNER_ID, scannerID);
            intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
            intent.putExtra(Constants.BATTERY_STATUS, b);
            startActivity(intent);
        }


    }

    public void findScanner(View view) {
        btnFindScanner = (Button) findViewById(R.id.btn_find_scanner);
        if(btnFindScanner!=null){
            btnFindScanner.setEnabled(false);
        }
        new FindScannerTask(scannerID).execute();
    }

    public void loadSampleBarcodes(View view) {
        Intent intent = new Intent(this, SampleBarcodes.class);
        intent.putExtra(Constants.SCANNER_ID, scannerID);
        intent.putExtra(Constants.SCANNER_NAME, getIntent().getStringExtra(Constants.SCANNER_NAME));
        startActivity(intent);
    }


    private class MyAsyncTask extends AsyncTask<String,Integer,Boolean>{
    int scannerId;
        StringBuilder outXML;
    DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
    private CustomProgressDialog progressDialog;

    public MyAsyncTask(int scannerId,  DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode,StringBuilder outXML){
       this.scannerId = scannerId;
        this.opcode = opcode;
        this.outXML = outXML;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new CustomProgressDialog(ActiveScannerActivity.this, "Execute Command...");
        progressDialog.show();
    }


    @Override
    protected Boolean doInBackground(String... strings) {
       return  executeCommand(opcode,strings[0],outXML,scannerId);
    }

    @Override
    protected void onPostExecute(Boolean b) {
        super.onPostExecute(b);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        if(!b){
            Toast.makeText(ActiveScannerActivity.this, "Cannot perform the action", Toast.LENGTH_SHORT).show();
        }
    }





}


    private class FindScannerTask extends AsyncTask<String,Integer,Boolean>{
        int scannerId;

        public FindScannerTask(int scannerId){
            this.scannerId = scannerId;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected Boolean doInBackground(String... strings) {

            long t0 =System.currentTimeMillis();

            TurnOnLEDPattern();
            BeepScanner();
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(System.currentTimeMillis()-t0 < 3000) {
                VibrateScanner();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                VibrateScanner();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BeepScanner();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                VibrateScanner();
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TurnOffLEDPattern();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if(btnFindScanner!=null){
                btnFindScanner.setEnabled(true);
            }

        }
        private void TurnOnLEDPattern() {
            String inXML = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-int>" +
                    88 + "</arg-int></cmdArgs></inArgs>";
            StringBuilder outXML = new StringBuilder();
            executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_SET_ACTION, inXML, outXML, scannerID);
        }

        private void TurnOffLEDPattern() {
            String inXML = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-int>" +
                    90 + "</arg-int></cmdArgs></inArgs>";
            StringBuilder outXML = new StringBuilder();
            executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_SET_ACTION, inXML, outXML, scannerID);
        }

        private void VibrateScanner() {
            String inXML = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs>";
            StringBuilder outXML = new StringBuilder();
            executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_VIBRATION_FEEDBACK, inXML, outXML, scannerID);
        }

        private void BeepScanner() {
            String inXML = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-int>" +
                    RMD_ATTR_VALUE_ACTION_HIGH_HIGH_LOW_LOW_BEEP + "</arg-int></cmdArgs></inArgs>";
            StringBuilder outXML = new StringBuilder();
            executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_SET_ACTION, inXML, outXML, scannerID);
        }

    }

}
