package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;

public class PGPSignedObject<T extends Serializable> implements Serializable {
	final byte[] obj;
	
	public PGPSignedObject(T obj, PGPSecretKey pgpSec, boolean armor) throws Exception, PGPException {
		byte[] clearData = Helpers.serialize(obj);
		ByteArrayOutputStream bytesout = new ByteArrayOutputStream();
		OutputStream out;
		if (armor) {
			out = new ArmoredOutputStream(bytesout);
		} else {
			out = bytesout;
		}
		
		PGPCompressedDataGenerator cGen = new PGPCompressedDataGenerator(
				PGPCompressedData.ZIP);
		
		PGPLiteralDataGenerator lGen = new PGPLiteralDataGenerator();
		
		
		PGPPrivateKey pgpPrivKey = pgpSec.extractPrivateKey(new char[2], "SC");
		PGPSignatureGenerator sGen = new PGPSignatureGenerator(
				new BcPGPContentSignerBuilder(
						pgpSec.getPublicKey().getAlgorithm(),
						PGPUtil.SHA256).setSecureRandom(
								new SecureRandom()));
		sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);

		Iterator it = pgpSec.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();

			spGen.setSignerUserID(false, (String) it.next());
			sGen.setHashedSubpackets(spGen.generate());
		}

		BCPGOutputStream bOut = new BCPGOutputStream(cGen.open(out));

		sGen.generateOnePassVersion(false).encode(bOut);

		OutputStream lOut = lGen.open(bOut, 
				PGPLiteralData.BINARY, 
				PGPLiteralData.CONSOLE, 
				clearData.length, 
				new Date());
		lOut.write(clearData);
		InputStream fIn  = new ByteArrayInputStream(clearData);
		int ch = 0;
		while ((ch = fIn.read()) >= 0) {
			lOut.write(ch);
			sGen.update((byte) ch);
		}
		sGen.generate().encode(bOut);

		lGen.close();
		cGen.close();
		out.close();
		
		this.obj = bytesout.toByteArray();
	}
}
