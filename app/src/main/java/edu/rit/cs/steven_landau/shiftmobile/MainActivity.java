package edu.rit.cs.steven_landau.shiftmobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static Socket server;
    public static ObjectInputStream input;
    public static ObjectOutputStream output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        fab.hide(); // Don't know why I can't permanently delete it.

        getPermissions();

        try {
            server = new Socket("localhost", 8012);
            output = new ObjectOutputStream(server.getOutputStream());
            output.writeObject(new Mobile());   // Let the server know we are a mobile device
            output.flush();
            input = new ObjectInputStream(server.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks to see if the app has been granted several permissions.
     * All permissions are necessary in order to function properly.
     */
    private void getPermissions() {
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
