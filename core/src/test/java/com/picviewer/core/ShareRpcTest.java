package com.picviewer.core;
import com.picviewer.core.net.ShareRpc;
import org.junit.*;import static org.junit.Assert.*;import java.io.*;import java.nio.*;import java.util.*;
public class ShareRpcTest {
 static ByteBuffer b(int n){return ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);}
 static byte[] hex(String s){byte[] out=new byte[s.length()/2];for(int i=0;i<out.length;i++)out[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return out;}
 // Independently serialized by an Impacket NDR32 fixture: photos disk + IPC$ named pipe.
 static final byte[] PAGE=hex("0100000001000000c20300000200000062ac000002000000d194000000000000702f0000bae8000003000000fd910000070000000000000007000000700068006f0074006f0073000000abab060000000000000006000000680065006c006c006f00000005000000000000000500000049005000430024000000abab060000000000000006000000680065006c006c006f000000020000000000000000000000");
 void reject(byte[] bytes){try{ShareRpc.parse(bytes);fail();}catch(IOException expected){}}
 static byte[] response(int call,int flags,byte[] stub){ByteBuffer p=b(24+stub.length);p.put(new byte[]{5,0,2,(byte)flags,16,0,0,0}).putShort((short)p.capacity()).putShort((short)0).putInt(call).putInt(stub.length).putShort((short)0).putShort((short)0).put(stub);return p.array();}
 @Test public void parsesLevelOneAndFiltersAdministrative(){try{ShareRpc.Page page=ShareRpc.parse(PAGE);assertEquals(2,page.shares.size());assertEquals(0,page.status);assertEquals(Collections.singletonList("photos"),ShareRpc.visible(page.shares));}catch(Exception e){throw new AssertionError(e);}}
 @Test public void hiddenAndNonDiskSharesAreNotShown(){assertEquals(Collections.singletonList("사진"),ShareRpc.visible(Arrays.asList(new ShareRpc.Share("C$",0),new ShareRpc.Share("IPC$",3),new ShareRpc.Share("Printer",1),new ShareRpc.Share("admin",0x80000000),new ShareRpc.Share("bad\n",0),new ShareRpc.Share("사진",0))));}
 @Test public void requestHasOnlyEnumerateLevelOne(){byte[] p=ShareRpc.enumerate(5,99);ByteBuffer bb=ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN);assertEquals(60,p.length);assertEquals(5,bb.getInt(12));assertEquals(15,bb.getShort(22));assertEquals(1,bb.getInt(28));assertEquals(99,bb.getInt(p.length-4));}
 @Test public void bindHasCorrectInterface(){byte[] p=ShareRpc.bind(1);assertEquals(72,p.length);assertEquals(11,p[2]);assertArrayEquals(hex("c84f324b7016d30112785a47bf6ee18803000000"),Arrays.copyOfRange(p,32,52));}
 @Test public void rejectsEveryTruncation(){for(int n=0;n<PAGE.length;n++)reject(Arrays.copyOf(PAGE,n));}
 @Test public void rejectsOversizedArray(){byte[] p=PAGE.clone();bput(p,12,0x7fffffff);reject(p);}
 @Test public void rejectsOversizedString(){byte[] p=PAGE.clone();bput(p,48,0x7fffffff);reject(p);}
 @Test public void rejectsLevelMismatch(){byte[] p=PAGE.clone();bput(p,0,2);reject(p);}
 @Test public void rejectsArrayMismatch(){byte[] p=PAGE.clone();bput(p,20,3);reject(p);}
 @Test public void errorReturnIsNotAnEmptySuccess(){byte[] p=PAGE.clone();bput(p,p.length-4,5);reject(p);}
 @Test public void supportsResumePage()throws Exception{byte[] p=Arrays.copyOf(PAGE,PAGE.length+4);bput(p,PAGE.length-8,0x20000);bput(p,PAGE.length-4,7);bput(p,PAGE.length,234);ShareRpc.Page r=ShareRpc.parse(p);assertEquals(234,r.status);assertEquals(7,r.resume);}
 @Test public void joinsTwoFragments()throws Exception{ShareRpc.Reassembler r=new ShareRpc.Reassembler(2);assertFalse(r.add(response(2,1,Arrays.copyOfRange(PAGE,0,35))));assertTrue(r.add(response(2,2,Arrays.copyOfRange(PAGE,35,PAGE.length))));assertArrayEquals(PAGE,r.result());}
 @Test public void rejectsWrongCallId(){try{new ShareRpc.Reassembler(2).add(response(3,3,PAGE));fail();}catch(IOException expected){}}
 @Test public void rejectsWrongFragmentOrder(){try{new ShareRpc.Reassembler(2).add(response(2,2,PAGE));fail();}catch(IOException expected){}}
 @Test public void rejectsDceAuthTrailer(){byte[] p=response(2,3,PAGE);p[10]=8;try{new ShareRpc.Reassembler(2).add(p);fail();}catch(IOException expected){}}
 @Test public void rejectsTooManyFragments()throws Exception{ShareRpc.Reassembler r=new ShareRpc.Reassembler(2);r.add(response(2,1,new byte[0]));try{for(int i=0;i<128;i++)r.add(response(2,0,new byte[0]));fail();}catch(IOException expected){}}
 @Test public void rejectsOversizedStub()throws Exception{ShareRpc.Reassembler r=new ShareRpc.Reassembler(2);r.add(response(2,1,new byte[32000]));try{for(int i=0;i<40;i++)r.add(response(2,0,new byte[32000]));fail();}catch(IOException expected){}}
 @Test public void boundedMalformedNdr()throws Exception{Random rand=new Random(20261114);for(int i=0;i<500;i++){byte[] p=new byte[rand.nextInt(512)];rand.nextBytes(p);if(i%2==0){p=PAGE.clone();p[rand.nextInt(p.length)]=(byte)rand.nextInt(256);}try{ShareRpc.parse(p);}catch(IOException expected){}}}
 @Test public void rejectsInvalidBindAck(){try{ShareRpc.checkBind(new byte[32],1);fail();}catch(IOException expected){}}
 @Test public void completedResponseCannotAcceptMore()throws Exception{ShareRpc.Reassembler r=new ShareRpc.Reassembler(2);r.add(response(2,3,PAGE));try{r.add(response(2,3,PAGE));fail();}catch(IOException expected){}}
 @Test public void incompleteResponseCannotBeRead()throws Exception{ShareRpc.Reassembler r=new ShareRpc.Reassembler(2);r.add(response(2,1,PAGE));try{r.result();fail();}catch(IOException expected){}}
 static void bput(byte[] p,int at,int value){ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN).putInt(at,value);}
}
