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
import android.util.JsonReader;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class anodeVPNService extends VpnService {
    private String cjdroutePath = "/data/data/com.anode.anode/cjdroute/";
    private String cjdrouteBinFile = "cjdroute";
    private String cjdrouteConfFile = "cjdroute.conf";
    private String cjdrouteLogFile = "cjdroute.log";
    private String cjdrouteSocket = "cjdns.socket";
    private String cjdnsIPv6Address;
    private Context mContext;
    //Thread for VPNService
    private Thread mThread;
    //Thread for launching cjdroute
    private Thread cjdrouteThread;
    //FD for the VPNService.builder
    private ParcelFileDescriptor mInterface;
    //a. Configure a builder for the interface.
    Builder builder = new Builder();

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cjdroutePath = getApplication().getFilesDir().getAbsolutePath();
        //Read cjdroute conf file to extract the address
        File confFile = new File(cjdroutePath + "/" + cjdrouteConfFile);
        if (confFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFile));
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf("\"ipv6\":" );
                    if ( idx > -1) {
                        cjdnsIPv6Address = line.substring(idx+8).replace("\"","").replace(",","");
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //TODO: create conf using cjdroute --genconf

        }
        LaunchCJDNS();
        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    prepare(getBaseContext());
                    //Configure the TUN and get the interface.
                    mInterface = builder.setSession("anodeVPNService")
                            .allowFamily(OsConstants.AF_INET)
                            //.addAddress("fc94:2fb3:7052:f216:c993:b634:c299:2ad7", 128)
                            .addAddress(cjdnsIPv6Address, 128)
                            .addRoute("fc00::", 8)
                            .establish();

                    String socketName = ( cjdroutePath + "/" + cjdrouteSocket);
                    LocalSocket localsocket = new LocalSocket();

                    //Try a few times to connect to the socket that is created from cjdroute
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
        if (cjdrouteThread != null) {
            cjdrouteThread.interrupt();
        }
        super.onDestroy();
    }

    public void LaunchCJDNS() {
        cjdrouteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream os = null;
                try {
                    /**/
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    processBuilder.command("whoami")
                            .redirectOutput(new File("/data/data/com.anode.anode/cjdroute/", "process.log"));
                    /**/
                    processBuilder.command("/data/data/com.anode.anode/cjdroute/cjdroute")
                            .redirectInput(new File("/data/data/com.anode.anode/cjdroute/", "socket_cjdroute.conf"))
                            .redirectOutput(new File("/data/data/com.anode.anode/cjdroute/", "cjdroute.log"));
                    /**/
                    Process p = processBuilder.start();
                    p.waitFor();
                    /**/
                    /*
                    Process p=Runtime.getRuntime().exec("sh");
                    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                    dos.writeBytes("/data/data/com.anode.anode/cjdroute/cjdroute < /data/data/com.anode.anode/cjdroute/socket_cjdroute.conf\n");
                    dos.flush();
                    dos.close();
                    p.waitFor();
                    */
                } catch (IOException | InterruptedException e) {
                    Log.e("LAUNCH_CJDROUTE", "Failed to execute cjdroute", e);
                } finally {

                }
            }
        }, "cjdrouteThread");
        //start the service
        cjdrouteThread.start();
    }

}

