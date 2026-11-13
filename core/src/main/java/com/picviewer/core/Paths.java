package com.picviewer.core;
import java.util.*;
public final class Paths {
 private Paths(){}
 public static String normalize(String input){String s=input==null?"":input.replace('\\','/').trim();List<String> parts=new ArrayList<>();for(String p:s.split("/+")){if(p.isEmpty()||p.equals("."))continue;if(p.equals(".."))throw new IllegalArgumentException("상위 경로(..)는 사용할 수 없습니다");if(p.indexOf('\0')>=0||p.indexOf(':')>=0)throw new IllegalArgumentException("올바르지 않은 폴더 경로");parts.add(p);}return String.join("/",parts);}
 public static String parent(String p){p=normalize(p);int i=p.lastIndexOf('/');return i<0?"":p.substring(0,i);}
 public static boolean under(String path,String parent){return parent.isEmpty()||path.equals(parent)||path.startsWith(parent+"/");}
 public static String join(String a,String b){return normalize(a+"/"+b);}
 public static int depth(String p){return p.isEmpty()?0:p.split("/").length;}
 /** Digit sequences compare numerically without parsing or integer overflow. */
 public static int natural(String a,String b){int i=0,j=0;while(i<a.length()&&j<b.length()){char x=Character.toLowerCase(a.charAt(i)),y=Character.toLowerCase(b.charAt(j));if(x>='0'&&x<='9'&&y>='0'&&y<='9'){int ai=i,bj=j;while(i<a.length()&&Character.isDigit(a.charAt(i)))i++;while(j<b.length()&&Character.isDigit(b.charAt(j)))j++;String as=a.substring(ai,i).replaceFirst("^0+(?!$)",""),bs=b.substring(bj,j).replaceFirst("^0+(?!$)","");int c=Integer.compare(as.length(),bs.length());if(c==0)c=as.compareTo(bs);if(c!=0)return c;}else{if(x!=y)return Character.compare(x,y);i++;j++;}}int c=Integer.compare(a.length(),b.length());return c==0?a.compareTo(b):c;}
}
