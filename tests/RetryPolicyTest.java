/* Created 2026-10-18. GPL-2.0-or-later. Deterministic virtual-clock tests. */
package jimm;
public class RetryPolicyTest {
 static void check(boolean b,String text){if(!b)throw new RuntimeException(text);}
 public static void main(String[] args){
  RetryPolicy p=new RetryPolicy();long now=1000;p.begin(now);
  int[] delays={5,15,30,60,60};
  for(int i=0;i<delays.length;i++){check(p.failed(120,now),"I/O retry");long deadline=now+delays[i]*1000;check(p.due==deadline,"Backoff "+i);p.failed(120,now+100);check(p.due==deadline,"Duplicate errors do not extend wait");check(!p.retry(deadline-1,false),"No early retry");check(!p.retry(deadline,true),"Wait for old worker exit");check(p.retry(deadline,false),"Retry once due");check(!p.retry(deadline,false),"No parallel retry");now=deadline+1;}
  p.stop();check(!p.failed(120,now)&&!p.retry(now+999999,false),"Manual stop prevents late-error restart");
  int[] fatal={110,111,112,113,114,117,122,127,100};for(int i=0;i<fatal.length;i++){p.begin(now);check(!p.failed(fatal[i],now)&&!p.wanted,"Auth/config/rate/unknown error stops");}
  p.begin(now);check(p.watchdog(now+89999)==0&&p.watchdog(now+90000)==2,"Connect timeout");p.connected(now);check(p.watchdog(now+999999)==0,"No unsupported idle probe disconnects");p.closed(now+1000);check(p.due==now+6000,"Silent transport closure schedules retry");p.stop();p.closed(now+2000);check(!p.wanted&&p.due==0,"Manual closure never restarts");
  p.begin(now);p.failed(118,now);p.retry(now+5000,false);p.connected(now+5001);p.received(now+65002);check(p.failures==0,"Stable connection resets backoff");
  System.out.println("PASS: 5/15/30/60 backoff, duplicate errors, no overlap, stop, fatal errors, connect deadline, idle compatibility, silent transport closure.");
 }
}
