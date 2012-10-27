package messages;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

public class ProtectedMessage implements Serializable {
	private SealedObject message;
	private SealedObject messageKey;

	public ProtectedMessage(Message message, PublicKey dest, KeyPair author) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException, IOException, NoSuchPaddingException, IllegalBlockSizeException {
		SignedMessage signedMessage = new SignedMessage(message, author);
		KeyGenerator kg = KeyGenerator.getInstance("AES", "SC");
		kg.init(128);
		Key aesKey = kg.generateKey();
		Cipher c = Cipher.getInstance("AES","SC");
		c.init(Cipher.ENCRYPT_MODE, aesKey);
		this.message = new SealedObject(signedMessage, c);
		Cipher d = Cipher.getInstance("ElGamal", "SC");
		d.init(Cipher.ENCRYPT_MODE, dest);
		messageKey = new SealedObject(aesKey, d);
	}
	
	public SignedMessage getMessage(PrivateKey recipient, PublicKey author) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, IOException, ClassNotFoundException {
		Key aesKey = (Key) messageKey.getObject(recipient,"SC");
		return (SignedMessage) message.getObject(aesKey, "SC");
	}
}
