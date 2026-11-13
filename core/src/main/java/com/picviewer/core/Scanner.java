package com.picviewer.core;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** A folder is replaced only after its entire directory listing succeeded. */
public final class Scanner {
 public static final class Entry {public final String name;public final boolean directory,link;public final long size,modified;public Entry(String n,boolean d,boolean l,long s,long m){name=n;directory=d;link=l;size=s;modified=m;}}
 public interface Source {List<Entry> list(String path) throws Exception;}
 public interface Progress {void onProgress(int folders,int photos);}
 public static final class Result {public final List<Photo> photos;public final Set<String> folders,failed;public final int visited;public final boolean cancelled;Result(List<Photo> p,Set<String> f,Set<String> e,int v,boolean c){photos=p;folders=f;failed=e;visited=v;cancelled=c;}public boolean complete(){return failed.isEmpty()&&!cancelled;}}
 public static Result scan(Source source,String root,List<Photo> old,Set<String> oldFolders,AtomicBoolean stop,Progress progress){
  root=Paths.normalize(root);Map<String,Photo> out=new LinkedHashMap<>();for(Photo p:old)if(Paths.under(p.path,root))out.put(p.path,p);
  Set<String> dirs=new TreeSet<>(Paths::natural);dirs.addAll(oldFolders);dirs.add(root);Set<String> failed=new LinkedHashSet<>(),seen=new HashSet<>();ArrayDeque<String> q=new ArrayDeque<>();q.add(root);int count=0;
  while(!q.isEmpty()&&!stop.get()){
   String folder=q.remove();if(!seen.add(folder))continue;if(seen.size()>100000){failed.add(folder);break;}
   try{
    List<Entry> entries=source.list(folder);if(stop.get())break;
    Set<String> children=new HashSet<>();Map<String,Photo> replacement=new LinkedHashMap<>();
    for(Entry e:entries){if(e.name.equals(".")||e.name.equals("..")||e.link||e.name.indexOf('/')>=0||e.name.indexOf('\\')>=0)continue;String p=Paths.join(folder,e.name);if(e.directory){children.add(p);q.add(p);}else if(Photo.candidate(p))replacement.put(p,new Photo(p,e.size,e.modified));}
    out.values().removeIf(p->p.folder().equals(folder));out.putAll(replacement);
    // A missing child subtree is deleted only when its parent listing succeeded.
    List<String> removed=new ArrayList<>();for(String dir:dirs)if(!dir.equals(root)&&Paths.parent(dir).equals(folder)&&!children.contains(dir))removed.add(dir);
    for(String gone:removed){out.values().removeIf(p->Paths.under(p.path,gone));dirs.removeIf(p->Paths.under(p,gone));}
    dirs.add(folder);dirs.addAll(children);count++;if(progress!=null)progress.onProgress(count,out.size());
   }catch(Exception e){failed.add(folder);}
  }
  List<Photo> photos=new ArrayList<>(out.values());photos.sort((a,b)->Paths.natural(a.path,b.path));return new Result(photos,dirs,failed,count,stop.get());
 }
}
