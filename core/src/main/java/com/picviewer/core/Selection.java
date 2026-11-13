package com.picviewer.core;
import java.util.*;
public final class Selection {
 public final Map<String,Boolean> rules=new LinkedHashMap<>(); public boolean recursive=true;
 public boolean includes(String folder){Boolean answer=false;int depth=-1;for(Map.Entry<String,Boolean> e:rules.entrySet()){String p=e.getKey();if((recursive?Paths.under(folder,p):folder.equals(p))&&p.length()>depth){answer=e.getValue();depth=p.length();}}return answer;}
 public void set(String folder,boolean selected){folder=Paths.normalize(folder);final String f=folder;rules.keySet().removeIf(p->Paths.under(p,f));rules.put(folder,selected);}
 public void all(String root,boolean selected){rules.clear();rules.put(Paths.normalize(root),selected);}
 /** 0 = off, 1 = all, 2 = mixed; includes empty directories. */
 public int state(String folder,Set<String> folders){boolean yes=false,no=false;for(String p:folders)if(p.equals(folder)||(recursive&&Paths.under(p,folder))){if(includes(p))yes=true;else no=true;}if(!yes&&!no)return includes(folder)?1:0;return yes?(no?2:1):0;}
 public List<Photo> filter(List<Photo> photos){List<Photo> result=new ArrayList<>();Set<String> paths=new HashSet<>();for(Photo p:photos)if(includes(p.folder())&&paths.add(p.path))result.add(p);return result;}
 public int roots(){int n=0;for(boolean v:rules.values())if(v)n++;return n;}
}
