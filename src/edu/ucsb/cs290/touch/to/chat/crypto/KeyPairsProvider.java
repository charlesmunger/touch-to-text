package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import android.util.Log;

public class KeyPairsProvider implements Serializable {
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	private KeyPair signingKeyPair;
	private KeyPair transmissonKeyPair;
	
	public KeyPairsProvider() {
		ExecutorService e = Executors.newCachedThreadPool();
		Future<KeyPair> signingFut = e.submit(new Generate("DSA",1024));
		Future<KeyPair> transFut = e.submit(new Generate("ElGamal", 1024));
		try {
			signingKeyPair = signingFut.get();
			transmissonKeyPair = transFut.get();
		} catch (Exception e1) {
			Log.wtf("touch-to-text", "Interrupted key generation", e1);
		}
	}
	
	private static KeyPair generate(String algorithm, int bits) {
		Log.d("touch-to-chat", "starting generation of "+algorithm);
		long time = System.currentTimeMillis();
		KeyPairGenerator gen = null;
		try {
			gen = KeyPairGenerator.getInstance(algorithm, "SC");
		} catch (GeneralSecurityException e) {
			Log.wtf("touch-to-text", e);
		}
		gen.initialize(bits);
		KeyPair kp = gen.generateKeyPair();
		Log.d("touch-to-text", "Done generating "+algorithm + "took "+ (System.currentTimeMillis()-time)/1000);
		return kp;
	}
	
	private class Generate implements Callable<KeyPair> {
		private final String algorithm;
		private final int bits;
		
		public Generate(String algorithm, int bits) {
			this.algorithm = algorithm;
			this.bits = bits;
		}
		
		@Override
		public KeyPair call() throws Exception {
			return generate(algorithm, bits);
		}
	}
	
	public SealablePublicKey getExternalKey() {
		return new SealablePublicKey(signingKeyPair,transmissonKeyPair.getPublic());
	}
}
