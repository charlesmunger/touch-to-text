package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore.PasswordProtection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.crypto.generators.ElGamalParametersGenerator;
import org.spongycastle.crypto.params.ElGamalParameters;
import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;

import android.content.Context;
import android.util.Base64;

/**
 * Generates a keyring containing public and secret DSA signing
 * key and an El Gamal key for encryption.
 * 
 */
final class PGPKeys {

	private static final String PUBLIC_KEY = "publicKey";
	private PGPSecretKeyRing privateKeyRing;
	private PGPPublicKeyRing publicKeyRing;
	private PGPPublicKey publicKey;
	private PGPSecretKey privateKey;
	private SecurePreferences encryptedPublicKey;


	static {
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
	}

	/**
	 * Recreates a Public and Private keypair from the database.
	 * @param publicKey
	 * @param privateKey
	 * @throws IOException
	 * @throws PGPException
	 */
	public PGPKeys(byte[] publicKeyRingBytes, byte[] privateKeyRingBytes) throws IOException, PGPException {
		privateKeyRing = new PGPSecretKeyRing(privateKeyRingBytes);
		privateKey = privateKeyRing.getSecretKey();
		publicKeyRing = new PGPPublicKeyRing(publicKeyRingBytes);
		publicKey = publicKeyRing.getPublicKey();
	}

	/**
	 * Creates a PGPKey containing only a user's public key
	 * from the database or any byte array.
	 * @param publicKeyBytes
	 * @throws IOException
	 * @throws PGPException
	 */
	public PGPKeys(byte[] publicKeyBytes) throws IOException, PGPException {
		publicKeyRing = new PGPPublicKeyRing(publicKeyBytes);
		publicKey = publicKeyRing.getPublicKey();
	}

	/**
	 * Creates a public key with the given name and passPhrase, and 
	 * inserts it into the database.
	 * @param identity
	 * @param passPhrase
	 */
	public PGPKeys(Context c, String identity,PasswordProtection passPhrase ) {
		generateDSAElGamal(c, identity, passPhrase);
	}

	/**
	 * Returns PGP Public Key as a byte array.
	 * 
	 * @param identity
	 * @param passPhrase
	 * @return
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException
	 * @throws SignatureException
	 * @throws PGPException
	 */


	@SuppressWarnings("deprecation")
	/**
	 * Returns the public Key RING! 
	 */
	byte[] getPublicKeyRing() 
			throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {
		return publicKeyRing.getEncoded();

	}

	public byte[] getPublicKey() {
		byte[] publicKeyBytes = null;
		if(publicKeyRing != null) {
			Iterator<PGPPublicKey> publicKeyRingIter = publicKeyRing.getPublicKeys();
			// How many keys are in the ring? 
			while(publicKeyRingIter.hasNext()) {
				PGPPublicKey key = publicKeyRingIter.next();
				try {
					publicKeyBytes = key.getEncoded();
				} catch (IOException e) {
					Logger.getLogger("touch-to-text").log(Level.SEVERE,
							"Problem e public key!", e);
				}
			}
		}
		return publicKeyBytes;
	}

	// Deprecated PGPKeyPair methods but I can't find a good replacement.
	@SuppressWarnings("deprecation")
	private void storeMyKeysInDB(String identity) 
			throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {
		if(privateKey == null) {
			DatabaseHelper.getInstance(null).insertKeypair(null, publicKey.getEncoded(), identity);
		} else {
			DatabaseHelper.getInstance(null).insertKeypair(privateKey.getEncoded(), publicKey.getEncoded(), identity);

		}
	}

	private void generateDSAElGamal(Context c, String name, PasswordProtection passphrase) {
		try {
			KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance("DSA", "SC");
			dsaKpg.initialize(1024); // Should benchmark vs. 2048 bit  keys
			KeyPair dsaKp = dsaKpg.generateKeyPair();

			KeyPairGenerator elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "SC");
			ElGamalParametersGenerator a = new ElGamalParametersGenerator();
			a.init(1024, 10, new SecureRandom());
			ElGamalParameters params = a.generateParameters();
			ElGamalParameterSpec elParams = new ElGamalParameterSpec(params.getP(), params.getG());
			elgKpg.initialize(elParams);
			KeyPair elgKp = elgKpg.generateKeyPair();

			PGPKeyPair dsaKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
			PGPKeyPair elgKeyPair = new PGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());

			PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair,
					name, PGPEncryptedData.AES_256, passphrase.getPassword(), true, null, null, new SecureRandom(), "BC");

			// Now we have our DSA and El Gamal keys in one place
			keyRingGen.addSubKey(elgKeyPair);
			publicKeyRing = keyRingGen.generatePublicKeyRing();
			publicKey = publicKeyRing.getPublicKey();

			privateKeyRing = keyRingGen.generateSecretKeyRing();
			privateKey = privateKeyRing.getSecretKey();
			storeMyKeysInDB(name);	

			encryptedPublicKey = new  SecurePreferences(
					c, "touchToTexPreferences.xml",
					MasterPassword.getInstance(null).getPassword().toString(),
					true);
			encryptedPublicKey.put(PUBLIC_KEY, Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));

		} catch(NoSuchAlgorithmException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Missing DSA or ELGAMAL!", e);
		} catch(NoSuchProviderException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Spongy Castle not found!", e);
		} catch(InvalidAlgorithmParameterException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Invalid Algorithm Parameters!", e);
		} catch(PGPException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Unknown PGP Exception!", e);
		} catch(Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Unknown Exception!", e);

		}

	}
}