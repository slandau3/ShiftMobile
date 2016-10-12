package edu.rit.cs.steven_landau.shiftmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import edu.rit.cs.steven_landau.shiftmobile.MainActivity;

/**
 * Created by Steven Landau on 9/27/2016.
 */
public class TextReceiver extends BroadcastReceiver{
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public String thoughtMessage;





    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(SMS_RECEIVED) || intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Bundle intentExtras = intent.getExtras();
            if (intentExtras != null) {
                Object[] sms = (Object[]) intentExtras.get("pdus");
                assert sms != null;
                for (int i = 0; i < sms.length; ++i) {
                    String format = intentExtras.getString("format");
                    SmsMessage smsMessage = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);
                    }

                    assert smsMessage != null;
                    String contents = smsMessage.getMessageBody();
                    String number = smsMessage.getOriginatingAddress();
                    String name = MainActivity.getName(number);
                    Log.i(MainActivity.TAG, "the number is : " + number + " the name is : " + name);
                    try {
                        if (!name.equals("NF")) {
                            Log.i(MainActivity.TAG, contents + "   " + number + "  " + name);
                            MainActivity.output.writeObject(new SendCard(contents, number, name));
                            MainActivity.output.flush();
                            Log.i(MainActivity.TAG, "written");
                        }  // Do nothing right now if the person is not in our contacts
                    } catch (Exception e) { // TODO
                        Log.i(MainActivity.TAG, e.getMessage()); // could have lost connection
                    }
                }
            }
        }
    }
}
