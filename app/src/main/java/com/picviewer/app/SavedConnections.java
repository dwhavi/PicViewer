package com.picviewer.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import java.io.IOException;

/** Local-only encrypted profile collection. No export, backup or cloud sync. */
final class SavedConnections {
 interface Cipher {String apply(boolean encrypt,String text)throws Exception;}
 static final int LIMIT=20;
 final SharedPreferences prefs;final Cipher cipher;
 SavedConnections(Context context){this(context,Config::crypt);}
 SavedConnections(Context context,Cipher cipher){prefs=context.getSharedPreferences("saved-connections",0);this.cipher=cipher;}
 static final class Profile {final String id,name;final Config config;Profile(String id,String name,Config config){this.id=id;this.name=name;this.config=config;}}
 List<Profile> all()throws Exception{String blob=prefs.getString("encrypted","");List<Profile> result=new ArrayList<>();if(blob.isEmpty())return result;JSONArray array=new JSONArray(cipher.apply(false,blob));if(array.length()>LIMIT)throw new IOException("PROFILE_LIMIT");for(int i=0;i<array.length();i++){JSONObject o=array.getJSONObject(i);result.add(new Profile(o.getString("id"),o.getString("name"),decode(o.getJSONObject("connection"))));}return result;}
 void save(String name,Config config)throws Exception{if(!config.ready()||config.local())throw new IOException("PROFILE_INCOMPLETE");name=name.trim();if(name.isEmpty())name=config.host+" / "+config.share;if(name.length()>80)throw new IOException("PROFILE_NAME_LIMIT");List<Profile> profiles=all();String id=UUID.randomUUID().toString();for(Iterator<Profile> it=profiles.iterator();it.hasNext();){Profile p=it.next();if(p.config.source().equals(config.source())){id=p.id;it.remove();}}if(profiles.size()>=LIMIT)throw new IOException("PROFILE_LIMIT");profiles.add(0,new Profile(id,name,config));write(profiles);}
 void remove(String id)throws Exception{List<Profile> profiles=all();profiles.removeIf(p->p.id.equals(id));write(profiles);}
 void write(List<Profile> profiles)throws Exception{JSONArray array=new JSONArray();for(Profile p:profiles)array.put(new JSONObject().put("id",p.id).put("name",p.name).put("connection",encode(p.config)));String encrypted=cipher.apply(true,array.toString());if(!prefs.edit().putString("encrypted",encrypted).commit())throw new IOException("SETTINGS_WRITE");}
 static JSONObject encode(Config c)throws JSONException{return new JSONObject().put("host",c.host).put("share",c.share).put("root",c.root).put("user",c.user).put("password",c.password).put("domain",c.domain).put("port",c.port);}
 static Config decode(JSONObject o)throws JSONException{Config c=new Config();c.host=o.getString("host");c.share=o.getString("share");c.root=o.getString("root");c.user=o.getString("user");c.password=o.getString("password");c.domain=o.getString("domain");c.port=o.getInt("port");if(!c.ready()||c.port<1||c.port>65535)throw new JSONException("PROFILE_INVALID");return c;}
}
