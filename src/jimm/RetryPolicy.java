/* Created 2026-10-18. GPL-2.0-or-later. Clock-driven network recovery policy. */
package jimm;
public final class RetryPolicy {
 public boolean wanted,online,connecting;public int failures,lastError;public long due,started,lastReceived,probeAt;
 public synchronized void begin(long now){wanted=true;online=false;connecting=true;failures=0;due=0;started=now;lastError=0;probeAt=0;}
 public synchronized void stop(){wanted=false;online=false;connecting=false;due=0;probeAt=0;lastError=0;}
 public static boolean retryable(int code){return code==118||code==120||code==121||code==123||code==124||code==125||code==126||code==128||code==129||code==221;}
 public synchronized boolean failed(int code,long now){lastError=code;if(!wanted)return false;if(!retryable(code)){stop();lastError=code;return false;}if(due!=0)return true;online=false;connecting=false;probeAt=0;int[] seconds={5,15,30,60};due=now+seconds[Math.min(failures,3)]*1000L;failures++;return true;}
 public synchronized void connected(long now){if(!wanted)return;online=true;connecting=false;due=0;lastReceived=now;started=now;probeAt=0;lastError=0;}
 public synchronized void received(long now){lastReceived=now;probeAt=0;if(online&&now-started>=60000)failures=0;}
 public synchronized boolean retry(long now,boolean workerBusy){if(!wanted||due==0||now<due||workerBusy)return false;due=0;connecting=true;started=now;return true;}
 public synchronized int watchdog(long now){return wanted&&connecting&&now-started>=90000?2:0;}
 public synchronized boolean isConnecting(){return wanted&&connecting;}
 public synchronized void closed(long now){if(wanted&&due==0)failed(120,now);}

 public synchronized boolean wantsConnection(){return wanted;}
 public synchronized String status(long now){if(!wanted)return lastError==0?"Не в сети":"Ошибка "+lastError;if(online)return "В сети";if(connecting)return "Подключение…";return "#"+lastError+" Повтор "+Math.max(0,(due-now+999)/1000)+"с";}
}
