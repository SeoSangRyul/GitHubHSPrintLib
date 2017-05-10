package kr.co.rayworld.printtest.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by ssl82 on 2017-04-28.
 */

public class HS_Printer {

    private static final String HWASUNG_VENDOR_ID = ".*mVendorId=6.*";
    private static final byte CMD_DLE = 16;
    private static final byte CMD_ESC = 27;
    private static final byte CMD_FS = 28;
    private static final byte CMD_GS = 29;

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final int SERIAL_MODE = 1;
    public static final int NETWORK_MODE = 2;

    private UsbDeviceConnection mConnection;
    private UsbDevice mDevice;
    private UsbEndpoint mEndpointBulk;
    private UsbManager mUsbManager;
    private Context mContext;
    private PendingIntent mPermissionIntent;
    private int mMode;

    //네트워크 주소
    private String mServerAddress;
    private int mPort;


    public HS_Printer(Context context, int mode){
        this.mMode = mode;
        this.mContext =  context;
        this.mUsbManager = ((UsbManager) context.getApplicationContext().getSystemService(context.USB_SERVICE));
        mPermissionIntent = PendingIntent.getBroadcast(mContext,0,new Intent(HS_Printer.ACTION_USB_PERMISSION),0);
        IntentFilter filter = new IntentFilter(HS_Printer.ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
        setDevice();
    }
    public HS_Printer(Context context, int mode, String serverAddress, int port){
        this.mMode = mode;
        this.mContext =  context;
        this.mServerAddress = serverAddress;
        this.mPort = port;

    }



    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if( HS_Printer.ACTION_USB_PERMISSION.equals(action)){
                synchronized(this){
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        if(device != null){
                            //call method to set up device communication

                        }
                    }
                    else{
                        Log.d("test","permission denied for device "+ device);
                    }
                }
            }
        }
    };


    private char setDevice() {
        Iterator localIterator = this.mUsbManager.getDeviceList().values().iterator();

        //USB 장비 확인
        if(!localIterator.hasNext()){
            Toast.makeText(mContext, "Can not find Hwasung USB Device!",Toast.LENGTH_SHORT).show();
            return 0;
        }

        UsbInterface localUsbInterface;
        UsbDeviceConnection localUsbDeviceConnection;
        while (localIterator.hasNext()) {
            UsbDevice localUsbDevice = (UsbDevice)localIterator.next();
            if (Pattern.matches(HWASUNG_VENDOR_ID, localUsbDevice.toString())){
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
                mUsbManager.requestPermission(this.mDevice, mPermissionIntent);
                try {
                    localUsbDeviceConnection = this.mUsbManager.openDevice(this.mDevice);
                }
                catch (Exception e){
                    return 0;
                }
                this.mConnection = localUsbDeviceConnection;

                Log.e("test", "localUsbDeviceConnection ");
                return 1;
            }

        }
        Toast.makeText(mContext, "Can not find Hwasung USB Device!",Toast.LENGTH_SHORT).show();
        return 0;
    }

    private void sendCommand(UsbDevice paramUsbDevice, int[] parammsgArray, int paramInt)
    {

        UsbEndpoint localUsbEndpoint = null;
        byte[] arrayOfByte = new byte[paramInt];
        if (this.mConnection != null) {
            localUsbEndpoint = paramUsbDevice.getInterface(0).getEndpoint(0);

            //arrayOfByte = new byte[paramInt];
        }
        for (int i = 0; i < paramInt; i++) {

            arrayOfByte[i] = (byte) parammsgArray[i];
        }
        try {
            this.mConnection.bulkTransfer(localUsbEndpoint, arrayOfByte, paramInt, 0);
        }catch(Exception e){

        }
    }

    private void serialPrint(String str){
        if(setDevice()!=0) {
            //String msg = "   Hwasung System\n   Unit 604 Bldg B Digital Empire,\n   Kwanyang 906-4,Dongan,Anyang City,\n   Gyeonggi-Do 431-060,S.Korea\n                  TEL.82-31-8086-7550\n                  FAX.82-31-8086-7555\n\n  Price       Q'ty       Item\n------------------------------------\n  EUR1.50       1        Coke\n  EUR2.99       1        T-Shirts\n  EUR6.50      24        Corn Chips\n  EUR2.68       3        KFC\n  EUR3.66       4        Button\n-------------------------------------\n";
            String msg;
            msg = str;
            int msgLength = msg.length();
            int[] msgArray = new int[msgLength];
            for (int i = 0; i < msg.length(); i++) {
                msgArray[i] = msg.getBytes()[i];
            }

            sendCommand(this.mDevice, msgArray, msgLength);



            // feeding
            msgArray[0] = 0xA;
            msgArray[1] = 0xA;
            msgArray[2] = 0xA;
            msgArray[3] = 0xA;
            msgArray[4] = 0xA;
            sendCommand(this.mDevice, msgArray, 5);

            // full cutting
            msgArray[0] = CMD_ESC;
            msgArray[1] = 0x69;
            sendCommand(this.mDevice, msgArray, 2);
        }
    }


    public void printString(String str){
        if(mMode == NETWORK_MODE) {
            new TCP_send().execute(str);
        }
        else if(mMode == SERIAL_MODE)
            serialPrint(str);

    }

    class TCP_send extends AsyncTask<String, String, String> {
        String serverAddr = mServerAddress;//"192.168.0.253";
        int strPort = mPort;//9100;
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
}
