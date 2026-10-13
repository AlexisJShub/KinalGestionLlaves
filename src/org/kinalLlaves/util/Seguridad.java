package org.kinalllaves.util;
import java.security.*;import java.util.*;import javax.crypto.*;import javax.crypto.spec.PBEKeySpec;
public final class Seguridad {
 private Seguridad(){}
 private static final int ITER=210000,LEN=256;
 public static String hash(String clave){if(clave==null||clave.length()<8)throw new IllegalArgumentException("La clave debe tener 8 o más caracteres");
  byte[] sal=new byte[16];new SecureRandom().nextBytes(sal);return "pbkdf2$"+ITER+"$"+Base64.getEncoder().encodeToString(sal)+"$"+Base64.getEncoder().encodeToString(derivar(clave.toCharArray(),sal,ITER));}
 public static boolean verificar(String clave,String guardado){
  if(clave==null||guardado==null)return false;
  try{String[] p=guardado.split("\\$");if(p.length!=4||!"pbkdf2".equals(p[0]))return false;
   int vueltas=Integer.parseInt(p[1]);if(vueltas<10000||vueltas>1000000)return false;
   byte[] salt=Base64.getDecoder().decode(p[2]);byte[] expected=Base64.getDecoder().decode(p[3]);
   return MessageDigest.isEqual(expected,derivar(clave.toCharArray(),salt,vueltas));
  }catch(RuntimeException ex){return false;}
 }
 private static byte[] derivar(char[] clave,byte[] salt,int iter){PBEKeySpec spec=new PBEKeySpec(clave,salt,iter,LEN);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  catch(GeneralSecurityException ex){throw new IllegalStateException("Algoritmo no disponible",ex);}finally{spec.clearPassword();Arrays.fill(clave,'\0');}}
}
