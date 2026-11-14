package com.picviewer.app;
import com.hierynomus.smbj.*;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.*;
import com.picviewer.core.Scanner;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.TimeUnit;

/** Uses FILE_OPEN and READ only. Does not create, rename or delete remote files. */
final class SmbSource implements PhotoSource {
 final Config config;SMBClient client;Connection connection;Session session;DiskShare disk;
 SmbSource(Config c){config=c;}
 void connect()throws Exception{
  if(disk!=null&&disk.isConnected())return;
  authenticate();disk=(DiskShare)session.connectShare(config.share);
 }
 void authenticate()throws Exception{
  if(session!=null&&connection!=null&&connection.isConnected())return;
  close();
  try(Socket s=new Socket()){s.connect(new InetSocketAddress(config.host,config.port),4500);}
  SmbConfig options=SmbConfig.builder().withTimeout(30,TimeUnit.SECONDS).withSoTimeout(0).withReadBufferSize(256*1024).withWriteBufferSize(64*1024).withSigningRequired(true).withDfsEnabled(false).build();
  client=new SMBClient(options);connection=client.connect(config.host,config.port);char[] pass=config.password.toCharArray();try{session=connection.authenticate(new AuthenticationContext(config.user,pass,config.domain));}finally{Arrays.fill(pass,'\0');}
 }
 public synchronized List<Scanner.Entry> list(String path)throws Exception{return retry(()->listOnce(path));}
 private List<Scanner.Entry> listOnce(String path)throws Exception{try{connect();List<Scanner.Entry> out=new ArrayList<>();for(FileIdBothDirectoryInformation f:disk.list(path.replace('/','\\'))){long a=f.getFileAttributes();out.add(new Scanner.Entry(f.getFileName(),(a&0x10)!=0,(a&0x400)!=0,f.getEndOfFile(),f.getLastWriteTime().toEpochMillis()));}return out;}catch(Exception e){String k=kind(e);if(k.equals("OFFLINE"))throw new Scanner.TransportUnavailable(e);if(k.equals("AUTH"))throw new SecurityException(e);throw e;}}
 void fetch(String path,File target,long max)throws Exception{fetch(path,target,max,-1,-1);}
 public synchronized void fetch(String path,File target,long max,long knownSize,long knownModified)throws Exception{retry(()->{fetchOnce(path,target,max,knownSize,knownModified);return null;});}
 private void fetchOnce(String path,File target,long max,long knownSize,long knownModified)throws Exception{
  connect();try(com.hierynomus.smbj.share.File f=disk.openFile(path.replace('/','\\'),EnumSet.of(AccessMask.FILE_READ_DATA,AccessMask.FILE_READ_ATTRIBUTES),null,EnumSet.allOf(SMB2ShareAccess.class),SMB2CreateDisposition.FILE_OPEN,EnumSet.noneOf(SMB2CreateOptions.class));OutputStream out=new BufferedOutputStream(new FileOutputStream(target))){com.hierynomus.msfscc.fileinformation.FileAllInformation before=f.getFileInformation(com.hierynomus.msfscc.fileinformation.FileAllInformation.class);long expected=before.getStandardInformation().getEndOfFile();long modified=before.getBasicInformation().getLastWriteTime().toEpochMillis();if((knownSize>=0&&knownSize!=expected)||(knownModified>=0&&knownModified!=modified))throw new IOException("FILE_CHANGED_SINCE_SCAN");if(expected>max)throw new IOException("FILE_LIMIT");byte[] b=new byte[128*1024];long offset=0;while(offset<expected){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();int n=f.read(b,offset,0,(int)Math.min(b.length,expected-offset));if(n<=0)throw new IOException("FILE_CHANGED_DURING_READ");out.write(b,0,n);offset+=n;}com.hierynomus.msfscc.fileinformation.FileAllInformation after=f.getFileInformation(com.hierynomus.msfscc.fileinformation.FileAllInformation.class);if(after.getStandardInformation().getEndOfFile()!=expected||after.getBasicInformation().getLastWriteTime().toEpochMillis()!=modified)throw new IOException("FILE_CHANGED_DURING_READ");}

 }
 interface ReadOperation<T>{T run()throws Exception;}
 <T>T retry(ReadOperation<T> op)throws Exception{for(int attempt=0;;attempt++){try{return op.run();}catch(Exception e){String type=kind(e);boolean again=attempt==0&&type.equals("OFFLINE")&&!Thread.currentThread().isInterrupted();if(type.equals("OFFLINE")||type.equals("AUTH"))close();if(!again)throw e;}}}
 static String kind(Throwable e){
  for(Throwable t=e;t!=null;t=t.getCause()){
   String s=String.valueOf(t).toUpperCase(Locale.ROOT);
   if(s.contains("LOGON_FAILURE")||s.contains("WRONG_PASSWORD")||s.contains("ACCOUNT_LOCKED")||s.contains("PASSWORD_EXPIRED")||s.contains("ACCOUNT_DISABLED"))return "AUTH";
   if(s.contains("ACCESS_DENIED"))return "DENIED";
   if(s.contains("OBJECT_NAME_NOT_FOUND")||s.contains("OBJECT_PATH_NOT_FOUND")||s.contains("NO_SUCH_FILE"))return "MISSING";
   if(s.contains("BAD_NETWORK_NAME"))return "SHARE";
   if(s.contains("FILE_LIMIT"))return "LIMIT";
   if(s.contains("FILE_CHANGED"))return "CHANGED";
   if(s.contains("NO SPACE LEFT")||s.contains("ENOSPC")||s.contains("DISK_FULL"))return "STORAGE";
   if(s.contains("SHARING_VIOLATION")||s.contains("FILE_LOCK_CONFLICT"))return "BUSY";
  }
  for(Throwable t=e;t!=null;t=t.getCause()){
   String s=String.valueOf(t).toUpperCase(Locale.ROOT);
   if(t instanceof java.net.SocketException||t instanceof java.net.SocketTimeoutException||t instanceof java.net.UnknownHostException||t instanceof java.io.EOFException||s.contains("CONNECTION REFUSED")||s.contains("CONNECTION RESET")||s.contains("BROKEN PIPE")||s.contains("TRANSPORTEXCEPTION")||s.contains("CONNECTION_CLOSED")||s.contains("CONNECTION_DISCONNECTED")||s.contains("NETWORK_NAME_DELETED")||s.contains("USER_SESSION_DELETED")||s.contains("IO_TIMEOUT")||s.contains("NETWORK_SESSION_EXPIRED"))return "OFFLINE";
  }
  return "FILE";
 }
 @Override public void close(){if(disk!=null)try{disk.close();}catch(Exception ignored){}if(connection!=null)try{connection.close();}catch(Exception ignored){}if(client!=null)try{client.close();}catch(Exception ignored){}disk=null;connection=null;client=null;session=null;}
}
