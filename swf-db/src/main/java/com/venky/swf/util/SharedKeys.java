package com.venky.swf.util;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

public class SharedKeys {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null){
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SharedKeys(){
        try {
            ensureKeyStore();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    KeyStore.SecretKeyEntry entry = null;
    private void ensureKeyStore() throws Exception{
        String alias = "succinct";
        String keyStoreDirectory = Config.instance().getProperty("swf.key.store.directory");
        String keyStorePassword = Config.instance().getProperty("swf.key.store.password");
        if (ObjectUtil.isVoid(keyStoreDirectory) || ObjectUtil.isVoid(keyStorePassword)){
            Config.instance().setProperty("swf.encryption.support","false");
            return;
        }

        File dirStore = new File(keyStoreDirectory);
        if (!dirStore.exists()){
            dirStore.mkdirs();
        }
        File keyStoreFile = new File(dirStore,"keystore.ks");

        String keyPassword = Config.instance().getProperty("swf.key.entry."+alias+".password");
        KeyStore.ProtectionParameter entryPassword =
                new KeyStore.PasswordProtection(keyPassword.toCharArray());

        KeyStore keyStore = null;
        keyStore = KeyStore.getInstance("jceks");
        if (keyStoreFile.exists()){
            FileInputStream is = null;
            try  {
                is = new FileInputStream(keyStoreFile);
                keyStore.load(is, keyStorePassword.toCharArray());
                entry =  (SecretKeyEntry) keyStore.getEntry(alias,entryPassword);
            }finally {
                if (is != null){
                    is.close();
                }
            }
        }else {
            keyStore.load(null,keyStorePassword.toCharArray());
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

            SecureRandom secureRandom = new SecureRandom();
            int keyBitSize = 256;
            keyGenerator.init(keyBitSize, secureRandom);

            SecretKey secretKey = keyGenerator.generateKey();
            entry = new SecretKeyEntry(secretKey);

            keyStore.setEntry(alias,entry,entryPassword);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(keyStoreFile);
                keyStore.store(fileOutputStream, keyStorePassword.toCharArray());
            }finally {
                if (fileOutputStream != null){
                    fileOutputStream.close();
                }
            }
        }
    }

    private static SharedKeys instance= null;

    public static SharedKeys getInstance(){
        if (instance != null){
            return instance;
        }
        synchronized (SharedKeys.class){
            if (instance == null) {
                instance = new SharedKeys();
            }
        }
        return instance;
    }

    private ThreadLocal<Boolean> enableEncryption = new ThreadLocal<>();
    public void setEnableEncryption(boolean enableEncryption){
        this.enableEncryption.set(enableEncryption);
    }

    public boolean isEncryptionEnabled(){
        if (enableEncryption.get() == null){
            enableEncryption.set(Config.instance().getBooleanProperty("swf.encryption.support",true));
        }
        return this.enableEncryption.get();
    }


    ThreadLocal<Cipher> cipherEncryptHolder =  new ThreadLocal<>();
    ThreadLocal<Cipher> cipherDecryptHolder =  new ThreadLocal<>();

    private Cipher getEncryptCipher(){
        Cipher cipher = cipherEncryptHolder.get();
        if (cipher == null ){
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, entry.getSecretKey());
                cipherEncryptHolder.set(cipher);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        return cipher;
    }
    private Cipher getDecryptCipher(){
        Cipher cipher = cipherDecryptHolder.get();
        if (cipher == null ){
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey());
                cipherDecryptHolder.set(cipher);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        return cipher;
    }

    public String   encrypt(String decrypted){
        try {
            if (decrypted == null || !isEncryptionEnabled()){
                return decrypted;
            }
            Cipher cipher= getEncryptCipher();
            byte[] encrypted = cipher.doFinal(decrypted.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    public String decrypt(String encrypted){
        try {
            if (encrypted == null || !isEncryptionEnabled()){
                return encrypted;
            }
            Cipher cipher = getDecryptCipher();
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted.getBytes(StandardCharsets.UTF_8)));
            return new String(decrypted);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
