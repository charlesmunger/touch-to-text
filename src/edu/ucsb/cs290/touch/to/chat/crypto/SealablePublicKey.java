package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.spongycastle.openpgp.PGPPublicKey;
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Externalizable {
	byte[] publicKey;
	String identity;

	public SealablePublicKey(byte[] publicKey, String identity) {
		this.identity = identity;
		this.publicKey = publicKey;
	}
	
	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		input.read(publicKey);
		identity = input.readUTF();
		// TODO Auto-generated method stub

	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.write(publicKey);
		output.writeUTF(identity);
	}
	
	// We could just SHA-1 the publicKey byte array, it would be efficient
	public String digest() {
		return "Digest Dummy String";
	}

}
