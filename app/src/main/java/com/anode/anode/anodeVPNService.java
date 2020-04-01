package com.anode.anode;

import android.content.Context;
import android.net.VpnService;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class anodeVPNService extends VpnService {

    private static final String TAG = "CJDROUTE";
    static final String FILENAME_CJDROUTE = "cjdroute";
    private static final String CMD_EXECUTE_CJDROUTE = "%1$s/" + FILENAME_CJDROUTE + " < %2$s/android_cjdroute.conf";
    private static final String CMD_ADD_DEFAULT_ROUTE = "ip -6 route add default via fc00::1 dev tun0 metric 4096";
    private static final String CJDROUTE_FILES_PATH = "/data/data/com.anode.anode/files";
    private Context mContext;

    anodeVPNService(Context context) {
        mContext = context;
    }

    public void LaunchCJDNS() {
        DataOutputStream os = null;
        try {
            Process process = Runtime.getRuntime().exec("pwd");
            //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // Execute cjdroute.
            //String filesDir = mContext.getFilesDir().getPath();
            os = new DataOutputStream(process.getOutputStream());
            //os.writeBytes(String.format(CMD_EXECUTE_CJDROUTE, filesDir, filesDir));
            os.writeBytes(String.format(CMD_EXECUTE_CJDROUTE, CJDROUTE_FILES_PATH, CJDROUTE_FILES_PATH));
            os.writeBytes("\n");
            os.writeBytes(CMD_ADD_DEFAULT_ROUTE);
            os.flush();
        } catch (IOException  e) {
            Log.e(TAG, "Failed to execute cjdroute", e);
        } finally {

        }
    }
}

