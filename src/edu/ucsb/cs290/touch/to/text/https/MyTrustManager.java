package edu.ucsb.cs290.touch.to.text.https;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class MyTrustManager implements X509TrustManager {
	private KeyStore keyStore;
	private X509TrustManager defaultTrustManager;
	private X509TrustManager appTrustManager;
	private final String LOG = "MyTrustManager";
	byte[] keyStored = null;
	String pwd;
	
	public MyTrustManager() {
		loadKeyStore();
		
		defaultTrustManager = getTrustManager(false);
		appTrustManager = getTrustManager(true);
	}
	
	private X509TrustManager getTrustManager(boolean withKeystore) {
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			if(withKeystore)
				tmf.init(keyStore);
			else
				tmf.init((KeyStore) null);
			for(TrustManager t : tmf.getTrustManagers())
				if(t instanceof X509TrustManager)
					return (X509TrustManager) t;
		} catch (KeyStoreException e) {
			Log.e(LOG, "key store exception: " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG, "no such algo exception: " + e.toString());
		}
		return null;
	}
	
	private void loadKeyStore() {
		//TODO: this is where you load up your keystore and store the bytes into the keyStored field if neccessary.
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch(KeyStoreException e) {
			Log.e(LOG, "key store exception: " + e.toString());
		}
		
		try {
			keyStore.load(null, null);
			if(keyStored != null)
				keyStore.load(new ByteArrayInputStream(keyStored), pwd.toCharArray());
			
			
		} catch(CertificateException e) {
			Log.e(LOG, "certificate exception: " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG, "no such algo exception: " + e.toString());
		} catch (IOException e) {
			Log.e(LOG, "IOException: " + e.toString());
		}
	}
	
	private void storeCertificate(X509Certificate[] chain) {
		try {
			for(X509Certificate cert : chain) {
				keyStore.setCertificateEntry(cert.getSubjectDN().toString(), cert);
			}
		} catch(KeyStoreException e) {
			Log.e(LOG, "keystore exception: " + e.toString());
		}
		
		appTrustManager = getTrustManager(true);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			keyStore.store(baos, pwd.toCharArray());
			updateKeyStore(baos.toByteArray());
			Log.d(LOG, "new key encountered!  length: " + baos.size());
		} catch(KeyStoreException e) {
			Log.e(LOG, "keystore exception: " + e.toString());	
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG, "no such algo exception: " + e.toString());
		} catch (IOException e) {
			Log.e(LOG, "IOException: " + e.toString());
		} catch (CertificateException e) {
			Log.e(LOG, "Certificate Exception: " + e.toString());
		}
	}
	
	private void updateKeyStore(byte[] newKey) {
		// TODO: this is where YOU update your own keystore if you need to (ie, if it's in an SQLite database)
	}
	
	private boolean isCertKnown(X509Certificate cert) {
		try {
			return keyStore.getCertificateAlias(cert) != null;
		} catch(KeyStoreException e) {
			return false;
		}
	}
	
	private boolean isExpiredException(Throwable e) {
		do {
			if(e instanceof CertificateExpiredException)
				return true;
			e = e.getCause();
		} while(e != null);
		return false;
	}
	
	private void checkCertificateTrusted(X509Certificate[] chain, String authType, boolean isServer) throws CertificateException {
		try {
			if(isServer)
				appTrustManager.checkServerTrusted(chain, authType);
			else
				appTrustManager.checkClientTrusted(chain, authType);
		} catch(CertificateException e) {
			if(isExpiredException(e))
				return;
			if(isCertKnown(chain[0]))
				return;
			
			try {
				if(isServer)
					defaultTrustManager.checkServerTrusted(chain, authType);
				else
					defaultTrustManager.checkClientTrusted(chain, authType);
			} catch(CertificateException ce) {
				storeCertificate(chain);
			}
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkCertificateTrusted(chain, authType, false);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkCertificateTrusted(chain, authType, true);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return defaultTrustManager.getAcceptedIssuers();
	}
	
}