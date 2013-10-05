package edu.ucsb.cs290.touch.to.text.crypto;

import java.security.InvalidKeyException;
import java.security.KeyStore.PasswordProtection;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import android.content.Context;
import android.content.SharedPreferences;

/** Since we never want to store the user's password in plaintext,
 * 	even in memory, we use a password keyed HMAC of the
 *	user's password as the key to the database.
 *
 *	Most methods are package level visible for the other crypto classes.	
 **/
final class MasterPassword {

	private static MasterPassword instance;
	private static PasswordProtection passphrase; 
	private static String prefs = "CrisisCommunicator.xml";
	private static String salt_key = "salt";
	private static Context context;
	private static String salt;

	/*
	 * Package level access to decrypt the database. {
	 */

	static MasterPassword getInstance(String password) {
		if(instance == null || passphrase == null || passphrase.isDestroyed()) {
			instance = new MasterPassword(password);
		}
		return instance;

	}

	char[] getPassword() {
		return passphrase.getPassword();
	}

	String getPasswordString() {
		return String.valueOf(passphrase.getPassword());
	}

	PasswordProtection getPasswordProtection() {
		return passphrase;
	}

	public MasterPassword(String userPass)  {
		byte[] encodedPass = null;
		
		try {
			SecretKey masterKey = generateKey(userPass.toCharArray(), getSalt());
			encodedPass = masterKey.getEncoded();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			e1.printStackTrace();
		}
		
		char[] buffer = new char[encodedPass.length >> 1];
		// PasswordProtection has no byte[] constructor.
		// Fastest byte[] to char[] conversion I know
		for(int i = 0; i < buffer.length; i++) {
			int bpos = i << 1;
			char c = (char)(((encodedPass[bpos]&0x00FF)<<8) + (encodedPass[bpos+1]&0x00FF));
			buffer[i] = c;
		}
		// Store in PasswordProtection, then do our best to remove user password from memory.
		passphrase = new PasswordProtection(buffer);
		for(int i=0;i<buffer.length;i++) {
			buffer[i]=0;
		}
		for(int i=0;i<encodedPass.length;i++) {
			encodedPass[i]=0;
		}
		// Should wipe out all remaining copies of userPass 
		// and zero out any Editables or UI elements that produced it.
		userPass = null;
		System.gc();
	}

	// Per Google's Android Developer Blog
	public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Number of PBKDF2 hardening rounds to use. Larger values increase
		// computation time. You should select a value that causes computation
		// to take >100ms.
		final int iterations = 4000; 

		// Generate a 256-bit key
		final int outputKeyLength = 256;

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		return secretKey;
	}

	// Create a random salt on first run and store it in the SharedPreferences.
	// Future calls access the stored value.
	public byte[] getSalt() throws NoSuchAlgorithmException {
		SharedPreferences sharedPreferences = context.getSharedPreferences(prefs, Context.MODE_PRIVATE);
		if(!sharedPreferences.contains(salt_key)) {
			final int outputKeyLength = 256;
			SecureRandom secureRandom = new SecureRandom();
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(outputKeyLength, secureRandom);
			SecretKey key = keyGenerator.generateKey();
			sharedPreferences.edit().putString(salt_key, new String(key.getEncoded())).commit();
		}
		String salt = sharedPreferences.getString(salt_key, null);
		return salt.getBytes();
	}

	public void forgetPassword() {
		try {
			passphrase.destroy();
		} catch (DestroyFailedException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Unable to destroy password in memory!", e);

		}
	}

	public static MasterPassword getInstance(String password, Context appContext) {
		context = appContext;
		return getInstance(password);
	}

}
