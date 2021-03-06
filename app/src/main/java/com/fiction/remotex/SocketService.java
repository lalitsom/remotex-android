package com.fiction.remotex;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.IpSecManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class SocketService extends Service {
    public Bitmap bmpimage = null;
    public String clientname = "client";
    public String servername = "server";
    public String tryhostname = null;
    public boolean isservicerunning = false;
    public int percentdownloaded = 0;
    PrintWriter output_writer;
    InputStreamReader inputreader;
    InputStream input_stream;
    Socket socket;
    BufferedReader buffer_reader;
    public boolean downloadingfile = false;
    public boolean cleaning_stream = false;
    private int heartbeat_count = 0;
    public String Password = "fiction";
    public Boolean connection_ack_received = false;
    public DatagramSocket s = null;

    List<String> AvailableDevices = new ArrayList<String>();

    //not used as of now
    public String GetServerName() {
        return servername;
    }
    public String GetClientName() {
        return clientname;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        start_heartbeat_counting();
        recieve_heartbeat();
    }

    public void start_heartbeat_counting(){
        // check if exactly one service input_stream running  using logs.. keep counting indefinitely
        final Thread heartbeat_thread = new Thread() {
            @Override
            public void run() {
                while (true) {
//                    Log.e(this.getClass().toString(), heartbeat_count + " " + isconnected());
//                    udp_listener();
//                    udp_sender();
                    heartbeat_count++;
                    android.os.SystemClock.sleep(2000);
                }
            }
        };
        heartbeat_thread.start();
    }

    public void recieve_heartbeat(){
        final Thread recieve_heartbeat_thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    Log.e("recieve ","heart");
//                    udp_receiver();
                        udp_receiver_broadcast();
                    android.os.SystemClock.sleep(2000);
                }
            }
        };
        recieve_heartbeat_thread.start();


        final Thread connected_heatbeats_thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                   udp_sender();
                    android.os.SystemClock.sleep(2000);
                }
            }
        };
        connected_heatbeats_thread.start();

    }

//
//    public void udp_sender_broadcast(){
//        Log.e("Broadcast ","started");
//
//        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//
//        DatagramSocket ds = null;
//        String message = "android message";
//        try {
//            ds = new DatagramSocket();
//            ds.setBroadcast(true);
//            InetAddress address = getBroadcastAddress();
//            DatagramPacket dp;
//            dp = new DatagramPacket(message.getBytes(), message.getBytes().length, address, 2600);
//            ds.send(dp);
//            Log.e("upd sender ", address.getHostAddress() + " sent " + message);
//        } catch (Exception e) {
//            Log.e("upd sender ", "error : " + e.getMessage());
//            e.printStackTrace();
//        }
//        finally {
//            if(ds!=null){
//                ds.close();
//            }
//
//        }
//    }

    public void udp_receiver_broadcast(){
        DatagramSocket socket = null;
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(2601, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            socket.setSoTimeout(4000);
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                //AvailableDevices.clear();
                socket.receive(packet);
                String data = packet.getAddress().getHostAddress()+":"+ new String(packet.getData()).trim();
                if(!AvailableDevices.contains(data)){
                    AvailableDevices.add(data);
                }

                Log.e("suc", data + packet.getSocketAddress());

        } catch (Exception  e) {
            e.printStackTrace();
            Log.e("error "," w " + e.toString());
        }
        finally {
            if(socket!=null){
                socket.close();

            }
        }
    }

public List<String> getAvailableDevices(){
    return AvailableDevices;
}


//
//    InetAddress getBroadcastAddress() throws IOException {
//        WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
//        DhcpInfo dhcp = wifi.getDhcpInfo();
//        // handle null somehow
//
//        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
//        byte[] quads = new byte[4];
//        for (int k = 0; k < 4; k++)
//            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
//        return InetAddress.getByAddress(quads);
//    }


