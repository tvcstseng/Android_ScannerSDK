package com.zebra.scannercontrol.app.services;

import android.app.IntentService;
import android.content.Intent;

import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.Constants;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *
 * This service sends out an ordered broadcast of the intent received
 * This is a tentative implementation(Can be changed)
 */
public class NotificationsService extends IntentService {

    //Integer to act as the Notification ID
    //Also used to test various events
    private static int ALTERNATE_INTENT_ACTIONS = 1;

    public NotificationsService() {

        super("NotificationsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null){
            if(intent.getParcelableExtra(Constants.DATA_BLUETOOTH_DEVICE) != null){
                sendCustomBroadcast(intent);
            }else {
                sendCustomBroadcast(intent.getStringExtra(Constants.INTENT_ACTION), intent.getStringExtra(Constants.INTENT_DATA));
            }
        }
    }

    /**
     * Method to set the text for the notification/alert, ID for the notification etc...
     *
     * @param action - String representing the action
     * @param descText - Description of the action
     */
    private void sendCustomBroadcast(String action, String descText){
        Intent broadcast = new Intent(action);
        broadcast.putExtra(Constants.NOTIFICATIONS_TEXT, descText);
        broadcast.putExtra(Constants.NOTIFICATIONS_ID, Application.INTENT_ID++);
        sendOrderedBroadcast(broadcast, null);
    }

    /**
     * Method to set the text for the notification/alert, ID for the notification etc...
     *
     * @param intent - Intent for broadcasting
     */
    private void sendCustomBroadcast(Intent intent){
        Intent broadcast = new Intent(intent.getAction());
        broadcast.putExtra(Constants.DATA_BLUETOOTH_DEVICE, intent.getParcelableExtra(Constants.DATA_BLUETOOTH_DEVICE));
       sendOrderedBroadcast(broadcast, null);
    }
}
