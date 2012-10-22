package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;

public class KeyExchange implements Serializable {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private KeyPair keypair;
	public KeyExchange() {
		this(256);
	}

	public KeyExchange(int keyBits) {
		try {
			ECNamedCurveParameterSpec dhSpec = ECNamedCurveTable.getParameterSpec("prime192v1");

			// Use the values to generate a key pair
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH","SC");
			keyGen.initialize(dhSpec);
			keypair = keyGen.generateKeyPair();
		} catch (Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "Exception in key exchange", e);
		}
	}
	
	public byte[] getPublicKeyBytes() {
		// somehow acquire an object...
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = null;
				byte[] yourBytes = null;
				try {
					out = new ObjectOutputStream(bos);
					out.writeObject(keypair.getPublic());
					yourBytes = bos.toByteArray();
				} catch (Exception e) {
					Logger.getLogger("touch-to-text").log(Level.SEVERE,
							"Error serializing public key", e);
				} finally {
					try {
						out.close();
						bos.close();
					} catch (Exception e) {
					} // TODO handle elegantly
				}
				return yourBytes;
	}
	
	public SecretKey setOtherPublicKey(byte[] b,String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, NoSuchProviderException {
		if(keypair == null) {
			throw new UnsupportedOperationException("Do not reuse key exchange objects.");
		}
		ECPublicKey keySpec = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			keySpec = (ECPublicKey) in.readObject();
		} catch (Exception e) {
			// TODO handle more gracefully
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Error decrypting payload", e);
		} finally {
			try {
				bis.close();
				in.close();
			} catch (Exception e) {
			}
		}
		
		KeyAgreement ka = KeyAgreement.getInstance("ECDH", "SC");
	    ka.init(keypair.getPrivate());

	    // Prepare to generate the secret key with the private key and public key of the other party
	    ka.doPhase(keySpec, true);
	    // Generate the secret key
	    keypair = null; //Ensure not reused
	    return ka.generateSecret(algorithm);
	}
}