//
//
//    public void udp_receiver(){
////
////        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
////        WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
////
////        multicastLock.setReferenceCounted(true);
////        multicastLock.acquire();
////
////        MulticastSocket msocket=null;
//        DatagramSocket ds=null;
//        try {
//            int port = 2601;
//           ds = new DatagramSocket(port);
//            byte[] buffer = new byte[2048];
//            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//            ds.receive(packet);
//
//                String msg = new String(buffer, 0, packet.getLength());
//                Log.e("UDP packet received", msg + packet.getSocketAddress());
//
//                packet.setLength(buffer.length);
//
//        } catch (Exception e) {
//            Log.e("udp rec ",e.getMessage());
//        }
//        finally {
//            if(ds!=null){
//                ds.close();
//            }
//        }
//    }
//
    public void udp_sender(){
        Log.e("upd sender ","started");
        if(!isconnected()){
            return;
        }
        Log.e("upd sender ","connected");
        int port = 2600;
        int random_number = (int)(Math.random()*100);
        String message = "IamAlive" + random_number;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(1000);
            // IP Address below is the IP address of that Device where server socket is opened.
            InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
            InetAddress address = socketAddress.getAddress();
            DatagramPacket dp;
            dp = new DatagramPacket(message.getBytes(), message.length(), address, port);
            ds.send(dp);
            Log.e("upd sender ","sent " + message);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("error udp sent",e.getMessage());
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
    }


    public void recieveimage() throws IOException {

        File download_directory = new File(Environment.getExternalStorageDirectory() + "/Download/Remote Devices/temp");
        if (!download_directory.exists())
            download_directory.mkdirs();


        String output_filename = Environment.getExternalStorageDirectory() + "/Download/Remote Devices/temp/tempfile.jpg";

        File output_file = new File(output_filename);
        try {
            output_file.setWritable(true, false);
            output_file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }

        FileOutputStream output_stream = new FileOutputStream(output_file, false);
        try {
            output_stream = new FileOutputStream(output_filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }

        //transfer bytes from the inputfile to the outputfile
        int totalbytes = 0;
        byte[] buffer = new byte[1024 * 8];
        crypt_inputread(buffer, 0, 8);
        int length;
        int received_bytes = 0;

        ByteBuffer tmp_buffer = ByteBuffer.wrap(buffer); // change order to little endian
        tmp_buffer.order(ByteOrder.LITTLE_ENDIAN);
        totalbytes = (int) tmp_buffer.getLong();


        while (received_bytes < totalbytes && (length = crypt_inputread(buffer, 0, buffer.length)) > 0) {
            output_stream.write(buffer, 0, length);
            received_bytes += length;
        }

        //Close the streams
        output_stream.flush();
        output_stream.close();
        bmpimage = BitmapFactory.decodeFile(output_filename);
    }


    public void recievefile(String savename) throws IOException {

        int totalbytes = 0;
        downloadingfile = true;

        File download_directory = new File(Environment.getExternalStorageDirectory() + "/Download/Remote Devices/Downloaded");

        if (!download_directory.exists())
            download_directory.mkdirs();

        String output_filename = Environment.getExternalStorageDirectory() + "/Download/Remote Devices/Downloaded/" + savename;
        File output_file = new File(output_filename);

        try {

            output_file.setWritable(true, false);
            output_file.createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }

        byte[] buffer = new byte[1024 * 128];
        FileOutputStream output_stream = new FileOutputStream(output_file, false);
        try {
            output_stream = new FileOutputStream(output_filename);
            cleaning_stream = true;
            crypt_inputread(buffer, 0, 8);
        } catch (Exception e) {
            Log.e(this.getClass().toString(),e.getMessage());
        }

        //transfer bytes from the inputfile to the outputfile

        int length;

        int received_bytes = 0;

        ByteBuffer tmp_buffer = ByteBuffer.wrap(buffer); // temp buffer to change order to little endian
        tmp_buffer.order(ByteOrder.LITTLE_ENDIAN);
        totalbytes = (int) tmp_buffer.getLong();

        if(totalbytes==-1){
            socket.setSoTimeout(500);
            //server stopped sending file.. or was not able to send.
        }


        boolean server_timed_out = false; // did server stop sending because of time output_writer

        if (totalbytes > 0) {
            int unitpercent = totalbytes / 100;

            try {
                while (received_bytes < totalbytes && (length = crypt_inputread(buffer, 0, buffer.length)) > 0) {

                    if (!downloadingfile && server_timed_out==false) {
                        // wait until server gets time out output_writer
                        // a guess of 4 secs
                        socket.setSoTimeout(500); // stop cleaning stream if there is nothing to read.
                        server_timed_out = true;
                    }
                    output_stream.write(buffer, 0, length);
                    received_bytes += length;
                    percentdownloaded = (int) (1.0 * received_bytes / unitpercent);
                }
            } catch (Exception e) {
                Log.e(this.getClass().toString(),e.getMessage() + "waht ");
                e.printStackTrace();

            }
        }
        Log.e(this.getClass().toString(),"cleaning");
        socket.setSoTimeout(0); // set back to infinite
        if (received_bytes == totalbytes) {
            percentdownloaded = 100;
        }

        downloadingfile = false;

        output_stream.flush();
        output_stream.close();

        cleaning_stream = false;
    }

    public void sendMessage(final String message) {
        if( !message.equals("syncback") && (cleaning_stream)){
         return;
        }

        final Thread send_msg_thread = new Thread() {
            @Override
            public void run() {
                if (output_writer != null && !output_writer.checkError()) {
                    try {
                        crypt_sendmsg(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(this.getClass().toString(),e.getMessage());
                    }
                    output_writer.flush();
                } else {
                    Log.e(this.getClass().toString(),"output writer input_stream null... disconnecting");
                    check_connection();

                }
            }
        };
        send_msg_thread.start();
    }

    public String recieveMessage() {
        String plainmsg = crypt_receivemsg();
        if(valid_msg(plainmsg)){

            return plainmsg;
        }
        else{
            disconnect();
            check_connection();
        }
        return "";
    }

    public boolean valid_msg(String plainmsg){
        if(plainmsg==null) return false;
        return true;
    }

    public boolean check_connection() {
        if (!isconnected()) {
            disconnect();
            show_connect_activity();
            return false;
        }
        return true;
    }

    public void show_connect_activity(){
        Intent dialogIntent = new Intent(this, Connect.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Add the bundle to the intent
        dialogIntent.putExtra("toast_msg","Disconnected!");
        this.startActivity(dialogIntent);
    }

    public void show_mainmenu_activity(){
        Intent dialogIntent = new Intent(this, MainMenu.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(dialogIntent);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void connect(String hostname) {
        if (socket == null) {
            connection_ack_received = false;
            tryhostname = hostname;
            Runnable connect = new connectSocket();
            new Thread(connect).start();
        }
    }

    public boolean isconnected() {
        try {
            if (socket == null || !socket.isConnected() ||
                    output_writer == null || output_writer.checkError() || !connection_ack_received){
                return false;
            }
            else
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
            return false;
        }
    }


    public void disconnect() {
        try {
            socket.shutdownInput();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }
        try {
            socket.shutdownOutput();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }
        socket = null;
    }



    class connectSocket implements Runnable {
        @Override
        public void run() {
            int port = 2055;
            try {
                try {
                    socket = new Socket(tryhostname, port);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(this.getClass().toString(),e.getMessage());
                    tryhostname = null;
                    return;
                }

                InetSocketAddress localSocketAddress = (InetSocketAddress) socket.getLocalSocketAddress();
                clientname = localSocketAddress.getHostName();

                InetSocketAddress serverAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                servername = serverAddress.getHostName();

                try {
                    //send the message to the server
                    output_writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    buffer_reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    input_stream = socket.getInputStream();
                    inputreader = new InputStreamReader(socket.getInputStream());

                    wait_while_connecting();

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(this.getClass().toString(),e.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(this.getClass().toString(),e.getMessage());
            }
        }
    }

    public void wait_while_connecting(){
        // check if exactly one service input_stream running  using logs.. keep counting indefinitely
        final Thread check_conn_success = new Thread() {
            @Override
            public void run() {
                int times = 3;
                while (times>0) {
                    times-=1;
                    if(output_writer!=null && socket.isConnected()){
                        get_connection_ack();
                        Log.e("time ",""+times);
                        break;
                    }
                    android.os.SystemClock.sleep(100);
                }
            }
        };
        check_conn_success.start();
    }


    public void get_connection_ack(){
        sendMessage("connected");
        String msg = recieveMessage();
        Log.e("whatistha ",msg);
        if(msg.equals("connected")){
            connection_ack_received = true;
            show_mainmenu_activity();
        }
        else{
            check_connection();
        }

    }



//    final send messages wrapper

    public void crypt_sendmsg(String msg){
        Log.e("sentmessage ", msg);
        String Encrypted_msg =  Encryption.encrypt(msg,Password);
        Log.e("sentcryptmessage ", Encrypted_msg);
        String Dmsg = Encryption.decrypt(Encrypted_msg,Password);
        Log.e("sentcry sanity ", Dmsg);
        output_writer.println(Encrypted_msg);
    }



    public String crypt_receivemsg(){
        String Decrypted_msg = null;
        try {
            String msg = buffer_reader.readLine();
            Log.e("recievedmessage ", msg);
            Decrypted_msg =  Encryption.decrypt(msg,Password);
            Log.e("rec_decdmessage ", Decrypted_msg);
        }catch (Exception e){

        }
        return Decrypted_msg;
    }

    public int crypt_inputread(byte[] buffer, int offset, int length) throws IOException{
        int len=0;
        try {
            len = input_stream.read(buffer, offset, length);
        }catch (Exception e){
            throw e;
        }
        return len;
    }

    @Override
    public void onDestroy() {
        try {
            disconnect();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }
        socket = null;
        try {
            // please DIE DIE DIE.....
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().toString(),e.getMessage());
        }
        super.onDestroy();
    }
}
