package edu.rit.cs.steven_landau.shiftmobile;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static Socket server;
    public static ObjectInputStream input;
    public static ObjectOutputStream output;
    public static HashMap<String, String> numToName = new HashMap<>();
    public static final String TAG = "SL";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide(); // Don't know why I can't permanently delete it.

        getPermissions();
        parseContacts();
        connect();
    }

    private void connect() {
        try {
            server = new Socket("localhost", 8012);
            output = new ObjectOutputStream(server.getOutputStream());
            output.writeObject(new Mobile());   // Let the server know we are a mobile device
            output.flush();
            input = new ObjectInputStream(server.getInputStream());
            new Thread(() -> {
                onReceiveFromServer();
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Wait for an object to be sent by the server.
     * An object being sent means means that the PC client is asking us to
     * send a text message to someone. Perhaps other requests will be sent
     * in the future.
     */
    private void onReceiveFromServer() {
        while (true) {
            try {
                Thread.sleep(100);   // No need to check from the server every nanosecond
                Object o = input.readObject();
                if (o instanceof SendCard) {
                    SendCard sc = (SendCard) o;
                    sendMessage(sc);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                break;   // Don't remember if this gets here when nothing has been received. If it does this catch statement should be empty
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send the text message over sms
     * @param sc The Object containing the
     *           information necessary to send the message.
     */
    public void sendMessage(SendCard sc) {
        String number = sc.getNumber();
        String message = sc.getMsg();
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, null, null);  // Can only handle text messages right now. Maybe I'll add other kinds in the future.
    }


    /**
     * Checks to see if the app has been granted several permissions.
     * All permissions are necessary in order to function properly.
     */
    private void getPermissions() {   // Need to make it so the app shuts down if the user chooses not disallow any of these privileges in the future.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }

    }


    /**
     * Goes through the phones contacts so that we can get a name from a number later on.
     */
    private void parseContacts() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur.getCount() > 0) {   // Gotta make sure you have friends
            while(cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Log.i(TAG, id);
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String number = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); // Keep an eye on this. I heard it's a bit more complicated
                numToName.put(number, name);
            }
        }
    }


    /**
     * Get the associated name of the number
     * @param number  A phone number that has been received in TextReceiver
     * @return The name of the person with the associated phone number or NF (Not found)
     */
    public static String getName(String number) {
        if (numToName.containsKey(number)) {
            return numToName.get(number);
        } else {
            return "NF";   // Not Found
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {  // TODO
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  // TODO
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
    protected void onStop() {
        super.onStop();
        try {
            output.close();
            input.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
