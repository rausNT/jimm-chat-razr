// Created 2026-10-18. GPL-2.0-or-later; see ../COPYING.
using System;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Text;
using System.Collections.Generic;

public sealed class Settings {
    public string Uin,Password,Host,Port;
    public Settings(string u,string p,string h,string port){Uin=u.Trim();Password=p;Host=h.Trim();Port=port.Trim();}
    public void Validate(){
        long u;int p;
        if(!Int64.TryParse(Uin,out u)||u<10000||u>2147483647||Uin.Length>10)throw new ArgumentException("Введите числовой UIN от 10000 до 2147483647.");
        if(Password.Length==0||Password.Length>128)throw new ArgumentException("Введите пароль (до 128 символов).");
        if(!Int32.TryParse(Port,out p)||p<1||p>65535)throw new ArgumentException("Порт должен быть от 1 до 65535.");
        if(Host.Length==0||Host.Length>128||Host.IndexOfAny(new char[]{' ', '/', ':','\r','\n','\t'})>=0)throw new ArgumentException("Введите имя сервера без протокола и порта.");
    }
    // Java DataInputStream.readUTF uses modified UTF-8 (UTF-16 code units).
    static void Utf(Stream output,string text){
        using(var bytes=new MemoryStream()){
            foreach(char c in text){if(c>=1&&c<=127)bytes.WriteByte((byte)c);else if(c<=2047){bytes.WriteByte((byte)(0xC0|(c>>6)));bytes.WriteByte((byte)(0x80|(c&63)));}else{bytes.WriteByte((byte)(0xE0|(c>>12)));bytes.WriteByte((byte)(0x80|((c>>6)&63)));bytes.WriteByte((byte)(0x80|(c&63)));}}
            output.WriteByte((byte)(bytes.Length>>8));output.WriteByte((byte)bytes.Length);bytes.Position=0;bytes.CopyTo(output);
        }
    }
    public byte[] Encode(){Validate();using(var s=new MemoryStream()){s.Write(new byte[]{0x4A,0x43,0x46,0x31},0,4);Utf(s,Uin);Utf(s,Password);Utf(s,Host);Utf(s,Port);return s.ToArray();}}
}
public static class Personalizer {
    public static byte[] Resource(string name){using(var s=Assembly.GetExecutingAssembly().GetManifestResourceStream(name)){if(s==null)throw new IOException("Missing bundled resource: "+name);using(var b=new MemoryStream()){s.CopyTo(b);return b.ToArray();}}}
    public static void Entry(ZipArchive zip,string name,byte[] data){var e=zip.CreateEntry(name,CompressionLevel.Optimal);e.LastWriteTime=new DateTimeOffset(2026,10,18,0,0,0,TimeSpan.Zero);using(var stream=e.Open())stream.Write(data,0,data.Length);}
    public static byte[] Add(byte[] template,string name,byte[] data){
        using(var output=new MemoryStream()){
            using(var target=new ZipArchive(output,ZipArchiveMode.Create,true))using(var input=new MemoryStream(template))using(var source=new ZipArchive(input,ZipArchiveMode.Read)){
                foreach(var e in source.Entries){if(e.FullName==name)continue;using(var b=new MemoryStream()){using(var s=e.Open())s.CopyTo(b);Entry(target,e.FullName,b.ToArray());}}
                Entry(target,name,data);
            }return output.ToArray();
        }
    }
    public static byte[] Build(Settings settings){return Add(Resource("template.jar"),"personal.dat",settings.Encode());}
    public static byte[] Bundle(Settings settings){
        byte[] jar=Build(settings),source=Add(Resource("source.zip"),"resources/personal.dat",settings.Encode());
        using(var output=new MemoryStream()){
            using(var z=new ZipArchive(output,ZipArchiveMode.Create,true)){
                Entry(z,"JimmChat.jar",jar);Entry(z,"corresponding-source.zip",source);Entry(z,"COPYING",Resource("COPYING"));
                string jad=Encoding.UTF8.GetString(Resource("template.jad"));jad=System.Text.RegularExpressions.Regex.Replace(jad,@"MIDlet-Jar-Size: \d+","MIDlet-Jar-Size: "+jar.Length);Entry(z,"JimmChat.jad",Encoding.UTF8.GetBytes(jad));
                Entry(z,"READ-ME.txt",Encoding.UTF8.GetBytes("PERSONAL PACKAGE — contains an extractable password in JAR and source. Keep private.\r\nInstall JimmChat.jar using the phone's normal Java installer.\r\nExisting saved settings override these defaults. Change them in My account; do not delete app data just to apply defaults.\r\nMatching GPL-2.0-or-later source and build instructions are in corresponding-source.zip.\r\n"));
            }return output.ToArray();
        }
    }
}
