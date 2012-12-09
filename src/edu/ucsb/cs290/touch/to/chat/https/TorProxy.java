package edu.ucsb.cs290.touch.to.chat.https;

import info.guardianproject.onionkit.trust.StrongHttpsClient;

import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;

import android.content.Context;
import android.util.Log;
import edu.ucsb.cs290.touch.to.chat.R;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.TokenAuthMessage;
import edu.ucsb.cs290.touch.to.chat.remote.register.RegisterUser;

public class TorProxy {

	public static void postThroughTor(Context c, TokenAuthMessage tm) throws CertificateException {
		executeHttpsPost(c, TokenAuthMessage.FIELD_NAME, tm);
	}

	public static void postThroughTor(Context c, RegisterUser ru) throws CertificateException {
		executeHttpsPost(c, RegisterUser.FIELD_NAME, ru);
	}

	private static void executeHttpsPost(Context c, String name, Serializable value) throws CertificateException{
		HttpClient http = new StrongHttpsClient(c);
		Log.d("touch-to-text","Registering with server");
		http.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
				new HttpHost("localhost", 8118));

		HttpPost method = new HttpPost("https://"
				+ c.getString(R.string.service_url) + "/" + name);
		
		HttpResponse response;
		try {
			method.setEntity(new ByteArrayEntity(Helpers.serialize(value)));
			response = http.execute(method);
			Log.d("HTTPtor", response.getStatusLine().getStatusCode()+"");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
