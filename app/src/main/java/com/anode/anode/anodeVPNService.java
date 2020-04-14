package com.anode.anode;

import android.content.Context;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.nio.file.Files;
import java.lang.Process;
import java.util.Iterator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class anodeVPNService extends VpnService {
    private static String LOGTAG = "anodeVPNService";
    private String cjdroutePath = "/data/data/com.anode.anode/files";
    private String cjdrouteBinFile = "cjdroute";
    private String cjdrouteTmpConfFile = "tempcjdroute.conf";
    private String cjdrouteConfFile = "cjdroute.conf";
    private String cjdrouteLogFile = "cjdroute.log";
    private String cjdrouteSocket = "cjdns.socket";
    private static String cjdnsPeers = "\"94.23.31.145:17102\":{\"login\":\"cjd-snode\",\"password\":\"wwbn34yhxhtubtghq6y2pksyt7c9mm8\",\"publicKey\":\"9syly12vuwr1jh5qpktmjc817y38bc9ytsvs8d5qwcnvn6c2lwq0.k\"},"+
            "\"198.167.222.70:54673\":{\"login\":\"ipredator.se/cjdns_public_node\",\"password\":\"use_more_bandwidth\",\"publicKey\":\"cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k\"}";
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

    private boolean cjdrouteRunning = false;
    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        File confFile = new File(cjdroutePath + "/" + cjdrouteConfFile);
        if (confFile.exists()) {
            cjdnsIPv6Address = getIPv6Address();
        } else {
            Log.i(LOGTAG,"Trying to create new cjdroute.conf file...");
            Genconf();
            ModifyConfFile();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cjdnsIPv6Address = getIPv6Address();
            if (cjdnsIPv6Address == "") {
                //TODO:handle error creating conf file...
                return START_STICKY;
            }
        }
        //Launch cjdroute with configuration file
        LaunchCJDNS();

        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
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
                    int tries = 0;
                    while (!localsocket.isConnected() || tries < 5) {
                        try {
                            localsocket.connect(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
                            break;
                        }
                        catch (Exception e) {
                            Log.e(LOGTAG, "Failed to connect to "+socketName,e);
                            e.printStackTrace();
                        }
                        /*
                        if (!isCjdrouteRunning()) {
                            LaunchCJDNS();
                        } else {
                            //TODO: cjdroute is running but can not connect socket?! What to do?
                            //restart it?
                        }

                         */
                        tries++;
                    }
                    //TODO: log error of failing to launch cjdns and return

                    FileDescriptor fda[] = {mInterface.getFileDescriptor()};

                    localsocket.setFileDescriptorsForSend(fda);
                    OutputStream os = localsocket.getOutputStream();
                    os.write(" ".getBytes("UTF-8"));
                    //os.flush();

                    while(true) {
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Log.e(LOGTAG,"Failed to connect to cjdroute...", e);
                    //TODO: Handle bad address...
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


    public void Genconf() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            processBuilder.command(cjdroutePath + "/" + cjdrouteBinFile, "--genconf")
                    .redirectOutput(new File(cjdroutePath, cjdrouteTmpConfFile))
                    .start();
            Thread.sleep(300);
            //Clean conf
            processBuilder.command(cjdroutePath + "/" + cjdrouteBinFile, "--cleanconf")
                    .redirectInput(new File(cjdroutePath, cjdrouteTmpConfFile))
                    .redirectOutput(new File(cjdroutePath, cjdrouteConfFile))
                    .start();
        } catch (InterruptedException | IOException e)
        {
            Log.e(LOGTAG,"Failed to generate new configuration file", e);
            e.printStackTrace();
        }
        try {
            Thread.sleep(300);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        //TODO: Delete cjdrouteTmpConfFile
    }

    public void LaunchCJDNS() {
        cjdrouteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream os = null;
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder();

                    //Run cjdroute with existing conf file
                    processBuilder.command(cjdroutePath + "/" + cjdrouteBinFile)
                            .redirectInput(new File(cjdroutePath, cjdrouteConfFile))
                            .redirectOutput(new File(cjdroutePath, cjdrouteLogFile))
                            .redirectErrorStream(true);

                    Process p = processBuilder.start();
                    //p.waitFor();
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to execute cjdroute", e);
                    cjdrouteRunning = false;
                } finally {
                    cjdrouteRunning = true;
                }
            }
        }, "cjdrouteThread");
        //start the service
        cjdrouteThread.start();
    }

    public String getIPv6Address() {
        Log.i(LOGTAG,"Trying to read IPv6 address from cjdroute.conf file...");
        String address = "";
        //Read cjdroute conf file to extract the address
        File confFile = new File(cjdroutePath + "/" + cjdrouteConfFile);
        if (confFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFile));
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf("\"ipv6\":");
                    if (idx > -1) {
                        address = line.substring(idx + 8).replace("\"", "").replace(",", "");
                        br.close();
                        Log.i(LOGTAG,"Read IPv6 address: "+address);
                        return address;
                    }
                }
                br.close();
                return address;
            } catch (IOException e) {
                Log.e(LOGTAG,"Can not read address: ",e);
                e.printStackTrace();
            }
        }
        return "";
    }

    public boolean isCjdrouteRunning() {
        /*
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("pgrep cjdroute");

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            if (bufferedReader.readLine() != null) {
                cjdrouteRunning = true;
                return true;
            } else {
                cjdrouteRunning = false;
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
         */
        return true;
    }

    public void ModifyJSONConfFile() {
        try {
            FileInputStream confFile = new FileInputStream(cjdroutePath + "/" + cjdrouteConfFile);
            StringBuffer fileContent = new StringBuffer("");
            byte[] buffer = new byte[1024];
            int size;
            while ((size = confFile.read(buffer)) != -1)
            {
                fileContent.append(new String(buffer, 0, size));
            }
            JSONObject json = new JSONObject(fileContent.toString());
            JSONObject interfaces = json.getJSONObject("interfaces");
            JSONArray UDPInterface = interfaces.getJSONArray("UDPInterface");

            //Add Peers
            JSONObject connectTo = UDPInterface.getJSONObject(0).getJSONObject("connectTo");
            JSONObject peer = new JSONObject();
            JSONObject peervalues = new JSONObject();
            peervalues.put("login", "cjd-snode");
            peervalues.put("password", "wwbn34yhxhtubtghq6y2pksyt7c9mm8");
            peervalues.put("publicKey", "9syly12vuwr1jh5qpktmjc817y38bc9ytsvs8d5qwcnvn6c2lwq0.k");
            peer.put("94.23.31.145:17102",peervalues);
            peervalues.put("login", "ipredator.se/cjdns_public_node");
            peervalues.put("password", "use_more_bandwidth");
            peervalues.put("publicKey", "cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k");
            peer.put("198.167.222.70:54673", peervalues);
            connectTo.put("connectTo", peer);

            //Add tunfd and tunnel socket
            JSONObject router = json.getJSONObject("router");
            JSONObject interf = router.getJSONObject("interface");
            interf.put("tunfd","android");
            interf.put("tunDevice",cjdroutePath+"/"+cjdrouteSocket);

            //Set security user to 0
            JSONArray security = json.getJSONArray("security");
            security.getJSONObject(0).put("setuser",0);
            security.getJSONObject(0).remove("keepNetAdmin");

            //Save file
            BufferedWriter writer = new BufferedWriter(new FileWriter(cjdroutePath + "/" + cjdrouteConfFile));
            writer.write(json.toString());
            writer.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        //File confFile = new File(cjdroutePath + "/" + cjdrouteConfFile);

    }

    public void ModifyConfFile() {
        Log.i(LOGTAG,"Modifying cjdroute.conf file...");
        File confFile = new File(cjdroutePath + "/" + cjdrouteConfFile);
        File newconfFile = new File(cjdroutePath + "/" + cjdrouteConfFile+".new");
        if (confFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(newconfFile));
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf("{ \"setuser\": \"nobody\", \"keepNetAdmin\": 1 },");
                    if (idx > -1) { //Comment out setuser line and set it to 0
                        bw.write("//"+line);
                        bw.newLine();
                        bw.write("{ \"setuser\": 0 },");
                        bw.newLine();
                    } else {//Copy all other lines
                        bw.write(line);
                        bw.newLine();
                    }
                    //Add peers
                    idx = line.indexOf("// Ask somebody who is already connected." );
                    if ( idx > -1) {
                        bw.write(cjdnsPeers);
                        bw.write("\n");
                    }
                    //Add pipe path
                    idx = line.indexOf("// \"pipe\": \"/data/local/tmp\"" );
                    if ( idx > -1) {
                        bw.write(line.replace("// \"pipe\": \"/data/local/tmp\""," \"pipe\": \""+cjdroutePath+"\","));
                    }
                    //TODO: Change tunfd and add socket to tundevice
                }
                br.close();
                bw.close();
            } catch (IOException e) {
                Log.e(LOGTAG,"Failed to add peers to cjdroute.conf.new file: ", e);
                e.printStackTrace();
            }
            try {
                Files.move(newconfFile.toPath(),confFile.toPath(),REPLACE_EXISTING);
            } catch (IOException e)
            {
                Log.e(LOGTAG,"Failed to copy new cjdroute.conf file: ", e);
                e.printStackTrace();
            }
        }
    }
}

