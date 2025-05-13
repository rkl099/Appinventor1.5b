// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016-2019 MIT, All rights reserved
// Copyright 2017-2019 Kodular, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

/*
     210205 First version
     211017 Add BT support
     211201 Add wifi support
     211229 Add modbus tcp, change name to Modbus
     230126 Add set diver
*/

package com.Modbus;

import android.content.Context;
import android.util.Log;

import com.physicaloid_ai.lib.Physicaloid;

//import com.physicaloid.lib.Physicaloid;
//import com.physicaloid.lib.Boards;


import com.google.appinventor.components.runtime.*;


import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import java.lang.reflect.Field; 
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(version = YaVersion.SERIAL_COMPONENT_VERSION,
    description = "New Serial Modbus extension",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png",
    androidMinSdk = 12)

@SimpleObject(external = true)
@UsesPermissions(permissionNames =
                 "android.permission.USB_PERMISSION, " +
                 "android.permission.ACCESS_NETWORK_STATE, " +
                 "android.permission.ACCESS_WIFI_STATE, " +
                 "android.permission.INTERNET, " +
                 "android.permission.BLUETOOTH, " +
                 "android.permission.BLUETOOTH_ADMIN, " +
                 "android.permission.READ_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "physicaloid_ai.jar" )
//@UsesLibraries(libraries = "physicaloid.jar" )

public class Modbus extends AndroidNonvisibleComponent implements Component {
  private static final String LOG_TAG = "Serial Component";

  private Context context;

  private Physicaloid mPhysicaloid=null;

  // Must correspond to default in Physicaloid
  private int baudRate = 9600;
  private int bytes = 1024;
  private int parity = 0;
  private int dtr = 0;
  private int rts = 0;
  private int driver = 0;
  
  private enum modbus {rtu, tcp }
  private modbus protocol = modbus.rtu;
  private int seq_nr;
  
  public Modbus (ComponentContainer container) {
    super(container.$form());
    context = container.$context();
    Log.d(LOG_TAG, "Created");
  }


//***********************  Init Open, Close  ******************************************************


  @SimpleFunction(description = "Initializes serial connection.")
  public void Initialize() {
    mPhysicaloid = new Physicaloid(context);
//    BaudRate(this.baudRate);   //cant be set before open
    Log.d(LOG_TAG, "Initialized");
  }

  @SimpleFunction(description = "Initializes serial bluetooth connection.")
  public void InitializeBT(String btname) {
    mPhysicaloid = new Physicaloid(context,false,btname);
    Log.d(LOG_TAG, "Initialized");
  }

  @SimpleFunction(description = "Initializes serial TCP wifi connection.")
  public void InitializeWIFI(String net, int dport ) {
    mPhysicaloid = new Physicaloid(context,false,net,dport);
    Log.d(LOG_TAG, "Initialized");
  }
  

  @SimpleFunction(description = "Opens serial connection. Returns true when opened.")
  public boolean Open() {
    Log.d(LOG_TAG, "Opening connection");
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "Open", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return false;
    }
    if ( !mPhysicaloid.open()) return false;
//  Set local state
    mPhysicaloid.setBaudrate(this.baudRate);
    mPhysicaloid.setParity(this.parity);
    mPhysicaloid.setDtrRts(this.dtr != 0,this.rts != 0);
    return true;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns true when the Serial connection is connected.")
  public boolean IsConnected() {
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "IsConnected", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return false;
    }
    return mPhysicaloid.isConnected();
  }

  @SimpleFunction(description = "Closes serial connection. Returns true when closed.")
  public boolean Close() {
    Log.d(LOG_TAG, "Closing connection");
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "Close", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return false;
    }
    return mPhysicaloid.close();
  }
  
  
