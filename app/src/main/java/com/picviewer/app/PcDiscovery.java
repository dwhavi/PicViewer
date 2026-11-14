package com.picviewer.app;

import android.content.Context;import android.net.*;import android.net.wifi.WifiManager;
import org.xmlpull.v1.*;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, foreground-only WS-Discovery probe. Reply addresses are never fetched as URLs. */
final class PcDiscovery {
 static final String SOAP="http://www.w3.org/2003/05/soap-envelope",ADDRESSING="http://schemas.xmlsoap.org/ws/2004/08/addressing",DISCOVERY="http://schemas.xmlsoap.org/ws/2005/04/discovery",COMPUTER="http://schemas.microsoft.com/windows/2006/08/wdp/pub";
 static final int MAX_PACKET=32768;
 static String probe(String id){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><s:Envelope xmlns:s=\""+SOAP+"\" xmlns:a=\""+ADDRESSING+"\" xmlns:d=\""+DISCOVERY+"\" xmlns:p=\""+COMPUTER+"\"><s:Header><a:Action>"+DISCOVERY+"/Probe</a:Action><a:MessageID>"+id+"</a:MessageID><a:ReplyTo><a:Address>"+ADDRESSING+"/role/anonymous</a:Address></a:ReplyTo><a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To></s:Header><s:Body><d:Probe><d:Types>p:Computer</d:Types></d:Probe></s:Body></s:Envelope>";}
 static boolean reply(byte[] bytes,int length,String id)throws Exception{
  if(length<=0||length>MAX_PACKET)return false;String xml=new String(bytes,0,length,StandardCharsets.UTF_8);String upper=xml.toUpperCase(Locale.ROOT);if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY"))return false;
  XmlPullParserFactory factory=XmlPullParserFactory.newInstance();factory.setNamespaceAware(true);XmlPullParser parser=factory.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);parser.setInput(new StringReader(xml));boolean action=false,related=false,computer=false,matches=false;int count=0,matchDepth=-1;
  for(int event=parser.next();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
   if(++count>4096||parser.getDepth()>32)return false;
   if(event==XmlPullParser.START_TAG){String ns=parser.getNamespace(),name=parser.getName();if(parser.getDepth()==1&&(!SOAP.equals(ns)||!"Envelope".equals(name)))return false;
    if(ADDRESSING.equals(ns)&&name.equals("Action"))action=(DISCOVERY+"/ProbeMatches").equals(parser.nextText().trim());
    else if(ADDRESSING.equals(ns)&&name.equals("RelatesTo"))related=id.equals(parser.nextText().trim());
    else if(DISCOVERY.equals(ns)&&name.equals("ProbeMatch")){matches=true;matchDepth=parser.getDepth();}
    else if(DISCOVERY.equals(ns)&&name.equals("Types")&&matchDepth>0){String types=parser.nextText();for(String type:types.trim().split("\\s+")){int colon=type.indexOf(':');String prefix=colon<0?"":type.substring(0,colon),local=type.substring(colon+1);if(local.equals("Computer")&&COMPUTER.equals(parser.getNamespace(prefix)))computer=true;}}
   }else if(event==XmlPullParser.END_TAG&&parser.getDepth()==matchDepth)matchDepth=-1;
  }
  return action&&related&&matches&&computer;
 }
 static boolean sameSubnet(InetAddress local,int prefix,InetAddress remote){byte[] a=local.getAddress(),b=remote.getAddress();if(a.length!=4||b.length!=4||prefix<1||prefix>32||remote.isAnyLocalAddress()||remote.isLoopbackAddress()||remote.isMulticastAddress()||remote.equals(local))return false;for(int i=0;i<4;i++){int bits=Math.min(8,Math.max(0,prefix-i*8)),mask=bits==0?0:(255<<(8-bits))&255;if((a[i]&mask)!=(b[i]&mask))return false;}int hostBits=32-prefix;if(hostBits>1){long host=0;for(byte v:b)host=(host<<8)|(v&255);long mask=(1L<<hostBits)-1;if((host&mask)==0||(host&mask)==mask)return false;}return true;}
 static final class Job implements AutoCloseable {final AtomicBoolean cancelled=new AtomicBoolean();volatile DatagramSocket socket;public void close(){cancelled.set(true);DatagramSocket s=socket;if(s!=null)s.close();}}
 static List<String> find(Context context,Job job)throws Exception{
  ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);Network active=manager.getActiveNetwork();NetworkCapabilities caps=manager.getNetworkCapabilities(active);LinkProperties link=manager.getLinkProperties(active);
  if(active==null||caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||link==null)throw new IOException("WIFI_REQUIRED");
  LinkAddress local=null;for(LinkAddress a:link.getLinkAddresses())if(a.getAddress() instanceof Inet4Address&&!a.getAddress().isLoopbackAddress()){local=a;break;}if(local==null)throw new IOException("WIFI_IPV4_REQUIRED");
  WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);WifiManager.MulticastLock lock=wifi.createMulticastLock("picviewer-discovery");lock.setReferenceCounted(false);
  try(MulticastSocket socket=new MulticastSocket(new InetSocketAddress(local.getAddress(),0))){job.socket=socket;active.bindSocket(socket);socket.setInterface(local.getAddress());socket.setTimeToLive(1);socket.setSoTimeout(350);lock.acquire();String id="urn:uuid:"+UUID.randomUUID();byte[] request=probe(id).getBytes(StandardCharsets.UTF_8);long started=System.nanoTime(),until=started+8_000_000_000L;TreeSet<String> found=new TreeSet<>();int packets=0,sent=0;
   while(!job.cancelled.get()&&System.nanoTime()<until&&packets<256&&found.size()<64){if(sent<2&&(sent==0||System.nanoTime()-started>1_000_000_000L)){socket.send(new DatagramPacket(request,request.length,InetAddress.getByName("239.255.255.250"),3702));sent++;}byte[] buffer=new byte[MAX_PACKET+1];DatagramPacket packet=new DatagramPacket(buffer,buffer.length);try{socket.receive(packet);packets++;if(sameSubnet(local.getAddress(),local.getPrefixLength(),packet.getAddress()))try{if(reply(buffer,packet.getLength(),id))found.add(packet.getAddress().getHostAddress());}catch(XmlPullParserException|IOException|IllegalArgumentException ignored){} }catch(SocketTimeoutException ignored){}}
   return new ArrayList<>(found);
  }finally{job.socket=null;if(lock.isHeld())lock.release();}
 }
}
