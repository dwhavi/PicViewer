package com.picviewer.app;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.util.*;
import com.picviewer.core.Paths;

final class Config {
 String host="",share="",root="",user="",domain="",password="";int port=445;
 boolean ready(){return !host.isEmpty()&&!share.isEmpty()&&!user.isEmpty();}
 String source(){return host.toLowerCase(Locale.ROOT)+":"+port+"/"+share+"/"+root+"/"+domain+"/"+user;}
 static Config load(Context c)throws Exception{android.content.SharedPreferences p=c.getSharedPreferences("connection",0);Config x=new Config();x.host=p.getString("host","");x.share=p.getString("share","");x.root=p.getString("root","");x.user=p.getString("user","");x.domain=p.getString("domain","");x.port=p.getInt("port",445);String encrypted=p.getString("secret","");if(!encrypted.isEmpty())x.password=crypt(false,encrypted);return x;}
 void save(Context c)throws Exception{String encrypted=crypt(true,password);boolean saved=c.getSharedPreferences("connection",0).edit().putString("host",host).putString("share",share).putString("root",Paths.normalize(root)).putString("user",user).putString("domain",domain).putInt("port",port).putString("secret",encrypted).commit();if(!saved)throw new java.io.IOException("SETTINGS_WRITE");}
 static String crypt(boolean encrypt,String value)throws Exception{
  String alias="picviewer.connection.v1";KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias(alias)){if(!encrypt)throw new Exception("저장한 암호를 열 수 없습니다. 다시 입력해 주세요.");KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");java.security.Key key=ks.getKey(alias,null);
  if(encrypt){cipher.init(Cipher.ENCRYPT_MODE,key);return Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);}
  String[] parts=value.split(":",2);cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
 }
}
