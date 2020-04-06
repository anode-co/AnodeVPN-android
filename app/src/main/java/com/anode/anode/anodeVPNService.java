package com.anode.anode;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

public class anodeVPNService extends VpnService {

    private static final String TAG = "CJDROUTE";
    static final String FILENAME_CJDROUTE = "cjdroute";
    private static final String CMD_EXECUTE_CJDROUTE = "%1$s/" + FILENAME_CJDROUTE + " < %2$s/android_cjdroute.conf";
    private static final String CJDROUTE_FILES_PATH = "/data/data/com.anode.anode/files";
    private Context mContext;

    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    //a. Configure a builder for the interface.
    Builder builder = new Builder();

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LaunchCJDNS();

        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ParcelFileDescriptor iface = null;
                try {
                    //a. Configure the TUN and get the interface.
                    mInterface = builder.setSession("anodeVPNService")
                            .addAddress("fc94:2fb3:7052:f216:c993:b634:c299:2ad7", 128)
                            .addRoute("fc00::", 8)
                            .establish();
                    builder.addAllowedApplication("com.android.chrome");
                    builder.addAllowedApplication("org.mozilla.firefox");
                    //b. Packets to be sent are queued in this input stream.
                    FileInputStream in = new FileInputStream(
                            mInterface.getFileDescriptor());
                    //b. Packets received need to be written to this output stream.
                    FileOutputStream out = new FileOutputStream(
                            mInterface.getFileDescriptor());
                    //c. The UDP channel can be used to pass/get ip package to/from server
                    DatagramChannel tunnel = DatagramChannel.open();
                    //d. Protect this socket, so package send by it will not be feedback to the vpn service.
                    protect(tunnel.socket());
                    // Connect to the server
                    tunnel.connect(new InetSocketAddress("198.167.222.70",54673));

                    //e. Use a loop to pass packets.
                    while (true) {
                        //TODO: Are we doing this or cjdroute?
                    }
                } catch (Exception e) {
                    // Catch any exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (mInterface != null) {
                            mInterface.close();
                            mInterface = null;
                        }
                    } catch (Exception e) {

                    }
                }
            }

        }, "MyVpnRunnable");

        //start the service
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onDestroy();
    }

    public void LaunchCJDNS() {
        DataOutputStream os = null;
        try {
            Runtime rt = Runtime.getRuntime();
            // Execute cjdroute.
            rt.exec(String.format(CMD_EXECUTE_CJDROUTE, CJDROUTE_FILES_PATH, CJDROUTE_FILES_PATH));
            //TODO: run it on DataOutputStream and get output to check for errors
        } catch (IOException  e) {
            Log.e(TAG, "Failed to execute cjdroute", e);
        }
        finally {
            //
        }
    }


}

