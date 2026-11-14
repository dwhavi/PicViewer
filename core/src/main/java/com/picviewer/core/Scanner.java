package com.picviewer.core;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** A folder is replaced only after its entire directory listing succeeded. */
public final class Scanner {
 public static final class Entry {public final String name;public final boolean directory,link;public final long size,modified;public Entry(String n,boolean d,boolean l,long s,long m){name=n;directory=d;link=l;size=s;modified=m;}}
 /** Stop the whole scan after a transport/session failure, preserving the last snapshot. */
 public static class Unavailable extends RuntimeException {public Unavailable(Throwable cause){super(cause);}}
 public static final class TransportUnavailable extends Unavailable {public TransportUnavailable(Throwable cause){super(cause);}}
 public interface Source {List<Entry> list(String path) throws Exception;}
 public interface Progress {void onProgress(int folders,int photos);}
 public static final class Result {public final List<Photo> photos;public final Set<String> folders,failed;public final int visited,skippedLinks;public final boolean cancelled;Result(List<Photo> p,Set<String> f,Set<String> e,int v,boolean c,int skipped){skippedLinks=skipped;photos=p;folders=f;failed=e;visited=v;cancelled=c;}public boolean complete(){return failed.isEmpty()&&!cancelled;}}
 public static Result scan(Source source,String root,List<Photo> old,Set<String> oldFolders,AtomicBoolean stop,Progress progress){
  root=Paths.normalize(root);Map<String,Map<String,Photo>> byFolder=new HashMap<>();int photoCount=0;for(Photo p:old)if(Paths.under(p.path,root)){Map<String,Photo> files=byFolder.computeIfAbsent(p.folder(),k->new LinkedHashMap<>());if(files.put(p.path,p)==null)photoCount++;}
  Set<String> dirs=new TreeSet<>(Paths::natural);dirs.addAll(oldFolders);dirs.add(root);Map<String,Set<String>> knownChildren=new HashMap<>();for(String d:dirs)if(!d.equals(root))knownChildren.computeIfAbsent(Paths.parent(d),k->new HashSet<>()).add(d);Set<String> failed=new LinkedHashSet<>(),seen=new HashSet<>();ArrayDeque<String> q=new ArrayDeque<>();q.add(root);int count=0,skipped=0;
  while(!q.isEmpty()&&!stop.get()){
   String folder=q.remove();if(!seen.add(folder))continue;if(seen.size()>100000){failed.add(folder);break;}
   try{
    List<Entry> entries=source.list(folder);if(stop.get())break;
    Set<String> children=new HashSet<>();Map<String,Photo> replacement=new LinkedHashMap<>();
    for(Entry e:entries){if(e.link){skipped++;continue;}if(e.name.equals(".")||e.name.equals("..")||e.link||e.name.indexOf('/')>=0||e.name.indexOf('\\')>=0)continue;String p=Paths.join(folder,e.name);if(e.directory){children.add(p);q.add(p);}else if(Photo.candidate(p))replacement.put(p,new Photo(p,e.size,e.modified));}
    Map<String,Photo> prior=byFolder.put(folder,replacement);photoCount+=replacement.size()-(prior==null?0:prior.size());
    // A missing child subtree is deleted only when its parent listing succeeded.
    List<String> removed=new ArrayList<>();for(String dir:knownChildren.getOrDefault(folder,Collections.emptySet()))if(!children.contains(dir))removed.add(dir);
    for(String gone:removed){ArrayDeque<String> prune=new ArrayDeque<>();prune.add(gone);while(!prune.isEmpty()){String d=prune.remove();Map<String,Photo> erased=byFolder.remove(d);if(erased!=null)photoCount-=erased.size();dirs.remove(d);Set<String> nested=knownChildren.remove(d);if(nested!=null)prune.addAll(nested);}}knownChildren.put(folder,children);
    dirs.add(folder);dirs.addAll(children);count++;if(progress!=null)progress.onProgress(count,photoCount);
   }catch(SecurityException e){throw e;}catch(TransportUnavailable e){failed.add(folder);failed.addAll(q);break;}catch(Unavailable e){throw e;}catch(Exception e){failed.add(folder);}
  }
  List<Photo> photos=new ArrayList<>();for(Map<String,Photo> files:byFolder.values())photos.addAll(files.values());photos.sort((a,b)->Paths.natural(a.path,b.path));return new Result(photos,dirs,failed,count,stop.get(),skipped);
 }
}
