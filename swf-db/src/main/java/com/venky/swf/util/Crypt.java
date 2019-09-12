package com.venky.swf.util;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Crypt {
    private static Crypt crypt;
    private Crypt(){

    }

    public static Crypt getInstance() {
        if (crypt != null) {
            return crypt;
        }
        synchronized (Crypt.class) {
            if (crypt == null) {
                crypt = new Crypt();
            }
        }
        return crypt;
    }


    public String getBase64Encoded(Key key){
        byte[] encoded = key.getEncoded();
        String b64Key = Base64.getEncoder().encodeToString(encoded);
        return b64Key;
    }

    public PublicKey getPublicKey(String base64PublicKey){
        byte [] binCpk = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(binCpk);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGO);
            PublicKey pKey = keyFactory.generatePublic(pkSpec);
            return pKey;
        }catch (NoSuchAlgorithmException | InvalidKeySpecException ex){
            throw new RuntimeException(ex);
        }
    }
    public static final String KEY_ALGO = "RSA";

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGO);
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        }catch (NoSuchAlgorithmException ex){
            throw new RuntimeException(ex);
        }
    }


    public String generateSignature(String payload, PrivateKey privateKey) throws  Exception{
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }
    public boolean verifySignature(String payload, String signature, String base64PublicKey)throws Exception{
        return verifySignature(payload,signature,getPublicKey(base64PublicKey));

    }
    public boolean verifySignature(String payload, String signature, PublicKey pKey)throws Exception{
        byte [] data = payload.getBytes();
        byte [] signatureBytes = Base64.getDecoder().decode(signature);
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(pKey);
        s.update(data);
        return s.verify(signatureBytes);

    }

}
