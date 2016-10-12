package edu.rit.cs.steven_landau.shiftmobile;

import android.Manifest;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static Socket server;
    public static ObjectInputStream input;
    public static ObjectOutputStream output;
    public static HashMap<String, String> numToName = new HashMap<>();
    public static final String TAG = "SL";
    private CheckBox cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); // Not safe but works. I will do networking the proper way at some point.
        StrictMode.setThreadPolicy(policy);  // Overrides the default network thread.
        Log.i(TAG, "init");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.i(TAG, "getting permissions");
        getPermissions();
        Log.i(TAG, "have permissions");

    }

    @Override
    protected void onStart() {
        super.onStart();
        parseContacts();
        Log.i(TAG, "parsed contacts");
        connect();
    }

    /**
     * Connect to the server which will talk to both Clients
     */
    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "about to connect to host");

                    server = new Socket("", 8012);
                    Log.i(TAG, "Connected to host. About to open the output stream");
                    output = new ObjectOutputStream(server.getOutputStream());
                    output.writeObject(new Mobile());   // Let the server know we are a mobile device
                    output.flush();
                    Log.i(TAG, "opened the output stream. About to open the input stream");
                    input = new ObjectInputStream(server.getInputStream());
                    Log.i(TAG, "opened the input stream about to await server response for the rest of time");
                    parseContacts();
                    pingContacts();
                    onReceiveFromServer();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.i(TAG, e.getMessage());
                } catch (IOException e) {
                    Log.i(TAG, e.getMessage());
                } finally {
                    try {
                        Log.i(TAG, "closing streams");
                        output.close();
                        input.close();
                        server.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Log.i(TAG, "retrying connection");
                        while (true) {
                          try {
                              Thread.sleep(10000);
                          } catch (InterruptedException e) {
                              e.printStackTrace();
                          }
                          connect(); // Keep trying to connect to the server, every 10 seconds.
                          // This will not affect the app shutting down in anyway. Upon shutdown onDestroy is called and this method is stopped.
                      }
                    }
                }
            }
        }).start();
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
                Log.i(TAG, "found ioe exception");
                Log.i(TAG, e.getMessage());
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
    private void getPermissions() {   // TODO: Need to make it so the app shuts down if the user chooses not disallow any of these privileges in the future.
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
        Log.i(TAG, "Setting up content resolver");
        ContentResolver cr = getContentResolver();
        Log.i(TAG, "setting up cursor");
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        Log.i(TAG, "about to check fi getcount > 0");
        if (cur.getCount() > 0) {   // Gotta make sure you have friends
            Log.i(TAG, "about to go through cursor");
            while(cur.moveToNext()) {
                Log.i(TAG, "about to get the id");
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Log.i(TAG, "The id is " + id);
                Log.i(TAG, "about to get the name");
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                Log.i(TAG, "about to get the number, the name is " + name);
                //String number = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)); // Keep an eye on this. I heard it's a bit more complicated
                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID, null, null);
                String number = null;
                numToName.clear();
                while (phones.moveToNext()) {
                    int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                        number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.i(TAG, "191: " + number);
                        String realNum = "";
                        for (int i = 0; i < number.length(); i++) {
                            if (Character.isDigit(number.charAt(i))) {
                                realNum += number.charAt(i);     // Get the number into a form that can be used by our program (without the (...) ).

                            }
                        }
                        Log.i(TAG, realNum);
                        numToName.put(realNum, name);
                    }
                }


                phones.close();

            }

        }
        cur.close();
    }

    /**
     * Check to see if a new contact has been added.
     * Check every 10 minutes
     */
    private void pingContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(600000);
                        parseContacts();
                    } catch (InterruptedException e) {
                        //e.printStackTrace(); Application closed.
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * Get the associated name of the number
     * @param number  A phone number that has been received in TextReceiver
     * @return The name of the person with the associated phone number or NF (Not found)
     */
    public static String getName(String number) {
        Log.i(TAG, String.valueOf(numToName.keySet()));
        if (numToName.containsKey(number)) {
            return numToName.get(number);
        } else {
            return "NF";   // Not Found
        }
    }

    private boolean hasInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);  // keep an eye on this one
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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

    /*@Override
    protected void onStop() {
        super.onStop();
        try {
            Log.i(TAG, "in on stop");
            output.close();
            input.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.finishAffinity();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Log.i(TAG, "in on destroy");
            output.close();
            input.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.finishAffinity();
        }
    }
}