/**********************  Properties *****************************************************************/


  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns true when the Serial connection is open.")
  public boolean IsOpen() {
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "IsOpen", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return false;
    }
    return mPhysicaloid.isOpened();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns true when the Serial has been initialized.")
  public boolean IsInitialized() {
    return mPhysicaloid != null;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Baud 300,600,1200,2400,9600,19200,38400,57600,115200")
  public int BaudRate() {
    return this.baudRate;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "9600")
  @SimpleProperty
  public void BaudRate(int baudRate) {
    this.baudRate = baudRate;
    Log.d(LOG_TAG, "Baud Rate: " + baudRate);
    if (mPhysicaloid != null)
      mPhysicaloid.setBaudrate(baudRate);
    else
      Log.w(LOG_TAG, "Could not set Serial Baud Rate to " + baudRate + ". Just saved, not applied to serial! Maybe you forgot to initialize it?");
  }


// 0=NONE,  1=ODD,  2=EVEN,  (3=MARK 4=SPACE) corresponds to UartConfig.PARITY_xxx
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Parity 0=no, 1=Odd, 2=Even")
  public int Parity() {
      return this.parity;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
  @SimpleProperty
  public void Parity(int parity) {
    this.parity = parity;
    Log.d(LOG_TAG, "Parity: " + parity);
    if (mPhysicaloid != null)
      mPhysicaloid.setParity(parity);
    else
      Log.w(LOG_TAG, "Could not set parity " + parity );
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "DTR 0,1")
  public int Dtr() {
      return this.dtr;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
  @SimpleProperty
  public void Dtr(int dtr) {
    this.dtr = dtr;
    Log.d(LOG_TAG, "Dtr: " + dtr);
    if (mPhysicaloid != null)
      mPhysicaloid.setDtrRts(this.dtr != 0,this.rts != 0);
    else
      Log.w(LOG_TAG, "Could not set dtr " + dtr );
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "RTS 0,1")
  public int Rts() {
      return this.rts;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
  @SimpleProperty
  public void Rts(int rts) {
    this.rts = rts;
    Log.d(LOG_TAG, "Rts: " + rts);
    if (mPhysicaloid != null)
      mPhysicaloid.setDtrRts(this.dtr != 0,this.rts != 0);
    else
      Log.w(LOG_TAG, "Could not set rts " + rts );
  }
  
  
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
  @SimpleProperty
  public void USBdriver(int driver) {
    this.driver = driver;
    Log.d(LOG_TAG, "Driver: " + driver);
    if (mPhysicaloid != null)
      mPhysicaloid.setDriver(this.driver);
    else
      Log.w(LOG_TAG, "Could not set driver " + rts );
  }



//********************************************************************************************

//  0=rtu, 1=tcp
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0" )
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "0=RTU, 1=TCP")
//  @SimpleProperty
  public void Protocol(int prot) {
    Log.d(LOG_TAG, "Protocol: " + prot);
      if (prot == 0) protocol = modbus.rtu;
      if (prot == 1) protocol = modbus.tcp;
  }


//  get Msb of num
  @SimpleFunction(description = "Returns Ms byte om unt16")
  public int HiByte (int num) {
    return ( (num>>8) & 0x0ff);
}


//  get  Lsb of num
  @SimpleFunction(description = "Returns Ls byte om unt16")
  public int LoByte(int num) {
    return ( num & 0x0ff );
}


//  get crc seed
  @SimpleFunction(description = "Returns crc16seed")
  public int CRC16Seed() {
    return (0x0ffff);
}


//  add data to crc
  @SimpleFunction(description = "Add data to crc16, return new. Transmit as lsb msb!")
  public int NewCRC(int Byte, int OldCRC) {
  
     int crc=OldCRC;
     crc ^= ((int)Byte)&0xff; 
     for (int i=0; i<8; i++)
     {
       if ((crc & 0x0001) != 0) {      // If the LSB is set
         crc >>>= 1;                    // Shift right and XOR 0xA001
         crc ^= 0xA001;
       }
       else                            // Else LSB is not set
         crc >>>= 1;    
     }     
     return (crc);
}


private int CRC16 ( byte[] mess, int l )
{
   int crc= 0xFFFF;
   for (int pos = 0; pos < l; pos++){
     crc ^= ((int)mess[pos])&0xff; 
     for (int i=0; i<8; i++)
     {
       if ((crc & 0x0001) != 0) {      // If the LSB is set
         crc >>>= 1;                    // Shift right and XOR 0xA001
         crc ^= 0xA001;
       }
       else                            // Else LSB is not set
         crc >>>= 1;    
     }     
   }
   return (crc<<8) | (crc>>>8);            //swap bytes
}


//  @SimpleFunction(description = "Reads registers using fc. If error, return -1")
  private int ReadRegs( int fc, int slave, int addr, int cnt) {

    byte[] buff = new byte[this.bytes];
    int crc;
    int pos=0;
    
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadRegs", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      if (protocol == modbus.tcp) {
         buff[pos++]=(byte)((seq_nr++ >>8) & 0xFF);    // transaction nr
         buff[pos++]=(byte) (seq_nr & 0xFF);
         buff[pos++]=0;    // protocol (always 0 for modbus)
         buff[pos++]=0;
         buff[pos++]=0;    // nr of bytes
         buff[pos++]=0;
      }      

      buff[pos++]=(byte) (slave & 0xFF);
      buff[pos++]=(byte) (fc & 0xFF);
      buff[pos++]=(byte)((addr>>8) & 0xFF);
      buff[pos++]=(byte) (addr & 0xFF);
      buff[pos++]=(byte) ((cnt>>8) & 0xFF);                 //no of reg
      buff[pos++]=(byte) (cnt & 0xFF);
      if (protocol == modbus.tcp) {
          buff[4] = (byte) ((pos-6)>>8);         //set length - modbus tcp header
          buff[5] = (byte) ((pos-6) & 0x00ff);
      }    
      if (protocol == modbus.rtu) {
        crc=CRC16(buff,6);
        buff[pos++]=(byte) (crc>>8);    
        buff[pos++]=(byte) (crc & 0x00ff);
      }  
      return mPhysicaloid.write(buff,pos);
    }  
  } 

  @SimpleFunction(description = "Reads 1 holding register using functon code 3. If error, return -1")
  public int ReadSingleHoldingReg(int slave, int addr) {

    int crc;
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      return ReadRegs (0x03, slave, addr, 1);
    }  
  } 
  
  @SimpleFunction(description = "Reads 1 double 32bit holding register using functon code 3. If error, return -1")
  public int ReadDoubleHoldingReg(int slave, int addr) {

    int crc;
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      return ReadRegs (0x03, slave, addr, 2);
    }  
  } 
  
  
  @SimpleFunction(description = "Reads 1 input register using function code 4. If error, return -1")
  public int ReadSingleInputReg(int slave, int addr) {

    int crc;
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      return ReadRegs (0x04, slave, addr, 1);
    }  
  } 



  @SimpleFunction(description = "Write 1 holding regster using functopn code 6. If error, return -1")
  private int WriteSingleReg( int fc, int slave, int addr, int data) {

    int crc;
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      int pos=0;
      byte[] buff = new byte[this.bytes];
      if (protocol == modbus.tcp) {      
         buff[pos++]=(byte)((seq_nr++ >>8) & 0xFF);    // transaction nr
         buff[pos++]=(byte) (seq_nr & 0xFF);
         buff[pos++]=0;    // protocol (always 0 for modbus)
         buff[pos++]=0;
         buff[pos++]=0;    // nr of bytes  (6 for single reg write)
         buff[pos++]=0;
      }
      buff[pos++]=(byte) (slave & 0xFF);
      buff[pos++]=(byte) (fc & 0xff);
      buff[pos++]=(byte)((addr>>8) & 0xFF);
      buff[pos++]=(byte) (addr & 0xFF);
      buff[pos++]=(byte)((data>>8) & 0xFF);
      buff[pos++]=(byte) (data & 0xFF);
      if (protocol == modbus.tcp) {
          buff[4] = (byte) ((pos-6)>>8);         //set length - modbus tcp header
          buff[5] = (byte) ((pos-6) & 0x00ff);
      }    
      if (protocol == modbus.rtu) {
         crc=CRC16(buff,6);
         buff[pos++]=(byte) (crc>>8);    
         buff[pos++]=(byte) (crc & 0x00ff);
      }   
      return mPhysicaloid.write(buff,pos);
    }  
  } 

  
  @SimpleFunction(description = "Write 1 holding regster using functopn code 6. If error, return -1")
  public int WriteSingleHoldingReg(int slave, int addr, int data) {

    int crc;
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      return WriteSingleReg(06, slave, addr, data );
    }  
  } 


  @SimpleFunction(description = "Respons for single read or write,if ok ret data (0..65535). If error ret <0")
  public long ResponseSingle() {

    int crc;
    byte[] buff = new byte[this.bytes];
    byte[] tcpbuff = new byte [6];
        
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ResponsSingle", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } 
    
    int n=mPhysicaloid.bytesInReadBuffer();
      
    if (protocol == modbus.tcp) {  
       if (n < 11) return -2;          //min 6+5 bytes for tcp
       mPhysicaloid.read(tcpbuff,6);
       // check tcp header0
    }
    
    int len = mPhysicaloid.read(buff);

    if (protocol == modbus.rtu) {       
       if (n<7) return -3;                //min 7 bytes incl crc for rtu
       crc=CRC16(buff,len);
       if (crc != 0) return -4;   //crc error
    }
       
    if ((buff[1] == 0x04) && (buff[2] == 2)) return (buff[3]<<8)&0xff00 | 0xff & (int) buff[4];                  // read single fc4 ok
    if ((buff[1] == 0x03) && (buff[2] == 2)) return (buff[3]<<8)&0xff00 | 0xff & (int) buff[4];                  // read single fc3 ok  
    if ((buff[1] == 0x03) && (buff[2] == 4))
      return (buff[3]<<24)&0xff000000 | (buff[4]<<16)&0xff0000 | (buff[5]<<8)&0xff00 | (int) buff[6] & 0xff ;    // read double fc3 ok      
    if  (buff[1] == 0x06)  return (buff[4]<<8)&0xff00 | 0xff & (int) buff[5];                                    // write single fc6 ok     
    return -5;    // unknown fc or nr bytes/regs error

  } 

 

