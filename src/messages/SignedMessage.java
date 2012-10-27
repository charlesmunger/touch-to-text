package messages;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;

public class SignedMessage implements Serializable {
	private final SignedObject signedMessage;
	private final PublicKey authorKey;
	public SignedMessage(Message message, KeyPair author) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
		signedMessage= new SignedObject(message, author.getPrivate(), Signature.getInstance("DSA", "SC"));
		authorKey = author.getPublic();
	}
	
	public Message getMessage(PublicKey author) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException, IOException, ClassNotFoundException {
		if(this.authorKey.equals(author) && signedMessage.verify(author, Signature.getInstance("DSA","SC"))) {
			return (Message) signedMessage.getObject();
		}
		throw new SignatureException("Signatures did not match!!! ");
	}
	
	public PublicKey getAuthor() {
		return authorKey;
	}
}
