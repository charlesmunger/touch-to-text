package edu.ucsb.cs290.touch.to.chat.https;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import android.util.Log;
import edu.ucsb.cs290.touch.to.chat.remote.messages.TokenAuthMessage;
import edu.ucsb.cs290.touch.to.chat.remote.register.RegisterUser;

public class TorProxy {
	private static final String serverUrl = "k4pf4spu5742f4px.onion";
	private static final String LOG = "TorProxy";
	private static final ExecutorService ex = Executors.newCachedThreadPool();
	public static String sendMessage(TokenAuthMessage m) {
		Map<String,Object>tempMap = new HashMap<String, Object>(1);
		tempMap.put("message", m);
		return executeHttpsPost(serverUrl, tempMap, "message");
	}
	
	public static String registerUser(RegisterUser r) {
		Map<String,Object>tempMap = new HashMap<String, Object>(1);
		tempMap.put("register", r);
		return executeHttpsPost(serverUrl, tempMap, "message");
	}
	
	private static String executeHttpsPost(final String host, final Map<String,Object> postData, final String contentType) {
		Future<String> future = ex.submit(new Callable<String>() {
			String result = "FAIL";
			String HYPHENS = "--";
			String LINE_END = "\r\n";
			String BOUNDARY = "***7hisIsMyBoUND4rY***";
			String hostname;
			
			URL url;
			HttpsURLConnection connection;
			HostnameVerifier hnv;
			DataOutputStream dos;
			SSLContext ssl;
			
			MyTrustManager itm;
			
			private void buildQuery() {
				Iterator<Entry<String, Object>> it = postData.entrySet().iterator();
				
				connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
				StringBuffer sb = new StringBuffer();
				try {
					dos = new DataOutputStream(connection.getOutputStream());
					sb = new StringBuffer();
					while(it.hasNext()) {
						sb = new StringBuffer();
						Entry<String, Object> e = it.next();
						
						sb.append(HYPHENS + BOUNDARY + LINE_END);
						
						sb.append("Content-Disposition: form-data; name=\"" + e.getKey() + "\"" + LINE_END);
						sb.append("Content-Type: " + contentType + "; charset=UTF-8" + LINE_END );
						sb.append("Cache-Control: no-cache" + LINE_END + LINE_END);
						sb.append(String.valueOf(e.getValue()) + LINE_END);
						dos.writeBytes(sb.toString());
					}
					
					dos.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END);
					
					dos.flush();
					dos.close();
					
				} catch (IOException e) {
					Log.e(LOG, e.toString());
					e.printStackTrace();
				}
			}
			
			@Override
			public String call() throws Exception {
				hostname = host.split("/")[0];
				url = new URL("https://" + host);
								
				hnv = new HostnameVerifier() {
					@Override
					public boolean verify(String hn, SSLSession session) {
						if(hn.equals(hostname))
							return true;
						else
							return false;
					}
				};
				
				itm = new MyTrustManager();
								
				ssl = SSLContext.getInstance("TLS");
				ssl.init(null, new TrustManager[] {itm}, new SecureRandom());
				
				HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
				HttpsURLConnection.setDefaultHostnameVerifier(hnv);
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8118));
				
				connection = (HttpsURLConnection) url.openConnection(proxy);
				
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Connection", "Keep-Alive");
				connection.setUseCaches(false);
				connection.setDoInput(true);
				connection.setDoOutput(true);
				
				buildQuery();
				
				try {
					InputStream is = connection.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String line;
					StringBuffer sb = new StringBuffer();
					while((line = br.readLine()) != null)
						sb.append(line);
					br.close();
					connection.disconnect();
					result = sb.toString();
				} catch(NullPointerException e) {
					Log.e(LOG, e.toString());
					e.printStackTrace();
				}
				return result;
			}
			
		});
		
		try {
			return future.get();
		} catch (InterruptedException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
			return null;
		} catch (ExecutionException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
			return null;
		}
	}
}
