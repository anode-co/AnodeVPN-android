package com.anode.anode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.VpnService;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static String LOGTAG = "MainActivity";
    private FloatingActionButton button_start_test;
    private Button button_get_peers;
    SharedPreferences prefs = null;
    AnodeUtil util;

    protected void InitializeApp(){
        //Create files folder
        getApplication().getFilesDir().mkdir();
        //Copy cjdroute
        InputStream in = null;
        OutputStream out = null;
        try {
            AssetManager am = getBaseContext().getAssets();
            //Read architecture
            String arch = System.getProperty("os.arch");
            Log.i(LOGTAG,"OS Architecture: "+arch);
            if (arch.contains("x86") || arch.contains("i686"))
            {
                in = am.open("x86/cjdroute");
            } else if (arch.contains("arm64-v8a")){
                in = am.open("arm64-v8a/cjdroute");
            } else if (arch.contains("armeabi")) {
                in = am.open("armeabi-v7a/cjdroute");
            } else {
                //Unknown architecture
                Log.i(LOGTAG,"Unknown CPU architecture");
                return;
            }

            out = new FileOutputStream(getApplication().getFilesDir()+"/cjdroute");
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1 ){
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
            //Set permissions
            File file = new File(getApplication().getFilesDir()+"/cjdroute");
            file.setExecutable(true);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOGTAG, "Failed to copy cjdroute file", e);
        }

        //Create conf
        util.Genconf();

        //Modify default conf file
        //cjdns.ModifyConfFile();
        util.ModifyJSONConfFile();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        util = new AnodeUtil();
        prefs = getSharedPreferences("com.anode.anode", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            InitializeApp();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

        button_start_test = findViewById(R.id.button_start_test);
        button_get_peers = findViewById(R.id.button_getPeers);
        //DEBUG
        //cjdns.ModifyJSONConfFile();
        button_start_test.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Launching cjdroute...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent intent = VpnService.prepare(getApplicationContext());
                if (intent != null) {
                    startActivityForResult(intent, 0);
                } else {
                    onActivityResult(0, RESULT_OK, null);
                }
            }
        });

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, AnodeVpnService.class);
            startService(intent);
        }
    }

    public void showMessageSnackbar(String msg)
    {
        View view = findViewById(button_start_test.getId());
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

}
