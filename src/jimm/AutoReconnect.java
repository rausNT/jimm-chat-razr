/* Created 2026-10-18. GPL-2.0-or-later. No message replay. */
package jimm;
import java.util.Timer;
import java.util.TimerTask;
import jimm.comm.*;
public final class AutoReconnect {
 private static final RetryPolicy policy=new RetryPolicy();private static Timer timer;
 public static void begin(){policy.begin(System.currentTimeMillis());ensureTimer();}
 private static synchronized void ensureTimer(){if(timer!=null)return;timer=new Timer();timer.schedule(new TimerTask(){public void run(){tick();}},1000,1000);}
 public static void stop(){policy.stop();}
 public static synchronized void shutdown(){policy.stop();if(timer!=null){timer.cancel();timer=null;}}
 public static boolean connecting(){return policy.isConnecting();}
 public static void transportClosed(){policy.closed(System.currentTimeMillis());}
 public static boolean wanted(){return policy.wantsConnection();}
 public static boolean failed(int code){return policy.failed(code,System.currentTimeMillis());}
 public static void connected(){policy.connected(System.currentTimeMillis());}
 public static void received(){policy.received(System.currentTimeMillis());}
 public static String status(){return policy.status(System.currentTimeMillis());}
 private static void tick(){try{
  long now=System.currentTimeMillis();
  if(policy.retry(now,Icq.workerBusy()))Icq.connectAutomatic();
  int action=policy.watchdog(now);
  if(action==2){JimmException.handleException(new JimmException(118,0));}
  SimpleChat.networkTick();
 }catch(Exception e){/* A timer tick must not kill subsequent recovery checks. */}}
}
