package com.anode.anode;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.dampcake.bencode.*;

public class AdminApi {
    private static String LOGTAG = "AdminApi";

    public static final int TIMEOUT = 5000;
    public static final int DGRAM_LENGTH = 4096;

    private InetAddress address;
    private int port;
    private byte[] password;


    static AdminApi from(String cjdrouteConf) throws IOException {
        String adr = "127.0.0.1";
        String pwd = "";
        int port = 11234;
        String bind = "127.0.0.1:11234";
        //Read cjdroute conf file to extract the address
        File confFile = new File(cjdrouteConf);
        if (confFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFile));
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf("\"password\":");
                    if (idx > -1) {
                        pwd = line.substring(idx + new String("\"password\":").length());
                    }
                    //idx = line.indexOf("\"bind\":");
                }
                br.close();
            } catch (IOException e) {
                Log.e(LOGTAG,"Can not read address: ",e);
                e.printStackTrace();
            }
        }

        InetAddress address = InetAddress.getByName(adr);
        byte[] password = pwd.getBytes();

        return new AdminApi(address, port, password);
    }

    private AdminApi(InetAddress address, int port, byte[] password) {
        this.address = address;
        this.port = port;
        this.password = password;
    }

    protected byte[] serialize(Map request) throws IOException {
        byte data[];
        Bencode serializer = new Bencode();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        data = serializer.encode(request);
        return data;
    }

    protected Map parse(byte[] data) throws IOException {
        Bencode parser = new Bencode();
        return (Map) parser.decode(data,Type.DICTIONARY);
    }

    protected DatagramSocket newAdminSocket() throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);
        return socket;
    }

    public String getBind() {
        return this.address.getHostAddress() + ":" + this.port;
    }

    public String getPeers(String path, int timeout, String nearbyPath) {
        //RouterModule_getPeers(path, nearbyPath=0, timeout='')
        //List<String> peers = new ArrayList<String>();
        HashMap<ByteBuffer, Object> request = new HashMap<>();

        //request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("RouterModule_getPeers".getBytes()),ByteBuffer.wrap("args"));
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("RouterModule_getPeers".getBytes()));

        DatagramSocket adminsocket = null;
        try {
            adminsocket = new DatagramSocket();
            adminsocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            Log.e(LOGTAG,"Failed to create socket", e);
            return null;
        }

        Bencode serializer = new Bencode();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte encoded[] = serializer.encode(request);
        DatagramPacket response = null;
        DatagramPacket dgram = new DatagramPacket(encoded, encoded.length, address, port);
        try {
            adminsocket.send(dgram);

            response = new DatagramPacket(new byte[DGRAM_LENGTH], DGRAM_LENGTH);
            adminsocket.receive(response);
            adminsocket.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to send command to Admin Api", e);
            e.printStackTrace();
        }
        Bencode parser = new Bencode();
        Map peers = parser.decode(response.getData(),Type.DICTIONARY);

        return peers.toString();
    }

    public boolean ping() {

        HashMap<ByteBuffer, Object> request = new HashMap<>();
        //Ping command
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("ping".getBytes()));

        try {
            DatagramSocket adminsocket = newAdminSocket();
            byte data[] = serialize(request);
            DatagramPacket dgram = new DatagramPacket(data, data.length, address, port);
            adminsocket.send(dgram);

            DatagramPacket responseDgram = new DatagramPacket(new byte[DGRAM_LENGTH], DGRAM_LENGTH);
            adminsocket.receive(responseDgram);
            adminsocket.close();
        } catch ( IOException e) {
            e.printStackTrace();
        }
        //TODO: check response datagram
        return false;
    }


}
