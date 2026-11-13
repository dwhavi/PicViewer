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
final class SmbSource implements Scanner.Source,AutoCloseable {
 final Config config;SMBClient client;Connection connection;Session session;DiskShare disk;
 SmbSource(Config c){config=c;}
 void connect()throws Exception{
  if(disk!=null&&disk.isConnected())return;
  close();
  try(Socket s=new Socket()){s.connect(new InetSocketAddress(config.host,config.port),4500);}
  SmbConfig options=SmbConfig.builder().withTimeout(8,TimeUnit.SECONDS).withSoTimeout(8000).withReadBufferSize(256*1024).withWriteBufferSize(64*1024).withSigningRequired(true).withDfsEnabled(false).build();
  client=new SMBClient(options);connection=client.connect(config.host,config.port);char[] pass=config.password.toCharArray();try{session=connection.authenticate(new AuthenticationContext(config.user,pass,config.domain));}finally{Arrays.fill(pass,'\0');}
  disk=(DiskShare)session.connectShare(config.share);
 }
 public List<Scanner.Entry> list(String path)throws Exception{connect();List<Scanner.Entry> out=new ArrayList<>();for(FileIdBothDirectoryInformation f:disk.list(path.replace('/','\\'))){long a=f.getFileAttributes();out.add(new Scanner.Entry(f.getFileName(),(a&0x10)!=0,(a&0x400)!=0,f.getEndOfFile(),f.getLastWriteTime().toEpochMillis()));}return out;}
 void fetch(String path,File target,long max)throws Exception{
  connect();try(com.hierynomus.smbj.share.File f=disk.openFile(path.replace('/','\\'),EnumSet.of(AccessMask.FILE_READ_DATA,AccessMask.FILE_READ_ATTRIBUTES),null,EnumSet.allOf(SMB2ShareAccess.class),SMB2CreateDisposition.FILE_OPEN,EnumSet.noneOf(SMB2CreateOptions.class));OutputStream out=new BufferedOutputStream(new FileOutputStream(target))){long expected=f.getFileInformation(com.hierynomus.msfscc.fileinformation.FileStandardInformation.class).getEndOfFile();if(expected>max)throw new IOException("FILE_LIMIT");byte[] b=new byte[128*1024];long offset=0;while(offset<expected){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();int n=f.read(b,offset,0,(int)Math.min(b.length,expected-offset));if(n<=0)throw new IOException("FILE_CHANGED_DURING_READ");out.write(b,0,n);offset+=n;}long after=f.getFileInformation(com.hierynomus.msfscc.fileinformation.FileStandardInformation.class).getEndOfFile();if(after!=expected)throw new IOException("FILE_CHANGED_DURING_READ");}

 }
 static String kind(Throwable e){String s=String.valueOf(e).toUpperCase(Locale.ROOT);if(s.contains("LOGON_FAILURE")||s.contains("WRONG_PASSWORD")||s.contains("ACCOUNT_LOCKED")||s.contains("PASSWORD_EXPIRED")||s.contains("ACCOUNT_DISABLED"))return "AUTH";if(s.contains("ACCESS_DENIED"))return "DENIED";if(s.contains("OBJECT_NAME_NOT_FOUND")||s.contains("OBJECT_PATH_NOT_FOUND")||s.contains("NO_SUCH_FILE"))return "MISSING";if(s.contains("BAD_NETWORK_NAME"))return "SHARE";if(s.contains("FILE_LIMIT"))return "LIMIT";return "OFFLINE";}
 @Override public void close(){if(disk!=null)try{disk.close();}catch(Exception ignored){}if(connection!=null)try{connection.close();}catch(Exception ignored){}if(client!=null)try{client.close();}catch(Exception ignored){}disk=null;connection=null;client=null;session=null;}
}
