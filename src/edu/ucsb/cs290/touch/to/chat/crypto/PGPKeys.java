package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.math.BigInteger;
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

import net.sqlcipher.database.SQLiteDatabase;

import org.spongycastle.jce.spec.ElGamalParameterSpec;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;

/**
 * Generates a keyring containing public and secret DSA signing
 * key and an El Gamal key for encryption.
 * 
 */
final class PGPKeys {

	private KeyPair dsaKp;
	private KeyPair elgKp;
	private PGPSecretKeyRing privateKeyRing;
	private PGPPublicKeyRing publicKeyRing;


	static {
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
	}

	/**
	 * Loads keyrings from the DB into the 
	 * @param publicKey
	 * @param privateKey
	 * @throws IOException
	 * @throws PGPException
	 */
	public PGPKeys(byte[] publicKey, byte[] privateKey) throws IOException, PGPException {
		privateKeyRing = new PGPSecretKeyRing(privateKey);
		publicKeyRing = new PGPPublicKeyRing(publicKey);
	}
	
	public PGPKeys(byte[] publicKey) throws IOException, PGPException {
		publicKeyRing = new PGPPublicKeyRing(publicKey);
	}

	// Deprecated PGPKeyPair methods but I can't find a good replacement.
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
	byte[] getPublicKey(String identity, PasswordProtection passPhrase) 
			throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {

		if ( publicKeyRing == null || privateKeyRing == null ) {
			if( dsaKp == null || elgKp == null) {
				generateDSAElGamal(identity, passPhrase);
			} else {
				PGPKeyPair dsaKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
				PGPKeyPair elgKeyPair = new PGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());

				PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair,
						identity, PGPEncryptedData.AES_256, passPhrase.getPassword(), true, null, null, new SecureRandom(), "BC");

				// Now we have our DSA and El Gamal keys in one place
				keyRingGen.addSubKey(elgKeyPair);
				publicKeyRing = keyRingGen.generatePublicKeyRing();
				privateKeyRing = keyRingGen.generateSecretKeyRing();
			}
		}
		Iterator<PGPPublicKey> publicKeyRingIter = publicKeyRing.getPublicKeys();
		// May need to intelligently pick keys, this just takes the last one.
		byte[] publicKey = null;

		while(publicKeyRingIter.hasNext()) {
			PGPPublicKey key = publicKeyRingIter.next();
			publicKey = key.getEncoded();
		}

		return publicKey;
	}


	// Deprecated PGPKeyPair methods but I can't find a good replacement.
	@SuppressWarnings("deprecation")
	private void storeMyKeysInDB(String identity, PasswordProtection passPhrase) 
			throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {


		PGPKeyPair dsaKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
		PGPKeyPair elgKeyPair = new PGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());

		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair,
				identity, PGPEncryptedData.AES_256, passPhrase.getPassword(), true, null, null, new SecureRandom(), "BC");

		// Now we have our DSA and El Gamal keys in one place
		keyRingGen.addSubKey(elgKeyPair);
		byte[] privateKeyRingBytes = keyRingGen.generateSecretKeyRing().getEncoded();
		byte[] publicKeyRingBytes = keyRingGen.generatePublicKeyRing().getEncoded();
		DatabaseHelper.getInstance(null).insertKeypair(privateKeyRingBytes, publicKeyRingBytes, identity);
		// LocalStorage: _id, private key, public key, timestamp (added), name

		//		Approach #0: Grab only the private key itself instead of the entire ring
		//		Iterator<PGPSecretKey> privateKeyRing =keyRingGen.generateSecretKeyRing().getSecretKeys();
		//		while(privateKeyRing.hasNext()) {
		//			PGPSecretKey key = privateKeyRing.next();
		//			privateKey = key.getEncoded();
		//		}
	}

	private void generateDSAElGamal(String name, PasswordProtection passphrase) {
		try {
			KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance("DSA", "BC");
			dsaKpg.initialize(1024); // Should benchmark vs. 2048 bit  keys
			dsaKp = dsaKpg.generateKeyPair();
			KeyPairGenerator elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "BC");
			BigInteger g = new BigInteger("153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc", 16);
			BigInteger p = new BigInteger("9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b", 16);
			ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);
			elgKpg.initialize(elParams);
			elgKp = elgKpg.generateKeyPair();
		} catch(NoSuchAlgorithmException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Missing DSA or ELGAMAL!", e);
		} catch(NoSuchProviderException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Bouncy Castle not found!", e);
		} catch(InvalidAlgorithmParameterException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Invalid Algorithm Parameters!", e);
		}
	}


	public PGPKeys(String name, PasswordProtection passphrase)  {
		generateDSAElGamal( name,  passphrase);
	}

}