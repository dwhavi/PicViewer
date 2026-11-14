package com.picviewer.app;

import android.text.*;
import android.widget.EditText;

/** Only assists single-key entry at the end. Paste, cursor edits and deletion stay literal. */
final class IpTyping implements TextWatcher {
 final EditText field;boolean changing;int start,removed,added;String before="";String autoPrefix=null;final java.util.List<Integer> inserted=new java.util.ArrayList<>();
 IpTyping(EditText field){this.field=field;}
 public void beforeTextChanged(CharSequence s,int st,int count,int after){if(changing)return;before=s.toString();start=st;removed=count;added=after;}
 public void onTextChanged(CharSequence s,int st,int old,int count){}
 public void afterTextChanged(Editable e){if(changing)return;String now=e.toString();String pending=autoPrefix;autoPrefix=null;
  if(removed!=0||added!=1||start!=before.length()){inserted.clear();return;}
  char c=now.charAt(now.length()-1);
  // A manually typed dot confirms our dot; a letter makes a numeric-prefix hostname literal.
  if(Character.isLetter(c)||c=='-'||c=='_'){StringBuilder literal=new StringBuilder(now);for(int i=inserted.size()-1;i>=0;i--)literal.deleteCharAt(inserted.get(i));inserted.clear();replace(e,literal.toString());return;}
  if(pending!=null&&before.equals(pending+".")&&c=='.'){inserted.remove(Integer.valueOf(before.length()-1));replace(e,before);return;}
  if(c<'0'||c>'9'||!now.matches("[0-9.]+"))return;
  String[] parts=now.split("\\.",-1);if(parts.length>=4)return;
  for(String part:parts)if(part.isEmpty()||part.length()>3||Integer.parseInt(part)>255)return;
  if(parts[parts.length-1].length()==3){autoPrefix=now;inserted.add(now.length());replace(e,now+".");}
 }
 void replace(Editable e,String value){changing=true;try{e.replace(0,e.length(),value);field.setSelection(value.length());}finally{changing=false;}}
}
