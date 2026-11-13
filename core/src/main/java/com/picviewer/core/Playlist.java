package com.picviewer.core;
import java.util.*;
public final class Playlist {
 public final List<String> order=new ArrayList<>(); public int index=0; public boolean random;public long cycle=0; private long seed;
 public Playlist(boolean random,long seed){this.random=random;this.seed=seed;}
 public void reset(List<Photo> photos,String previous){order.clear();List<Photo> sorted=new ArrayList<>(photos);sorted.sort((a,b)->{int c=Paths.natural(a.name(),b.name());return c!=0?c:Paths.natural(a.path,b.path);});Set<String> seen=new HashSet<>();for(Photo p:sorted)if(seen.add(p.path))order.add(p.path);if(random){Collections.shuffle(order,new Random(seed+cycle++));if(order.size()>1&&Objects.equals(order.get(0),previous))Collections.swap(order,0,1);}index=0;}
 public String current(){return order.isEmpty()?null:order.get(Math.min(Math.max(index,0),order.size()-1));}
 public String next(List<Photo> latest){String before=current();if(order.isEmpty()){reset(latest,null);return current();}if(++index>=order.size())reset(latest,before);return current();}
 public String previous(){if(!order.isEmpty())index=(index-1+order.size())%order.size();return current();}
 /** New files wait for the next cycle; confirmed deletions disappear immediately. */
 public void reconcile(List<Photo> latest){Set<String> paths=new HashSet<>();for(Photo p:latest)paths.add(p.path);String current=current();int oldIndex=index;order.removeIf(p->!paths.contains(p));int i=order.indexOf(current);index=i>=0?i:Math.min(oldIndex,Math.max(0,order.size()-1));}
 public long seed(){return seed;}
 public void restore(List<String> paths,int position,long cycle){order.clear();order.addAll(paths);index=Math.max(0,Math.min(position,order.size()-1));this.cycle=cycle;}
}
