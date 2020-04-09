package com.anode.anode;

import android.content.Context;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class anodeVPNService extends VpnService {

    private static final String TAG = "CJDROUTE";
    private static final String CMD_EXECUTE_CJDROUTE = "/data/data/com.anode.anode/cjdroute/cjdroute < /data/data/com.anode.anode/cjdroute/socket_cjdroute.conf";
    private Context mContext;

    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    //a. Configure a builder for the interface.
    Builder builder = new Builder();

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //LaunchCJDNS();
        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    prepare(getBaseContext());
                    //a. Configure the TUN and get the interface.
                    mInterface = builder.setSession("anodeVPNService")
                            .allowFamily(OsConstants.AF_INET)
                            .addAddress("fc94:2fb3:7052:f216:c993:b634:c299:2ad7", 128)
                            .addRoute("fc00::", 8)
                            .establish();

                    /*
                    DatagramChannel tunnel = DatagramChannel.open();
                    protect(tunnel.socket());
                    tunnel.connect(new InetSocketAddress("198.167.222.70",54673));
                     */

                    String socketName = (getApplication().getCacheDir().getAbsolutePath() + "/" + "cjdns.socket");

                    LocalSocket localsocket = new LocalSocket();

                    while (!localsocket.isConnected())
                    {
                        try {
                            localsocket.connect(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        Thread.sleep(500);
                    }

                    FileDescriptor fda[] = {mInterface.getFileDescriptor()};

                    localsocket.setFileDescriptorsForSend(fda);
                    OutputStream os = localsocket.getOutputStream();
                    os.write(" ".getBytes("UTF-8"));
                    //os.flush();

                    while(true)
                    {
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
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
            //Runtime rt = Runtime.getRuntime();
            Process process = Runtime.getRuntime().exec("sh");
            //Process process = rt.exec(new String[]{"/data/data/com.anode.anode/cjdroute/cjdroute", '</data/data/com.anode.anode/cjdroute/sock_cjdroute.conf'});
            //ProcessBuilder processBuilder = new ProcessBuilder("/data/data/com.anode.anode/cjdroute/cjdroute < /data/data/com.anode.anode/cjdroute/sock_cjdroute.conf");
            //Process process = processBuilder.start();
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(String.format(CMD_EXECUTE_CJDROUTE, "/data/data/com.anode.anode/cjdroute", "/data/data/com.anode.anode/cjdroute"));
            os.writeBytes("\n");
            os.flush();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));
            // Read the output from the command
            String s = null;
            /*while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }*/

            // Read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            /*
            Process process = Runtime.getRuntime().exec("sh");
            // Execute cjdroute.
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(String.format(CMD_EXECUTE_CJDROUTE, CJDROUTE_FILES_PATH, CJDROUTE_FILES_PATH));
            os.writeBytes("\n");
            os.flush();
            */

        } catch (IOException  e) {
            Log.e(TAG, "Failed to execute cjdroute", e);
        }
        finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }
    }


}

