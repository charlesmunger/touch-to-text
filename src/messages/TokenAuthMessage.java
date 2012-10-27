package messages;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.UUID;

public class TokenAuthMessage implements Serializable {
	private final ProtectedMessage pm;
	private final PublicKey destination;
	private final SignedObject secretToken;
	public TokenAuthMessage(ProtectedMessage pm, PublicKey destination, SignedObject secretToken) {
		this.destination = destination;
		this.secretToken = secretToken;
		this.pm = pm;
	}
	
	public PublicKey getDestination() {
		try {
			if (secretToken.verify(destination, Signature.getInstance("DSA", "SC"))) {
				return destination;
			}
		} catch (GeneralSecurityException e) {
			
		}
		return null;
	}
	
	public UUID getToken() throws IOException, ClassNotFoundException {
		return (UUID) secretToken.getObject();
	}
	
	public ProtectedMessage getMessage() {
		return pm;
	}
}
