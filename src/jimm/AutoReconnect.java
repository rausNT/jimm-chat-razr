/* Created 2026-10-18. GPL-2.0-or-later. No message replay. */
package jimm;
import java.util.Timer;
import java.util.TimerTask;
import jimm.comm.*;
public final class AutoReconnect {
 private static final RetryPolicy policy=new RetryPolicy();private static Timer timer;private static boolean probeSending;
 public static void begin(){policy.begin(System.currentTimeMillis());ensureTimer();}
 private static synchronized void ensureTimer(){if(timer!=null)return;timer=new Timer();timer.schedule(new TimerTask(){public void run(){tick();}},1000,1000);}
 public static void stop(){policy.stop();}
 public static synchronized void shutdown(){policy.stop();if(timer!=null){timer.cancel();timer=null;}}
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
  if(action==1){final Icq.Connection connection=Icq.c;synchronized(AutoReconnect.class){if(probeSending)return;probeSending=true;}
   new Thread(new Runnable(){public void run(){try{if(connection!=null&&connection==Icq.c&&Icq.isConnected())connection.sendPacket(new SnacPacket(4,4,0,new byte[0],new byte[0]));}catch(JimmException e){if(connection==Icq.c)JimmException.handleException(e);}finally{synchronized(AutoReconnect.class){probeSending=false;}}}}).start();
  }
  if(SimpleChat.singleton!=null)SimpleChat.singleton.repaint();
 }catch(Exception e){/* A timer tick must not kill subsequent recovery checks. */}}
}
