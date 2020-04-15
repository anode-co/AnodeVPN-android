package com.anode.anode;

import android.content.Context;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.renderscript.ScriptGroup;
import android.system.OsConstants;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class anodeVPNService extends VpnService {
    private static final String LOGTAG = "anodeVPNService";
    private static final String CJDNS_PATH = "/data/data/com.anode.anode/files";
    private static final String CJDROUTE_SOCK = "cjdroute.sock";
    private static final String CJDROUTE_LOG = "cjdroute.log";

    private static final BobjTools bobj = new BobjTools();

    private String cjdrouteBinFile = "cjdroute";
    private String cjdrouteConfFile = "cjdroute.conf";
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
    MainActivity main = new MainActivity();

    private static String read(LocalSocket ls) throws Exception {
        Log.i(LOGTAG, "Reading time");
        InputStream is = ls.getInputStream();
        int av;
        do {
            av = is.available();
            Thread.sleep(50);
        } while (av < 1);
        Log.i(LOGTAG, "Available " + av);
        byte[] b = new byte[av];
        ls.getInputStream().read(b);
        return new String(b);
    }

    private static int importFd(LocalSocket ls, FileDescriptor fd) throws Exception {
        FileDescriptor fda[] = {fd};
        ls.setFileDescriptorsForSend(fda);
        ls.getOutputStream().write("d1:q14:Admin_importFde".getBytes("UTF-8"));
        String str = read(ls);
        Log.i(LOGTAG, "Got back: " + str);
        return 0;
    }

    private static FileDescriptor exportFd(LocalSocket ls, int fd) throws Exception {
        byte[] d = bobj.dict(
            "q", "Admin_exportFd",
                "args", bobj.dict(
                    "fd", fd
                )
        ).bytes();
        ls.getOutputStream().write(d);
        Log.i(LOGTAG, "Got back: " + read(ls));
        FileDescriptor[] fds = ls.getAncillaryFileDescriptors();
        if (fds.length < 1) { throw new Exception("Did not read back file descriptor"); }
        return fds[0];
    }

    private static int getUdpFd(LocalSocket ls, int ifNum) throws Exception {
        Log.i(LOGTAG, "getUdpFd");
        byte[] b = bobj.dict(
            "q", "UDPInterface_getFd",
            "args", bobj.dict(
                "interfaceNumber", ifNum
            )
        ).bytes();
        ls.getOutputStream().write(b);
        String s = read(ls);
        Log.i(LOGTAG, "Got back: " + s);
        Object dec = new Benc(s).decode();
        Log.i(LOGTAG, "Decoded: " + dec);
        return 0;
    }

    private static void runnerThread() throws Exception {
        LocalSocket ls = new LocalSocket();
        for (int tries = 0; ; tries++) {
            String socketName = ( CJDNS_PATH + "/" + CJDROUTE_SOCK);
            try {
                Log.i(LOGTAG,"Connecting to socket...");
                ls.connect(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
                ls.setSendBufferSize(1024);
            } catch (Exception e) {
//                Log.e(LOGTAG, "Failed to connect to cjdns, trying again in 200ms",e);
                if (tries > 100) {
                    throw new RuntimeException("Unable to establish socket to cjdns");
                }
            }
            if (ls.isConnected()) { break; }
            try { Thread.sleep(200); } catch (Exception e) { }
        }
        getUdpFd(ls,0);



//        //Configure the TUN and get the interface
//        mInterface = builder.setSession("anodeVPNService")
//                .allowFamily(OsConstants.AF_INET)
//                .addAddress(cjdnsIPv6Address, 128)
//                .addRoute("fc00::", 8)
//                .establish();
//
//
//        FileDescriptor fda[] = {mInterface.getFileDescriptor()};
//
//        localsocket.setFileDescriptorsForSend(fda);
//        OutputStream os = localsocket.getOutputStream();
//        InputStream is = localsocket.getInputStream();
//        os.write("d1:q14:Admin_importFde".getBytes("UTF-8"));
//
//        is.available()
//        //os.flush();
//        main.showMessageSnackbar("CJDNS running...");
//        while(true)
//        {
//            Thread.sleep(500);
//        }
    }

    // Services interface
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String,String> entry : env.entrySet()) {
            Log.i(LOGTAG, entry.getKey() + " = " + entry.getValue());
        }
        File confFile = new File(CJDNS_PATH + "/" + cjdrouteConfFile);
        version();
        if (confFile.exists() && confFile.length() > 0) {
            cjdnsIPv6Address = getIPv6Address();
        } else {
            Log.i(LOGTAG,"Trying to create new cjdroute.conf file...");
            Genconf();
            //Addpeers();
            cjdnsIPv6Address = getIPv6Address();
            if (cjdnsIPv6Address == "") {
                throw new RuntimeException("Unable to get cjdns ipv6 address from conf file");
            }
        }
        Log.i(LOGTAG,"Got ipv6 address: " + cjdnsIPv6Address);
        //Launch cjdroute with configuration file
        LaunchCJDNS();

        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnerThread();
                } catch (Exception e) {
                    if (mInterface != null) {
                        try { mInterface.close(); } catch (Exception ee) { }
                        mInterface = null;
                    }
                    e.printStackTrace();
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

    public void version() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            Process p = processBuilder.command(CJDNS_PATH + "/" + cjdrouteBinFile, "--version")
                    .redirectErrorStream(true)
                    .start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                Log.e(LOGTAG, "cjdroute: " + line);
            }
            p.waitFor();
        } catch (Exception e)
        {
            Log.e(LOGTAG,"Failed to generate new configuration file", e);
            e.printStackTrace();
        }
    }

    public void Genconf() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            processBuilder.command(CJDNS_PATH + "/" + cjdrouteBinFile, "--genconf")
                    .redirectOutput(new File(CJDNS_PATH, cjdrouteConfFile))
                    .start().waitFor();
        } catch (Exception e)
        {
            Log.e(LOGTAG,"Failed to generate new configuration file", e);
            e.printStackTrace();
        }

    }

    public void LaunchCJDNS() {
        DataOutputStream os = null;
        try {
            Log.e(LOGTAG, "Launching cjdroute (file size: " +
                    new File(CJDNS_PATH + "/" + cjdrouteBinFile).length() + ")");
            ProcessBuilder processBuilder = new ProcessBuilder();

            //Run cjdroute with existing conf file
            ProcessBuilder pb = processBuilder.command(CJDNS_PATH + "/" + cjdrouteBinFile)
                    .redirectInput(new File(CJDNS_PATH, cjdrouteConfFile))
                    .redirectOutput(new File(CJDNS_PATH, CJDROUTE_LOG))
                    .redirectErrorStream(true);
            pb.environment().put("TMPDIR", CJDNS_PATH);
            Process p = processBuilder.start();
            p.waitFor();
            Log.e(LOGTAG, "cjdns exited with " + p.exitValue());
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to execute cjdroute", e);
        } finally {

        }
    }

    public String getIPv6Address() {
        Log.i(LOGTAG,"Trying to read IPv6 address from cjdroute.conf file...");
        String address = "";
        //Read cjdroute conf file to extract the address
        File confFile = new File(CJDNS_PATH + "/" + cjdrouteConfFile);
        if (!confFile.exists()) {
            throw new RuntimeException("Unable to find cjdroute.conf");
        }
        Log.i(LOGTAG,"File size is: " + confFile.length());
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
            throw new RuntimeException("Could not find cjdns address in file");
        } catch (IOException e) {
            Log.e(LOGTAG,"Can not read address: ",e);
            e.printStackTrace();
            throw new RuntimeException("Unable to read cjdroute.conf " + e.getMessage());
        }
    }

    public void Addpeers(){
        Log.i(LOGTAG,"Adding peers to cjdroute.conf file...");
        File confFile = new File(CJDNS_PATH + "/" + cjdrouteConfFile);
        File newconfFile = new File(CJDNS_PATH + "/" + cjdrouteConfFile+".new");
        if (confFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(newconfFile));
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                    int idx = line.indexOf("// Ask somebody who is already connected." );
                    if ( idx > -1) {
                        bw.write(cjdnsPeers);
                        bw.write("\n");
                    }
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

            //TODO: delete original file and mv .conf.new to .conf
        }
    }
}

