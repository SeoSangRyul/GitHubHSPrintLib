package kr.co.rayworld.printtest.printer;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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

import kr.co.rayworld.printtest.MainActivity;
import kr.co.rayworld.printtest.R;

/**
 * Created by ssl82 on 2017-04-28.
 */

public class HS_Printer {

    private static final String HWASUNG_VENDOR_ID = ".*mVendorId=6.*";
    private static final int MAX_IMAGE_LINE = 192;

    private static final byte CMD_DLE = 0x10;
    private static final byte CMD_ESC = 0x1b;
    private static final byte CMD_GS = 0x1d;
    private static final byte CMD_FS = 0x1c;
    private static final byte CMD_SUB = 0x1a;

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
    public void printBitmap(Bitmap bitmap){
        if(mMode == NETWORK_MODE) {
            new TCP_send_Bitmap().execute(bitmap);
        }
        else if(mMode == SERIAL_MODE){

        }

    }
    class TCP_send_Bitmap extends AsyncTask<Bitmap, String, String> {
        String serverAddr = mServerAddress;//"192.168.0.253";
        int strPort = mPort;//9100;
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            if(bitmaps[0] == null){
                return null;
            }
            Socket socket = null;
            try {
                socket = new Socket(serverAddr, strPort);
                OutputStream out = socket.getOutputStream();
                writeImage(out,bitmaps[0]);
                if(bitmaps[0] != null){
                    bitmaps[0].recycle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(socket!=null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return "";
        }

        private void writeImage(OutputStream out, Bitmap bm) throws IOException {
            String Title;
            String Message;
            String mDeviceNameStr;
            int mDataCnt,i,j,k,byteOffset,bitToByte,byteWidth,prnLine;
            int bmWidth,bmHeight,x,y;
            int[] mPixels;
            int[] mIntBuf,mIntBuf2;
            byte[] sendBuff;
            byte rest = 0;	// multiple of 8?
            int blockCnt = 0;	// row data block counter

            mIntBuf2 = new int[128];


            // get image data height
            bmHeight = bm.getHeight();
            // get image data width
            bmWidth = bm.getWidth();

            byteWidth = bmWidth / 8;
            i = bmWidth % 8;
            if (i > 0) { byteWidth = byteWidth + 1; rest = 1; }	// none multiple of 8

            mPixels = new int[bmWidth*MAX_IMAGE_LINE];		// max prnLine = 192
            mIntBuf = new int[(byteWidth*MAX_IMAGE_LINE)];

            // lower the print speed for smooth printing
            // print speed command 3byte
            sendBuff = new byte[3];
            sendBuff[0] = CMD_SUB;
            sendBuff[1] = 's';
            sendBuff[2] = 6;

            out.write(sendBuff);

            x = y = 0;
            blockCnt = (bmHeight / MAX_IMAGE_LINE) ;
            if((bmHeight % MAX_IMAGE_LINE) > 0 ) blockCnt += 1;

            for( k=0;k<blockCnt;k++){
                if(bmHeight >= MAX_IMAGE_LINE){
                    prnLine = MAX_IMAGE_LINE;
                    bmHeight = bmHeight - prnLine;
                }
                else{
                    prnLine = bmHeight;
                }

                // get poxel color data
                bm.getPixels(mPixels, 0, bmWidth, x, y, bmWidth, prnLine);

                j = 0;
                bitToByte = 0;
                int bit_p = 0;

                for(i=0;i<(bmWidth*prnLine);i++){
                    switch( bit_p % 8){
                        case 0:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x80; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
//                                Title = "image data error(bit7)";
//                                Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
//                                showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 1:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x40; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit6)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 2:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x20; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit5)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 3:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x10; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit4)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 4:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x08; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit3)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 5:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x04; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit2)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }
                            break;
                        case 6:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x02; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit1)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }

                            break;
                        case 7:
                            if( mPixels[i] == 0xff000000) {bitToByte |= 0x01; }	// black
                            if( mPixels[i] != 0xff000000 && mPixels[i] != 0xffffffff ) {
                                //Title = "image data error(bit0)";
                                //Message = "pixel value:"+ new Integer(mPixels[i]).toString() ;
                                //showDialog( Title, Message);
//                                return;
                            }

                            break;
                    }

                    if( (bit_p % 8) == 7 ){
                        mIntBuf[j] = bitToByte;
                        bitToByte = 0;
                        j++;
                    }

                    bit_p++;

                    // last dot each line
                    if( ( i % bmWidth) == (bmWidth-1) ){
                        if(rest == 1) {		// if none multiple of 8, last bytean
                            mIntBuf[j] =  bitToByte;
                            bitToByte = 0;
                            bit_p = 0;
                            j++;
                        }
                    }

                }	// i end

                // send raster bitimage command 8byte

                sendBuff = new byte[8];
                sendBuff[0] = CMD_GS;
                sendBuff[1] = 'v';
                sendBuff[2] = '0';
                sendBuff[3] = 0x00;				// size normal
                sendBuff[4] = (byte)(byteWidth % 256);	// xL(LSB horizental number of data)
                sendBuff[5] = (byte)(byteWidth / 256);	// xH(MSB horizental number of data)
                sendBuff[6] = (byte)(prnLine % 256);	// yL(LSB vertical number of line)
                sendBuff[7] = (byte)(prnLine / 256);	// yH(MSB vertical number of line)
                out.write(sendBuff);

                mDataCnt = byteWidth * prnLine;

                sendBuff = new byte[mDataCnt];
                for(int l=0;l<sendBuff.length;l++){
                    sendBuff[l]=(byte)mIntBuf[l];
                }
                Log.e("test","sendBuff[length] : "+sendBuff.length);
                out.write(sendBuff);

                y += prnLine;
            }	// k end
            sendBuff = new byte[3];
            sendBuff[0] = CMD_SUB;
            sendBuff[1] = 's';
            sendBuff[2] = 14;
            out.write(sendBuff);

            // feeding
            sendBuff = new byte[5];
            sendBuff[0] = 0xA;
            sendBuff[1] = 0xA;
            sendBuff[2] = 0xA;
            sendBuff[3] = 0xA;
            sendBuff[4] = 0xA;
            out.write(sendBuff);

            // full cutting
            sendBuff = new byte[2];
            sendBuff[0] = CMD_ESC;
            sendBuff[1] = 0x69;
            out.write(sendBuff);


        }
    }


    class TCP_send extends AsyncTask<String, String, String> {
        String serverAddr = mServerAddress;//"192.168.0.253";
        int strPort = mPort;//9100;
        @Override
        protected String doInBackground(String... strings) {
            Socket socket = null;
            try {
                socket = new Socket(serverAddr, strPort);
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
            } finally {
                try {
                    if(socket!=null)
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return "";

        }
    }
}
