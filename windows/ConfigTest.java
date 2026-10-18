/* Created 2026-10-18. GPL-2.0-or-later. Cross-runtime synthetic fixture test. */
import jimm.PersonalDefaults;
import java.io.*;
public class ConfigTest {
 public static void main(String[] args)throws Exception{
  String[] v=PersonalDefaults.read(new FileInputStream(args[0]));
  if(!v[0].equals("1234567")||!v[1].equals("Тест\0🙂")||!v[2].equals("icq.jimm.online")||!v[3].equals("5190"))throw new Exception("Cross-runtime mismatch");
  if(PersonalDefaults.read(null)!=null)throw new Exception("Absent resource");
  boolean rejected=false;try{PersonalDefaults.read(new ByteArrayInputStream(new byte[]{1,2,3,4}));}catch(IOException e){rejected=true;}if(!rejected)throw new Exception("Bad magic");
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(0x4A434631);out.writeUTF("1234567");out.writeUTF("p");out.writeUTF("host");out.writeUTF("65536");rejected=false;try{PersonalDefaults.read(new ByteArrayInputStream(b.toByteArray()));}catch(IOException e){rejected=true;}if(!rejected)throw new Exception("Invalid port");
  System.out.println("PASS: C# modified UTF read by actual Java reader; missing resource, malformed magic and invalid port handled.");
 }
}
