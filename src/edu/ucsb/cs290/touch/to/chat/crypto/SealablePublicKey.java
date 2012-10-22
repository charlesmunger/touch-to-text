package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Serializable;
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Serializable {
	byte[] publicKey;
	String identity;

	public SealablePublicKey(byte[] publicKey, String identity) {
		this.identity = identity;
		this.publicKey = publicKey;
	}
	
//	@Override
//	public void readExternal(ObjectInput input) throws IOException,
//			ClassNotFoundException {
//		identity = input.readUTF();
//		publicKey = (byte[]) input.readObject();
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void writeExternal(ObjectOutput output) throws IOException {
//		output.writeUTF(identity);
//		output.writeObject(publicKey);
//	}
	
	// We could just SHA-1 the publicKey byte array, it would be efficient
	public String digest() {
		return "Digest Dummy String";
	}

}
