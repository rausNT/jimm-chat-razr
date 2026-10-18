// Created 2026-10-18. GPL-2.0-or-later. Uses synthetic credentials only.
using System;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Drawing;
using System.Windows.Forms;
static class SelfTest {
 static void Check(bool b,string message){if(!b)throw new Exception(message);}
 static byte[] Read(byte[] zip,string name){using(var m=new MemoryStream(zip))using(var z=new ZipArchive(m,ZipArchiveMode.Read)){var e=z.GetEntry(name);Check(e!=null,"Missing "+name);using(var s=e.Open())using(var b=new MemoryStream()){s.CopyTo(b);return b.ToArray();}}}
 static bool Same(byte[] a,byte[] b){if(a.Length!=b.Length)return false;for(int i=0;i<a.Length;i++)if(a[i]!=b[i])return false;return true;}
 public static int Run(string folder){
  try{
   Directory.CreateDirectory(folder);var settings=new Settings("1234567","Тест\0🙂","icq.jimm.online","5190");byte[] template=Personalizer.Resource("template.jar"),config=settings.Encode(),jar=Personalizer.Build(settings);
   Check(Same(Read(jar,"personal.dat"),config),"Config bytes");
   using(var m=new MemoryStream(template))using(var z=new ZipArchive(m,ZipArchiveMode.Read)){Check(z.GetEntry("personal.dat")==null,"Template must be public");foreach(var e in z.Entries)Check(Same(Read(template,e.FullName),Read(jar,e.FullName)),"Changed original entry: "+e.FullName);}
   byte[] bundle=Personalizer.Bundle(settings),source=Read(bundle,"corresponding-source.zip");Check(Same(Read(source,"resources/personal.dat"),config),"Matching personal source");Check(Same(Read(bundle,"JimmChat.jar"),jar),"Matching bundled JAR");Check(Encoding.UTF8.GetString(Read(bundle,"JimmChat.jad")).Contains("MIDlet-Jar-Size: "+jar.Length),"JAD size");Check(Read(bundle,"COPYING").Length>10000,"License");
   bool rejected=false;try{new Settings("x","p","host","5190").Encode();}catch(ArgumentException){rejected=true;}Check(rejected,"Reject invalid UIN");rejected=false;try{new Settings("1234567","p","host","65536").Encode();}catch(ArgumentException){rejected=true;}Check(rejected,"Reject invalid port");
   File.WriteAllBytes(Path.Combine(folder,"synthetic-personal.dat"),config);
   Application.EnableVisualStyles();using(var form=new InstallerForm(false)){form.StartPosition=FormStartPosition.Manual;form.Location=new Point(-10000,-10000);form.Show();form.PerformLayout();Application.DoEvents();using(var bitmap=new Bitmap(form.Width,form.Height)){form.DrawToBitmap(bitmap,new Rectangle(0,0,bitmap.Width,bitmap.Height));bitmap.Save(Path.Combine(folder,"installer-preview.png"));}form.Close();}
   File.WriteAllText(Path.Combine(folder,"result.txt"),"PASS: validated inputs; original JAR entries preserved; modified UTF fixture; corresponding source; correct JAD size; GPL included; form constructed. No Bluetooth transfer performed.");return 0;
  }catch(Exception e){Directory.CreateDirectory(folder);File.WriteAllText(Path.Combine(folder,"result.txt"),"FAIL: "+e);return 1;}
 }
}
