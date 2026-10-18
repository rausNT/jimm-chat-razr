// Created 2026-10-18. GPL-2.0-or-later. Metadata only; never sends credentials.
using System;
using System.Net;
using System.IO;
using System.Collections;
using System.Collections.Generic;
using System.Web.Script.Serialization;
public static class Updates {
 public const string Current="0.2.0";
 public static string Check(){
  ServicePointManager.SecurityProtocol|=SecurityProtocolType.Tls12;
  var request=(HttpWebRequest)WebRequest.Create("https://api.github.com/repos/rausNT/jimm-chat-razr/releases?per_page=5");request.UserAgent="JimmChatSetup/"+Current;request.Timeout=5000;request.ReadWriteTimeout=5000;request.AllowAutoRedirect=false;
  using(var response=request.GetResponse())using(var reader=new StreamReader(response.GetResponseStream())){
   string json=reader.ReadToEnd();if(json.Length>262144)throw new IOException("Release response too large");
   var serializer=new JavaScriptSerializer{MaxJsonLength=262144};var releases=serializer.DeserializeObject(json) as object[];if(releases==null)return null;
   Version current=new Version(Current),best=current;
   foreach(var item in releases){var r=item as Dictionary<string,object>;if(r==null||!r.ContainsKey("tag_name")||(r.ContainsKey("draft")&&(bool)r["draft"]))continue;string tag=r["tag_name"] as string;Version candidate;if(tag!=null&&tag.StartsWith("v")&&Version.TryParse(tag.Substring(1),out candidate)&&candidate>best)best=candidate;}
   return best>current?best.ToString():null;
  }
 }
}
