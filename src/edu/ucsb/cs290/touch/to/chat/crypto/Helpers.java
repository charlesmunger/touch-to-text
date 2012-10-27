package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Helpers {

	public static Object deserialize(byte[] b) {
		Object temp = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			temp = in.readObject();
		} catch (Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Error deserializing object", e);
		} finally {
			try {
				bis.close();
				in.close();
			} catch (Exception e) {
			}
		}
		return temp;
	}
	
	public static byte[] serialize(Serializable s) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] yourBytes = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(s);
			yourBytes = bos.toByteArray();
		} catch (Exception e) {
			Logger.getLogger("touch-to-text").log(Level.SEVERE,
					"Error serializing object", e);
		} finally {
			try {
				out.close();
				bos.close();
			} catch (Exception e) {
			} // TODO handle elegantly
		}
		return yourBytes;
	}
}
