package com.zebra.scannercontrol.app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.Constants;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class HomeActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener,ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate{
    private FrameLayout llBarcode;
    private NavigationView navigationView;
    private static final int PERMISSIONS_ACCESS_COARSE_LOCATION = 10;
    static boolean firstRun = true;
    Dialog dialog;
    Dialog dialogBTAddress;
    static String btAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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
                this, drawer,toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

         navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        }else{
            Initialize();
        }
    }

    private void Initialize() {
        initializeDcsSdk();
        llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(DCSSDKDefs.DCSSDK_BT_PROTOCOL.LEGACY_B, DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.KEEP_CURRENT);
        if(barCodeView!=null) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            int x = width * 9 / 10;
            int y = x / 3;
            barCodeView.setSize(x, y);
            llBarcode.addView(barCodeView, layoutParams);
        }
        addDevConnectionsDelegate(this);
        setTitle("Pair New Scanner");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Initialize();

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void initializeDcsSdk(){
        Application.sdkHandler.dcssdkEnableAvailableScannersDetection(true);
        Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
        Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
        Application.sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.home, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        if(id==R.id.nav_pair_device){
            if(Application.isAnyScannerConnected) {
                AlertDialog.Builder dlg = new  AlertDialog.Builder(this);
                dlg.setTitle("This will disconnect your current scanner");
                //dlg.setIcon(android.R.drawable.ic_dialog_alert);
                dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg) {

                        disconnect(Application.currentConnectedScannerID);
                        Application.barcodeData.clear();
                        Application.CurScannerId=Application.SCANNER_ID_NONE;
                        finish();
                        Intent intent = new Intent(HomeActivity.this,HomeActivity.class);
                        startActivity(intent);
                    }
                });

                dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg) {

                    } });
                dlg.show();

            }
        }else if (id == R.id.nav_devices) {
            intent = new Intent(this, ScannersActivity.class);
            startActivity(intent);
        }else if (id == R.id.nav_find_cabled_scanner) {
            intent = new Intent(this, FindCabledScanner.class);
            startActivity(intent);
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

        return true;
    }


    @Override
    protected void onDestroy(){
        Application.sdkHandler.dcssdkClose();
        super.onDestroy();
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
        navigationView.getMenu().findItem(R.id.nav_about).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_pair_device).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_pair_device).setCheckable(false);
        navigationView.getMenu().findItem(R.id.nav_devices).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_connection_help).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_settings).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_find_cabled_scanner).setChecked(false);
        navigationView.getMenu().findItem(R.id.nav_find_cabled_scanner).setCheckable(false);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        TextView txtBarcodeType = (TextView)findViewById(R.id.scan_to_connect_barcode_type);
        TextView txtScannerConfiguration = (TextView)findViewById(R.id.scan_to_connect_scanner_config);
        String sourceString = "";
        txtBarcodeType.setText(Html.fromHtml(sourceString));
        txtScannerConfiguration.setText("");
        boolean dntShowMessage = settings.getBoolean(Constants.PREF_DONT_SHOW_INSTRUCTIONS, false);
        int barcode = settings.getInt(Constants.PREF_PAIRING_BARCODE_TYPE, 0);
        boolean setDefaults = settings.getBoolean(Constants.PREF_PAIRING_BARCODE_CONFIG, true);
        int protocolInt = settings.getInt(Constants.PREF_COMMUNICATION_PROTOCOL_TYPE, 0);
        String strProtocol = "SSI over Classic Bluetooth";
        llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
        DCSSDKDefs.DCSSDK_BT_PROTOCOL protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.LEGACY_B;
        DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG config = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.KEEP_CURRENT;
        if(barcode ==0){
            txtBarcodeType.setText("");
            txtScannerConfiguration.setText("");
            sourceString = "<b>"+"STC Suite "+"</b>";
            txtBarcodeType.setText(Html.fromHtml(sourceString));
            switch (protocolInt){
                case 0:
                    protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST;//SSI over Classic Bluetooth
                    strProtocol = "<b>SSI over Classic Bluetooth</>";
                    break;
                case 1:
                    protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE;//SSI over Bluetooth LE
                    strProtocol = "<b>Bluetooth LE</>";
                    break;
                default:
                    protocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_CRADLE_HOST;//SSI over Classic Bluetooth
                    break;
            }
            if(setDefaults){
                config = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS;
                txtScannerConfiguration.setText(Html.fromHtml("<i> Set Factory Defaults, Com Protocol = "+strProtocol+"</i>"));
            }else{
                txtScannerConfiguration.setText(Html.fromHtml("<i> Keep Current Settings, Com Protocol = "+strProtocol+"</i>"));
            }
        }else{
            sourceString = "<b>"+"Legacy Pairing "+"</b>";
            txtBarcodeType.setText(Html.fromHtml(sourceString));
            txtScannerConfiguration.setText("");
        }
        selectedProtocol = protocol;
        selectedConfig = config;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(protocol, config);
        if(barCodeView!=null) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            int x = width * 9 / 10;
            int y = x / 3;
            barCodeView.setSize(x, y);
            llBarcode.addView(barCodeView, layoutParams);
        }else{
            // SDK was not able to determine Bluetooth MAC. So call the dcssdkGetPairingBarcode with BT Address.

            btAddress= getDeviceBTAddress(settings);
            if(btAddress.equals("")){
                llBarcode.removeAllViews();
            }else {
                barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(protocol, config, btAddress);
                if (barCodeView != null) {
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    int height = size.y;
                    int x = width * 9 / 10;
                    int y = x / 3;
                    barCodeView.setSize(x, y);
                    llBarcode.addView(barCodeView, layoutParams);
                }
            }
        }
        if(dialogBTAddress == null && firstRun && !dntShowMessage){
            dialog = new Dialog(HomeActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_launch_instructions);

            CheckBox chkDontShow = (CheckBox)dialog.findViewById(R.id.chk_dont_show);
            chkDontShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        SharedPreferences.Editor settingsEditor = getSharedPreferences(Constants.PREFS_NAME, 0).edit();
                        settingsEditor.putBoolean(Constants.PREF_DONT_SHOW_INSTRUCTIONS, isChecked).commit();
                    }
                }
            });
            TextView continueButton = (TextView) dialog.findViewById(R.id.btn_continue);
            // if decline button is clicked, close the custom dialog
            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Close dialog
                    dialog.dismiss();
                    dialog = null;
                }
            });

            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            Window window = dialog.getWindow();
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            firstRun = false;
        }
    }

    private String getDeviceBTAddress(SharedPreferences settings) {
        String bluetoothMAC = settings.getString(Constants.PREF_BT_ADDRESS, "");
        if(bluetoothMAC.equals("")) {
            if(dialogBTAddress==null) {
                dialogBTAddress = new Dialog(HomeActivity.this);
                dialogBTAddress.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialogBTAddress.setContentView(R.layout.dialog_get_bt_address);


                TextView navigateBTAddress =  (TextView) dialogBTAddress.findViewById(R.id.txt_navigate_bt_address);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    navigateBTAddress.setText(Html.fromHtml("Navigate to \"ABOUT PHONE\">Status and <u>long tap</u> \"Bluetooth address\".", Html.FROM_HTML_MODE_LEGACY));
                }else{
                    navigateBTAddress.setText(Html.fromHtml("Navigate to \"ABOUT PHONE\">Status and <u>long tap</u> \"Bluetooth address\"."));
                }
                final TextView abtPhoneButton = (TextView) dialogBTAddress.findViewById(R.id.abt_phone);
                abtPhoneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(abtPhoneButton.getText().equals(getResources().getString(R.string.about_phone))) {
                        Intent statusSettings = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
                        startActivity(statusSettings);

                        }else{

                        if(dialogBTAddress!=null) {
                            dialogBTAddress.dismiss();
                            dialogBTAddress = null;
                            }
                            startHomeActivityAgain();
                        }

                    }
                });

                final EditText editTextBluetoothAddress = (EditText)dialogBTAddress.findViewById(R.id.text_bt_address);
                editTextBluetoothAddress.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        String bluetoothAddress = s.toString();
                        if (isValidBTAddress(bluetoothAddress)) {
                            SharedPreferences.Editor settingsEditor = getSharedPreferences(Constants.PREFS_NAME, 0).edit();
                            settingsEditor.putString(Constants.PREF_BT_ADDRESS, bluetoothAddress).commit();
                            Drawable dr = getResources().getDrawable(R.drawable.tick);
                            dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
                            editTextBluetoothAddress.setCompoundDrawables(null,null,dr,null);
                            abtPhoneButton.setText("CONTINUE");
//                            if(dialogBTAddress!=null) {
//                                dialogBTAddress.dismiss();
//                                dialogBTAddress = null;
                        }else{
                            editTextBluetoothAddress.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                            abtPhoneButton.setText(getResources().getString(R.string.about_phone));

                        }
                    }
                });

                final ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipBoard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {

                    @Override
                    public void onPrimaryClipChanged() {
                        ClipData clipData = clipBoard.getPrimaryClip();
                        ClipData.Item item = clipData.getItemAt(0);
                        String bluetoothAddress = item.getText().toString();
                        if (isValidBTAddress(bluetoothAddress)) {
                            SharedPreferences.Editor settingsEditor = getSharedPreferences(Constants.PREFS_NAME, 0).edit();
                            settingsEditor.putString(Constants.PREF_BT_ADDRESS, bluetoothAddress).commit();
                            clipBoard.removePrimaryClipChangedListener(this);
                            if(dialogBTAddress!=null) {
                                dialogBTAddress.dismiss();
                                dialogBTAddress = null;
                            }
                            startHomeActivityAgain();
                        }
                    }
                });
                dialogBTAddress.setCancelable(false);
                dialogBTAddress.setCanceledOnTouchOutside(false);
                dialogBTAddress.show();
                Window window = dialogBTAddress.getWindow();
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                bluetoothMAC = settings.getString(Constants.PREF_BT_ADDRESS, "");
            }else{
                dialogBTAddress.show();
            }
        }
        return bluetoothMAC;
    }

    private void startHomeActivityAgain() {
        Intent i = new Intent(this, HomeActivity.class);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
    }

    public boolean isValidBTAddress(String text) {
        return text != null && text.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    private int getX() {
        final float scale = this.getResources().getDisplayMetrics().density;
        int x = (int) (20 * scale + 0.5f);
        Point size = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(size);
        int width = size.x;
        return width - x;
    }

    private int getY() {
        final float scale = this.getResources().getDisplayMetrics().density;
        int y = (int) (430 * scale + 0.5f);
        return y;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadBarcode();
        if(dialog !=null){
            Window window = dialog.getWindow();
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void reloadBarcode() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = Application.sdkHandler.dcssdkGetPairingBarcode(selectedProtocol, selectedConfig);
        if(barCodeView!=null) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            int x = width * 9 / 10;
            int y = x / 3;
            barCodeView.setSize(x, y);
            llBarcode.addView(barCodeView, layoutParams);
        }
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
        ArrayList<DCSScannerInfo> activeScanners = new ArrayList<DCSScannerInfo>();
        Application.sdkHandler.dcssdkGetActiveScannersList(activeScanners);
        Intent intent = new Intent(HomeActivity.this, ActiveScannerActivity.class);

        for (DCSScannerInfo scannerInfo : Application.mScannerInfoList) {
            if (scannerInfo.getScannerID() == scannerID) {
                intent.putExtra(Constants.SCANNER_NAME, scannerInfo.getScannerName());
                intent.putExtra(Constants.SCANNER_ADDRESS, scannerInfo.getScannerHWSerialNumber());
                intent.putExtra(Constants.SCANNER_ID, scannerInfo.getScannerID());
                intent.putExtra(Constants.AUTO_RECONNECTION, scannerInfo.isAutoCommunicationSessionReestablishment());
                intent.putExtra(Constants.CONNECTED, true);
                intent.putExtra(Constants.PICKLIST_MODE,getPickListMode(scannerID));

                if(scannerInfo.getScannerModel() !=null && scannerInfo.getScannerModel().startsWith("PL3300")){ // remove this condition when CS4070 get the capability
                    intent.putExtra(Constants.PAGER_MOTOR_STATUS, true);
                }else {
                    intent.putExtra(Constants.PAGER_MOTOR_STATUS, isPagerMotorAvailable(scannerID));
                }

                intent.putExtra(Constants.BEEPER_VOLUME,getBeeperVolume(scannerID));

                Application.isAnyScannerConnected = true;
                Application.currentConnectedScannerID = scannerID;
                Application.currentConnectedScanner = scannerInfo;
                Application.lastConnectedScanner = Application.currentConnectedScanner;
                startActivity(intent);
                break;
            }
        }
        return true;
    }

    private int getBeeperVolume(int scannerID) {
        int beeperVolume = 0;

        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>140</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,in_xml,outXML,scannerID);

        if(outXML !=null) {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new StringReader(outXML.toString()));
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
                            if (name.equals("value")) {
                                beeperVolume = Integer.parseInt(text.trim());
                            }
                            break;
                    }
                    event = parser.next();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        if(beeperVolume == 0){
            return 100;
        }else if(beeperVolume == 1){
            return 50;
        }else{
            return 0;
        }
    }

    private boolean isPagerMotorAvailable(int scannerID) {
        boolean isFound = false;
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>613</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,in_xml,outXML,scannerID);
        if(outXML !=null) {
            if(outXML.toString().contains("<id>613</id>")){
                isFound = true;
            }
        }
        return isFound;
    }


    private int getPickListMode(int scannerID) {
        int attr_val = 0;
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>402</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,in_xml,outXML,scannerID);

        if(outXML !=null) {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new StringReader(outXML.toString()));
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
                            if (name.equals("value")) {
                                attr_val = Integer.parseInt(text.trim());
                            }
                            break;
                    }
                    event = parser.next();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return attr_val;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        Application.isAnyScannerConnected = false;
        Application.currentConnectedScannerID = -1;
        Application.lastConnectedScanner = Application.currentConnectedScanner;
        Application.currentConnectedScanner = null;
        return false;
    }
}