/*
  *****************   Read and write a number 0..255 that is sent received to serial line   *********************************
*/

  @SimpleFunction(description = "Reads 1 byte data from serial as int (0-255). If empty, return -1")
  public int ReadByte() {

    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "ReadByte", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      byte[] buf = new byte[this.bytes];
      if (mPhysicaloid.read(buf,1) > 0) {
        return 0x00ff & (int) buf[0];
      }
    }
    return -1;
  } 


  @SimpleFunction(description = "Writes int (0-255) as 1 byte to serial.")
  public void WriteByte (int number) {
    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "WriteByte", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
    }
    else {
      byte[] buf = new byte[1];
      buf[0] = (byte) (number & 0x00ff);
      int result = mPhysicaloid.write(buf);
      if (result == -1)
        form.dispatchErrorOccurredEvent(Modbus.this, "WriteSerial", ErrorMessages.ERROR_SERIAL_WRITING);
    }
  }
 

  
//***********************  Misc functions  *****************************************  

  @SimpleFunction(description = "Nr of bytes to read in buffer")
  public int Available() {

    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "Available", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
      return mPhysicaloid.bytesInReadBuffer(); 
    }
  } 


  @SimpleFunction(description = "Flush read buffer")
  public int Flush() {

    if (mPhysicaloid == null) {
      form.dispatchErrorOccurredEvent(Modbus.this, "Flush", ErrorMessages.ERROR_SERIAL_NOT_INITIALIZED);
      return -1;
    } else {
          byte[] buf = new byte[this.bytes];
          int len = mPhysicaloid.read(buf);
//          lbuf_i=0; 
          return 0;
    }
  } 
 

// Return driver name

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Driver name from Open")
  public String DriverName() {
    if (mPhysicaloid != null)
        return mPhysicaloid.getDriverName();
//        return mPhysicaloid.getPhysicalConnectionName();  // like USB etc
    else
        return "";    
  }





}
