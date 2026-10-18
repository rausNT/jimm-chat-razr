/* Diagnostic build, modified 2026-10-18. GPL-2.0-or-later. */
package jimm;
/** Fixed numeric events only: never records credentials, addresses or payloads. */
public final class ConnectionTrace {
 private static final String[] rows=new String[20];
 private static int count,next,progress,packets;
 private static long started;
 public static synchronized void begin(){count=next=progress=packets=0;started=System.currentTimeMillis();event(1,0);}
 public static synchronized void event(int kind,int value){rows[next]=seconds()+"s "+kind+":"+value;next=(next+1)%rows.length;if(count<rows.length)count++;}
 public static synchronized void packet(){packets++;}
 public static synchronized void progress(int value){if(progress!=value){progress=value;event(2,value);}}
 private static long seconds(){return started==0?0:Math.max(0,(System.currentTimeMillis()-started)/1000);}
 public static synchronized String status(){return progress+"% "+seconds()+"s RX:"+packets;}
 public static synchronized String report(){StringBuffer b=new StringBuffer("Connection diagnostics\n");b.append(status());b.append("\n1=start 2=stage 3=error 4=socket-open 5=EOF 6=reset 7=permission\n");for(int i=0;i<count;i++){b.append(rows[(next-count+i+rows.length)%rows.length]);b.append('\n');}return b.toString();}
}
