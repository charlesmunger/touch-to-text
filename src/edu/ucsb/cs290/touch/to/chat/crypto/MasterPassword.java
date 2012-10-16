package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spongycastle.util.encoders.Base64;

/** Since we never want to store the user's password in plaintext,
 * 	even in memory, we use a 500 round SHA-512 hash of the
 *	user's password as the key to the database.
 *	Most methods are package level visible for the other crypto classes.	
**/
public class MasterPassword {

	private static String password;
	private static int PW_HASH_ITERATION_COUNT= 500;
    private static MessageDigest md;

	static String getPassword() {
		return password;
	}

	public static void setPassword(String userPass) {
		byte[] salt = null;
		byte[] pw = null;

		// Since we want to not use a consistent salt, but
		// we have to use the same salt every time, may as
		// well pull it from the user's password, crypt-style.
		
		// We should also set a 7+ character minimum password!
		String saltChars = userPass.substring(0, 4);

		try {
			salt = saltChars.getBytes("UTF-8");
			pw = userPass.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE, "No UTF-8?!", e);
		}

		byte[] digest = run(pw, salt);
		for (int i = 0; i < PW_HASH_ITERATION_COUNT - 1; i++) {
			digest = run(digest, salt);
		}
		
		password = new String(digest);
		// Zero out the byte array containing plaintext pass
		// Can't do anything about userPass, it's immutable...
		for(int i=0;i<pw.length;i++) {
			pw[i]=0;
		}
		
	}

	private static byte[] run(byte[] input, byte[] salt) {
		md.update(input);
		return md.digest(salt);
	}
	
	public static void forgetPassword() {
		password = null;
	}

}
