package com.picviewer.app;
import com.picviewer.core.net.ShareRpc;
import com.hierynomus.smbj.share.*;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.*;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;

/** Authenticated share enumeration only. No file writes or arbitrary RPC operations. */
final class ShareBrowser {
 static final class Job implements AutoCloseable {
  final AtomicBoolean cancelled=new AtomicBoolean();volatile SmbSource source;
  void check()throws IOException{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("CANCELLED");}
  public void close(){cancelled.set(true);SmbSource s=source;if(s!=null){Thread t=new Thread(s::close,"share-lookup-close");t.setDaemon(true);t.start();}}
 }
 static List<String> find(Config cfg,Job job)throws Exception{
  try(SmbSource source=new SmbSource(cfg)){job.source=source;job.check();source.authenticate();job.check();
   try(PipeShare ipc=(PipeShare)source.session.connectShare("IPC$");NamedPipe pipe=ipc.open("srvsvc",SMB2ImpersonationLevel.Impersonation,EnumSet.of(AccessMask.GENERIC_READ,AccessMask.GENERIC_WRITE),null,EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ,SMB2ShareAccess.FILE_SHARE_WRITE),SMB2CreateDisposition.FILE_OPEN,EnumSet.noneOf(SMB2CreateOptions.class))){
    Wire wire=new Wire(pipe,job);wire.send(ShareRpc.bind(1));ShareRpc.checkBind(wire.fragment(),1);List<ShareRpc.Share> entries=new ArrayList<>();Set<Long> resumes=new HashSet<>();long resume=0;int call=2;
    do{job.check();if(!resumes.add(resume)||call>33)throw new IOException("RPC_PAGE_LIMIT");wire.send(ShareRpc.enumerate(call,resume));ShareRpc.Reassembler join=new ShareRpc.Reassembler(call++);while(!join.add(wire.fragment()))job.check();ShareRpc.Page page=ShareRpc.parse(join.result());entries.addAll(page.shares);if(entries.size()>ShareRpc.MAX_ENTRIES)throw new IOException("RPC_SHARE_LIMIT");if(page.status==0)return ShareRpc.visible(entries);resume=page.resume;}while(true);
   }
  }finally{job.source=null;}
 }
 static final class Wire {
  final NamedPipe pipe;final Job job;byte[] pending=new byte[0];int cursor=0;final long deadline=System.nanoTime()/1000000L+20000;long total=0;
  Wire(NamedPipe p,Job j){pipe=p;job=j;}
  void check()throws IOException{job.check();if(System.nanoTime()/1000000L>deadline)throw new IOException("RPC_TIMEOUT");}
  void send(byte[] data)throws IOException{check();if(cursor!=pending.length)throw new IOException("RPC_EXTRA_DATA");if(pipe.write(data)!=data.length)throw new IOException("RPC_SHORT_WRITE");}
  byte[] exact(int n)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream(n);while(out.size()<n){check();if(cursor==pending.length){pending=new byte[16384];int count=pipe.read(pending);if(count<=0)throw new EOFException("RPC_EOF");pending=Arrays.copyOf(pending,count);cursor=0;total+=count;if(total>2L*ShareRpc.MAX_STUB)throw new IOException("RPC_TRANSFER_LIMIT");}int count=Math.min(n-out.size(),pending.length-cursor);out.write(pending,cursor,count);cursor+=count;}return out.toByteArray();}
  byte[] fragment()throws IOException{byte[] header=exact(16);int length=ShareRpc.fragmentLength(header);byte[] packet=Arrays.copyOf(header,length);byte[] rest=exact(length-16);System.arraycopy(rest,0,packet,16,rest.length);return packet;}
 }
}
