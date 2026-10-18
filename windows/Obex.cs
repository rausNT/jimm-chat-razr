// Adapted 2026-10-18: explicit selected recipient and in-memory generated payload. GPL-2.0-or-later.
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;

// Uses Windows RFCOMM service discovery, then the standard OBEX Object Push protocol.
public sealed class RazrBluetoothEndPoint : EndPoint {
    readonly ulong address;
    public RazrBluetoothEndPoint(ulong address) { this.address=address; }
    public override AddressFamily AddressFamily { get { return (AddressFamily)32; } }
    public override SocketAddress Serialize() {
        var sa=new SocketAddress(AddressFamily,30);
        var addr=BitConverter.GetBytes(address);
        var service=new Guid("00001105-0000-1000-8000-00805f9b34fb").ToByteArray();
        for(int i=0;i<8;i++) sa[2+i]=addr[i];
        for(int i=0;i<16;i++) sa[10+i]=service[i];
        for(int i=26;i<30;i++) sa[i]=0;
        return sa;
    }
}

public static class RazrBluetoothObex {
    static byte[] ReadExact(Socket socket,int count) {
        var data=new byte[count]; int offset=0;
        while(offset<count) {
            int n=socket.Receive(data,offset,count-offset,SocketFlags.None);
            if(n==0) throw new IOException("Bluetooth connection closed by peer");
            offset+=n;
        }
        return data;
    }
    static byte[] Receive(Socket socket) {
        var header=ReadExact(socket,3);
        int count=header[1]*256+header[2];
        if(count<3||count>16384) throw new IOException("Invalid OBEX packet length");
        var packet=new byte[count]; Array.Copy(header,packet,3);
        if(count>3) Array.Copy(ReadExact(socket,count-3),0,packet,3,count-3);
        return packet;
    }
    static byte[] Exchange(Socket socket,byte[] packet) {
        int offset=0;
        while(offset<packet.Length) offset+=socket.Send(packet,offset,packet.Length-offset,SocketFlags.None);
        return Receive(socket);
    }
    static byte[] Packet(byte operation,byte[] body) {
        int length=body.Length+3;
        if(length>65535) throw new ArgumentException("OBEX packet too large");
        var result=new byte[length]; result[0]=operation;
        result[1]=(byte)(length>>8); result[2]=(byte)length;
        Array.Copy(body,0,result,3,body.Length); return result;
    }
    static byte[] VariableHeader(byte id,byte[] value) { return Packet(id,value); }
    public static void Send(ulong address,byte[] jar,Action<string> log,Action<int> progress,System.Threading.CancellationToken cancel) {
        if(address==0||jar==null||jar.Length<1024||jar.Length>1048576)throw new ArgumentException("Invalid target or payload");
        string fileName="JimmChat.jar";bool send=true;
        using(var socket=new Socket((AddressFamily)32,SocketType.Stream,(ProtocolType)3)) {
            using(var registration=cancel.Register(()=>socket.Dispose())) {
            cancel.ThrowIfCancellationRequested();
            socket.ReceiveTimeout=90000; socket.SendTimeout=15000;
            log("Connecting to selected phone...");
            // Socket.ConnectAsync uses ConnectEx, which is not the RFCOMM connect API.
            var connect=System.Threading.Tasks.Task.Run(()=>socket.Connect(new RazrBluetoothEndPoint(address)));
            if(!connect.Wait(20000)) throw new TimeoutException("Bluetooth connection timed out (20 seconds)");
            connect.GetAwaiter().GetResult();
            log("RFCOMM connected; sending OBEX CONNECT.");
            var response=Exchange(socket,new byte[]{0x80,0,7,0x10,0,0x10,0});
            log("OBEX CONNECT: "+BitConverter.ToString(response));
            if(response[0]!=0xA0 || response.Length<7) throw new IOException("OBEX CONNECT rejected");
            int maximum=response[5]*256+response[6];
            if(maximum<255) throw new IOException("Invalid remote packet size");
            byte[] connection=new byte[0];
            for(int pos=7;pos<response.Length;) {
                int kind=response[pos]&0xC0;
                int length=kind==0xC0?5:kind==0x80?2:
                    pos+2<response.Length?response[pos+1]*256+response[pos+2]:0;
                if(length<2||pos+length>response.Length) throw new IOException("Invalid OBEX header");
                if(response[pos]==0xCB) { connection=new byte[5]; Array.Copy(response,pos,connection,0,5); }
                pos+=length;
            }
            if(send) {
                var metadata=new List<byte>(connection);
                metadata.AddRange(VariableHeader(0x01,Encoding.BigEndianUnicode.GetBytes(fileName+"\0")));
                metadata.AddRange(new byte[]{0xC3,(byte)(jar.Length>>24),(byte)(jar.Length>>16),(byte)(jar.Length>>8),(byte)jar.Length});
                // Send the first Body header with Name and Length, as a normal Object Push.
                // Some older handsets do not respond to a metadata-only PUT.
                int firstCount=Math.Min(jar.Length,Math.Min(maximum,4096)-metadata.Count-6);
                var firstBody=new byte[firstCount]; Array.Copy(jar,firstBody,firstCount);
                metadata.AddRange(VariableHeader(0x48,firstBody));
                log("Requesting phone acceptance for "+fileName+" ("+jar.Length+" bytes)...");
                response=Exchange(socket,Packet(0x02,metadata.ToArray()));
                log("OBEX PUT first body: "+BitConverter.ToString(response));
                if(response[0]!=0x90) throw new IOException("Phone rejected file metadata: "+response[0].ToString("X2"));
                int offset=firstCount;
                var deadline=DateTime.UtcNow.AddMinutes(3);
                while(offset<jar.Length) {
                    cancel.ThrowIfCancellationRequested();
                    if(DateTime.UtcNow>deadline) throw new TimeoutException("Bluetooth transfer exceeded 3 minutes");
                    int count=Math.Min(Math.Min(maximum,4096)-6-connection.Length,jar.Length-offset);
                    bool final=offset+count==jar.Length;
                    socket.ReceiveTimeout=final?90000:10000;
                    var body=new byte[count]; Array.Copy(jar,offset,body,0,count);
                    var headers=new List<byte>(connection);
                    headers.AddRange(VariableHeader(final?(byte)0x49:(byte)0x48,body));
                    response=Exchange(socket,Packet(final?(byte)0x82:(byte)0x02,headers.ToArray()));
                    byte expected=final?(byte)0xA0:(byte)0x90;
                    if(response[0]!=expected) throw new IOException("Phone rejected transfer at offset "+offset+": "+response[0].ToString("X2"));
                    offset+=count;progress(offset*100/jar.Length);
                    if(final||offset%32720==0) log("Acknowledged bytes: "+offset+" / "+jar.Length);
                }
                log("TRANSFER_COMPLETE: final OBEX Success received. Installation on phone still needs verification.");
            }
            try{response=Exchange(socket,Packet(0x81,connection));}catch(Exception){/* Final PUT already acknowledged. */}
            }
        }
    }
}
