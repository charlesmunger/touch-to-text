package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
//TODO Danny fill this in
//Class is final to prevent classloader attack
public final class SealablePublicKey implements Externalizable {

	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		// TODO Auto-generated method stub

	}
	
	public String digest() {
		return "Digest Dummy String";
	}

}
