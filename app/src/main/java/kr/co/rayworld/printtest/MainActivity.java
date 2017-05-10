package kr.co.rayworld.printtest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.Pattern;

import kr.co.rayworld.printtest.printer.HS_Printer;

public class MainActivity extends AppCompatActivity {
    private static final byte CMD_DLE = 16;
    private static final byte CMD_ESC = 27;
    private static final byte CMD_FS = 28;
    private static final byte CMD_GS = 29;
    private static final String HWASUNG_VENDOR_ID = ".*mVendorId=6.*";
    private static final String TAG = "UsbMenuActivity";
    private UsbDeviceConnection mConnection;
    private UsbDevice mDevice;
    private UsbEndpoint mEndpointBulk;
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        UsbManager mUsbManager =(UsbManager) getSystemService(Context.USB_SERVICE);


        //init();

        //setDevice();
        //printTest("   Gilgamesh : Dual Wield\n   Trans Tina, Orlando\n");
        //HS_Printer hp = new HS_Printer(this);
        //hp.printString("   Gilgamesh : Dual Wield\n   Trans Tina, Orlando\n");
        //new TCP_send().execute("   Gilgamesh : Dual Wield\n   Trans Tina, Orlando\n");
        //new TCP_send2().execute("   Gilgamesh : Dual Wield\n   아나 속 안좋네\n");
        HS_Printer hp = new HS_Printer(this,HS_Printer.NETWORK_MODE,"192.168.0.253",9100);
        hp.printString("   Gilgamesh : Dual Wield\n   아나 속 안좋네\n");
    }

    private void init() {
        this.mUsbManager = ((UsbManager) getSystemService(USB_SERVICE));
    }

    class TCP_send extends AsyncTask<String, String, String>{
        String serverAddr = "192.168.0.253";
        int strPort = 9100;
        @Override
        protected String doInBackground(String... strings) {
            try {
                Socket socket = new Socket(serverAddr, strPort);
                OutputStream out = socket.getOutputStream();

                if(strings[0]!=null) {
                    String msg = strings[0];
                    byte[] msgArr = msg.getBytes();
                    out.write(msgArr);
                }

                byte[] msgArray;

                // feeding
                msgArray = new byte[5];
                msgArray[0] = 0xA;
                msgArray[1] = 0xA;
                msgArray[2] = 0xA;
                msgArray[3] = 0xA;
                msgArray[4] = 0xA;
                out.write(msgArray);

                // full cutting
                msgArray = new byte[2];
                msgArray[0] = CMD_ESC;
                msgArray[1] = 0x69;
                out.write(msgArray);





            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";

        }
    }

    class TCP_send2 extends AsyncTask<String, String, String>{
        String serverAddr = "192.168.0.253";
        int strPort = 9100;
        @Override
        protected String doInBackground(String... strings) {
            try {
                Socket socket = new Socket(serverAddr, strPort);
                OutputStream out = socket.getOutputStream();

                if(strings[0]!=null) {
                    String msg = strings[0];
                    byte[] msgArr = msg.getBytes(Charset.forName("EUC-KR"));
                    out.write(msgArr);
                }

                byte[] msgArray;

                // feeding
                msgArray = new byte[5];
                msgArray[0] = 0xA;
                msgArray[1] = 0xA;
                msgArray[2] = 0xA;
                msgArray[3] = 0xA;
                msgArray[4] = 0xA;
                out.write(msgArray);

                // full cutting
                msgArray = new byte[2];
                msgArray[0] = CMD_ESC;
                msgArray[1] = 0x69;
                out.write(msgArray);





            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";

        }
    }




    private void printTest(String str){
        String msg = "   Hwasung System\n   Unit 604 Bldg B Digital Empire,\n   Kwanyang 906-4,Dongan,Anyang City,\n   Gyeonggi-Do 431-060,S.Korea\n                  TEL.82-31-8086-7550\n                  FAX.82-31-8086-7555\n\n  Price       Q'ty       Item\n------------------------------------\n  EUR1.50       1        Coke\n  EUR2.99       1        T-Shirts\n  EUR6.50      24        Corn Chips\n  EUR2.68       3        KFC\n  EUR3.66       4        Button\n-------------------------------------\n";
        msg = str;
        int msgLength =msg.length();
        int[] msgArray = new int[msgLength];
        for(int i =0 ;i <msg.length();i++){
            msgArray[i] =  msg.getBytes()[i];
        }

        sendCommand(this.mDevice, msgArray, msgLength);


        msgArray[0] = 0;
        msgArray[1] = 10;
        sendCommand(this.mDevice, msgArray, 2);
        msgArray[0] = 27;
        msgArray[1] = 97;
        msgArray[2] = 0;
        sendCommand(this.mDevice, msgArray, 3);
        msgArray[0] = 10;
        msgArray[1] = 10;
        msgArray[2] = 10;
        msgArray[3] = 10;
        msgArray[4] = 10;
        sendCommand(this.mDevice, msgArray, 5);
        msgArray[0] = 27;
        msgArray[1] = 105;
        sendCommand(this.mDevice, msgArray, 2);
    }

    private void sendCommand(UsbDevice paramUsbDevice, int[] parammsgArray, int paramInt)
    {
        UsbEndpoint localUsbEndpoint = null;
        byte[] arrayOfByte = new byte[paramInt];
        if (this.mConnection != null)
        {
            localUsbEndpoint = paramUsbDevice.getInterface(0).getEndpoint(0);

            //arrayOfByte = new byte[paramInt];
        }
        for (int i = 0; i<paramInt; i++)
        {

            arrayOfByte[i] = (byte)parammsgArray[i];
        }
        this.mConnection.bulkTransfer(localUsbEndpoint, arrayOfByte, paramInt, 0);
    }


    private char setDevice() {
        this.mUsbManager = ((UsbManager) getApplicationContext().getSystemService(USB_SERVICE));
        Iterator localIterator = this.mUsbManager.getDeviceList().values().iterator();



        UsbInterface localUsbInterface;
        UsbDeviceConnection localUsbDeviceConnection;
        while (localIterator.hasNext()) {
            UsbDevice localUsbDevice = (UsbDevice)localIterator.next();
            if (Pattern.matches(".*mVendorId=6.*", localUsbDevice.toString())){
                Log.e("test","pattern : "+localUsbDevice.toString());
                this.mDevice = localUsbDevice;
                localUsbInterface = this.mDevice.getInterface(0);
                UsbEndpoint localUsbEndpoint = localUsbInterface.getEndpoint(0);
                if (localUsbEndpoint.getType() != 2)
                {
                    Log.e("test", "endpoint is not BULK type");
                    return '\000';
                }
                this.mEndpointBulk = localUsbEndpoint;
                localUsbDeviceConnection = this.mUsbManager.openDevice(this.mDevice);
                this.mConnection = localUsbDeviceConnection;

                Log.e("test", "localUsbDeviceConnection ");
            }

        }
        return 0;
    }
}

