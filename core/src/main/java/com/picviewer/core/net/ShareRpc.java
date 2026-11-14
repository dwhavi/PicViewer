package com.picviewer.core.net;

import java.io.*;import java.nio.*;import java.nio.charset.*;import java.util.*;
/** Narrow DCE/RPC v5 + NDR32 codec for SRVSVC NetrShareEnum level 1, not a general RPC client. */
public final class ShareRpc {
 private ShareRpc(){}
 public static final int MAX_STUB=1024*1024,MAX_ENTRIES=4096;
 public static final class Share {public final String name;public final int type;public Share(String n,int t){name=n;type=t;}public boolean visibleDisk(){return (type&0xffff)==0&&(type&0x80000000)==0&&!name.endsWith("$")&&!name.isEmpty()&&name.length()<=255&&name.indexOf('/')<0&&name.indexOf('\\')<0&&name.chars().noneMatch(c->c<32||c==127);}}
 public static final class Page {public final List<Share> shares;public final long resume,status;Page(List<Share>s,long r,long e){shares=s;resume=r;status=e;}}
 public static List<String> visible(List<Share> shares){TreeSet<String> names=new TreeSet<>(com.picviewer.core.Paths::natural);for(Share s:shares)if(s.visibleDisk())names.add(s.name);return new ArrayList<>(names);}
 static ByteBuffer little(int n){return ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);}
 static byte[] pdu(int type,int call,byte[] body){ByteBuffer b=little(16+body.length);b.put(new byte[]{5,0,(byte)type,3,16,0,0,0});b.putShort((short)b.capacity()).putShort((short)0).putInt(call).put(body);return b.array();}
 static void syntax(ByteBuffer b,String uuid,int major,int minor){UUID u=UUID.fromString(uuid);long m=u.getMostSignificantBits(),l=u.getLeastSignificantBits();b.putInt((int)(m>>>32)).putShort((short)(m>>>16)).putShort((short)m);for(int i=7;i>=0;i--)b.put((byte)(l>>>(i*8)));b.putShort((short)major).putShort((short)minor);}
 public static byte[] bind(int call){ByteBuffer b=little(56);b.putShort((short)16384).putShort((short)16384).putInt(0).putInt(1);b.putShort((short)0).put((byte)1).put((byte)0);syntax(b,"4b324fc8-1670-01d3-1278-5a47bf6ee188",3,0);syntax(b,"8a885d04-1ceb-11c9-9fe8-08002b104860",2,0);return pdu(11,call,b.array());}
 public static byte[] enumerate(int call,long resume){ByteBuffer stub=little(36);stub.putInt(0).putInt(1).putInt(1).putInt(0x20000).putInt(0).putInt(0).putInt(65536).putInt(0x20004).putInt((int)resume);ByteBuffer body=little(8+stub.capacity());body.putInt(stub.capacity()).putShort((short)0).putShort((short)15).put(stub.array());return pdu(0,call,body.array());}
 static int u16(byte[] p,int at){return (p[at]&255)|((p[at+1]&255)<<8);}
 public static int fragmentLength(byte[] bytes)throws IOException{if(bytes.length<16)throw new IOException("RPC_HEADER");if(bytes[0]!=5||bytes[1]!=0||bytes[4]!=16||bytes[5]!=0||bytes[6]!=0||bytes[7]!=0)throw new IOException("RPC_REPRESENTATION");int n=u16(bytes,8);if(n<16||n>65535)throw new IOException("RPC_LENGTH");return n;}
 static void header(byte[] p,int call,int type)throws IOException{if(fragmentLength(p)!=p.length||u16(p,10)!=0||p[2]!=type||ByteBuffer.wrap(p,12,4).order(ByteOrder.LITTLE_ENDIAN).getInt()!=call)throw new IOException("RPC_RESPONSE");}
 public static void checkBind(byte[] p,int call)throws IOException{header(p,call,12);if((p[3]&3)!=3||p.length<30)throw new IOException("RPC_BIND");int sec=u16(p,24),pos=(26+sec+3)&~3;if(pos+28>p.length||p[pos]!=1||u16(p,pos+4)!=0)throw new IOException("RPC_BIND_REJECTED");ByteBuffer expected=little(20);syntax(expected,"8a885d04-1ceb-11c9-9fe8-08002b104860",2,0);if(!Arrays.equals(expected.array(),Arrays.copyOfRange(p,pos+8,pos+28)))throw new IOException("RPC_NDR_UNSUPPORTED");}
 public static final class Reassembler {
  final int call;int count=0;boolean done=false;ByteArrayOutputStream stub=new ByteArrayOutputStream();public Reassembler(int c){call=c;}
  public boolean add(byte[] fragment)throws IOException{if(done||++count>128)throw new IOException("RPC_FRAGMENTS");header(fragment,call,2);int flags=fragment[3]&255;if(((flags&1)!=0)!=(count==1)||(flags&~3)!=0||fragment.length<24||u16(fragment,20)!=0)throw new IOException("RPC_FRAGMENT");if(stub.size()+fragment.length-24>MAX_STUB)throw new IOException("RPC_LIMIT");stub.write(fragment,24,fragment.length-24);done=(flags&2)!=0;return done;}
  public byte[] result()throws IOException{if(!done)throw new IOException("RPC_INCOMPLETE");return stub.toByteArray();}
 }
 static final class Ndr {
  final ByteBuffer b;Ndr(byte[] data)throws IOException{if(data.length>MAX_STUB)throw new IOException("RPC_LIMIT");b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);}
  long n()throws IOException{align();if(b.remaining()<4)throw new IOException("NDR_TRUNCATED");return Integer.toUnsignedLong(b.getInt());}
  void align()throws IOException{int p=(b.position()+3)&~3;if(p>b.limit())throw new IOException("NDR_TRUNCATED");b.position(p);}
  String string()throws IOException{long max=n(),offset=n(),actual=n();if(max>32768||actual>max||offset!=0||actual<1||actual*2>b.remaining())throw new IOException("NDR_STRING");byte[] text=new byte[(int)actual*2];b.get(text);if(text[text.length-1]!=0||text[text.length-2]!=0)throw new IOException("NDR_TERMINATOR");try{return StandardCharsets.UTF_16LE.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(text,0,text.length-2)).toString();}catch(CharacterCodingException e){throw new IOException("NDR_UTF16",e);}}
 }
 public static Page parse(byte[] data)throws IOException{Ndr n=new Ndr(data);if(n.n()!=1||n.n()!=1)throw new IOException("NDR_LEVEL");long container=n.n();List<Share> out=new ArrayList<>();if(container!=0){long count=n.n(),buffer=n.n();if(count>MAX_ENTRIES||count>0&&buffer==0)throw new IOException("NDR_COUNT");if(buffer!=0){long conformant=n.n();if(conformant!=count||count*12>n.b.remaining())throw new IOException("NDR_ARRAY");long[] names=new long[(int)count],remarks=new long[(int)count];int[] types=new int[(int)count];for(int i=0;i<count;i++){names[i]=n.n();types[i]=(int)n.n();remarks[i]=n.n();}for(int i=0;i<count;i++){String name=names[i]!=0?n.string():"";if(remarks[i]!=0)n.string();out.add(new Share(name,types[i]));}}}n.n();long resumePointer=n.n(),resume=resumePointer==0?0:n.n(),status=n.n();if(n.b.hasRemaining())throw new IOException("NDR_TRAILING");if(status!=0&&status!=234)throw new IOException("RPC_STATUS_"+status);return new Page(out,resume,status);}
}
