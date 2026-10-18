/* Created 2026-10-18: installer-supplied initial settings. GPL-2.0-or-later. */
package jimm;
import java.io.*;

/** Initial values only. Options.load() subsequently overrides these with RMS. */
public final class PersonalDefaults {
 public static String[] read(InputStream stream) throws IOException {
  if(stream==null)return null;
  DataInputStream in=new DataInputStream(stream);
  try {
   if(in.readInt()!=0x4A434631)throw new IOException("Unknown settings format");
   String[] v=new String[4];for(int i=0;i<4;i++)v[i]=in.readUTF();
   long u;int port;
   try{u=Long.parseLong(v[0]);port=Integer.parseInt(v[3]);}catch(Exception e){throw new IOException("Invalid settings");}
   if(u<10000||u>2147483647L||v[0].length()>10||v[1].length()==0||v[1].length()>128||v[2].length()==0||v[2].length()>128||v[2].indexOf(' ')>=0||v[2].indexOf('/')>=0||v[2].indexOf(':')>=0||port<1||port>65535||in.read()!=-1)throw new IOException("Invalid settings");
   return v;
  } finally {in.close();}
 }
}
