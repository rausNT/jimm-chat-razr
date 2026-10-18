package jimm;
public class ConnectionTraceTest {
 public static void main(String[] args){
  ConnectionTrace.begin();ConnectionTrace.progress(12);ConnectionTrace.packet();
  if(ConnectionTrace.status().indexOf("12%")<0||ConnectionTrace.status().indexOf("RX:1")<0)throw new AssertionError("stage and receive count");
  for(int i=0;i<1000;i++)ConnectionTrace.event(3,i);
  String report=ConnectionTrace.report();
  if(report.length()>1000||report.indexOf("3:979\n")>=0||report.indexOf("3:999\n")<0)throw new AssertionError("bounded chronological ring");
  ConnectionTrace.begin();if(ConnectionTrace.status().indexOf("RX:0")<0||ConnectionTrace.report().indexOf("3:999")>=0)throw new AssertionError("new attempt clears old data");
  System.out.println("PASS: bounded numeric diagnostics, stage/receive count, reset between attempts.");
 }
}
