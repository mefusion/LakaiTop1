package pro.delfik.callisto.vkontakte;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pro.delfik.callisto.Callisto;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VK {

	private static final String token = "5cd7565d245774d08cd43bd8522ed2167a25a1d8338a3d309c5ee91ca7adef94c7a3b1668a2f6b8d87d16";
	
	public static void start() {
		new Thread(LongPoll::run, "VKBot").start();
	}
	
	public static String query(String method) {
		return get("https://api.vk.com/method/" + method, "v=5.80&access_token=" + token);
	}

	public static String query(String method, String params) {
		return get("https://api.vk.com/method/" + method, "v=5.80&access_token=" + token + "&" + params);
	}
	
	public static void markAsRead(int from_id) {
		VK.query("messages.markAsRead", "peer_id=" + from_id);
	}

	public static String uploadPhoto(File f) {
		String id = "";
		String upload_url;

		try{
			String server = VK.query("photos.getMessagesUploadServer");

			JSONObject s = new JSONObject(server);
			upload_url = s.getJSONObject("response").getString("upload_url");

			String res = post_upload(upload_url, f);


			JSONObject j = new JSONObject(res);

			int _server = j.getInt("server");
			String _photo = j.getString("photo");
			String _hash = j.getString("hash");

			String params = "server=" + _server + "&photo=" + _photo + "&hash=" + _hash;

			res = VK.query("photos.saveMessagesPhoto", params);

			j = new JSONObject(res);
			JSONArray arr = j.getJSONArray("response");
			JSONObject g = arr.getJSONObject(0);
			id = g.getString("id");
		}catch (Exception e){
			e.printStackTrace();
		}

		return id;
	}

	public static String getUserName(int uid) {
		String data = query("users.get", "user_id=" + uid);
		String full_name;
		try{
			JSONObject obj = new JSONObject(data);
			JSONArray response = obj.getJSONArray("response");
			JSONObject _data = response.getJSONObject(0);
			String first_name = _data.getString("first_name");
			String last_name = _data.getString("last_name");

			full_name = first_name + " " + last_name;

		}catch (JSONException e){
			e.printStackTrace();
			full_name = "";
		}

		return full_name;
	}
	
	public static int getUserID(String arg) {
		String data = query("users.get", "user_ids=" + arg);
		try{
			JSONObject obj = new JSONObject(data);
			JSONArray response = obj.getJSONArray("response");
			JSONObject _data = response.getJSONObject(0);
			return _data.getInt("uid");
		} catch (JSONException e){
			return -1;
		}
	}
	
	public static class MultipartUtility {

		private final String boundary;
		private static final String LINE_FEED = "\r\n";
		private HttpURLConnection httpConn;
		private String charset;
		private OutputStream outputStream;
		private PrintWriter writer;

		public MultipartUtility(String var1, String var2) throws IOException {
			this.charset = var2;
			this.boundary = "===" + System.currentTimeMillis() + "===";
			URL var3 = new URL(var1);
			this.httpConn = (HttpURLConnection) var3.openConnection();
			this.httpConn.setUseCaches(false);
			this.httpConn.setDoOutput(true);
			this.httpConn.setDoInput(true);
			this.httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary);
			this.httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
			this.httpConn.setRequestProperty("Test", "Bonjour");
			this.outputStream = this.httpConn.getOutputStream();
			this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, var2), true);
		}

		public void addFormField(String var1, String var2) {
			this.writer.append("--").append(this.boundary).append("\r\n");
			this.writer.append("Content-Disposition: form-data; name=\"").append(var1).append("\"").append("\r\n");
			this.writer.append("Content-Type: text/plain; charset=").append(this.charset).append("\r\n");
			this.writer.append("\r\n");
			this.writer.append(var2).append("\r\n");
			this.writer.flush();
		}

		public void addFilePart(String var1, File var2) throws IOException {
			String var3 = var2.getName();
			this.writer.append("--").append(this.boundary).append("\r\n");
			this.writer.append("Content-Disposition: form-data; name=\"").append(var1).append("\"; filename=\"").append(var3).append("\"").append("\r\n");
			this.writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(var3)).append("\r\n");
			this.writer.append("Content-Transfer-Encoding: binary").append("\r\n");
			this.writer.append("\r\n");
			this.writer.flush();
			FileInputStream var4 = new FileInputStream(var2);
			byte[] var5 = new byte[4096];
			boolean var6 = true;

			int var7;
			while ((var7 = var4.read(var5)) != -1){
				this.outputStream.write(var5, 0, var7);
			}

			this.outputStream.flush();
			var4.close();
			this.writer.append("\r\n");
			this.writer.flush();
		}

		public void addHeaderField(String var1, String var2) {
			this.writer.append(var1).append(": ").append(var2).append("\r\n");
			this.writer.flush();
		}

		public List<String> finish() throws IOException {
			ArrayList<String> var1 = new ArrayList<>();
			this.writer.append("\r\n").flush();
			this.writer.append("--").append(this.boundary).append("--").append("\r\n");
			this.writer.close();
			int var2 = this.httpConn.getResponseCode();
			if(var2 != 200){
				throw new IOException("Server returned non-OK status: " + var2);
			}else{
				BufferedReader var3 = new BufferedReader(new InputStreamReader(this.httpConn.getInputStream()));
				String var4;

				while ((var4 = var3.readLine()) != null){
					var1.add(var4);
				}

				var3.close();
				this.httpConn.disconnect();
				return var1;
			}
		}
	}

	public static String post_upload(String var0, File var1) {
		String response = "";

		try{
			MultipartUtility multipartUtility = new MultipartUtility(var0, "utf-8");
			multipartUtility.addFilePart("file", var1);
			List var4 = multipartUtility.finish();

			String var6;
			for (Iterator var5 = var4.iterator(); var5.hasNext(); response += var6){
				var6 = (String) var5.next();
			}
		}catch (IOException var7){
			response = "upload error";
			var7.printStackTrace();
		}

		return response;
	}

	public static String get(String server, String args) {
		return get(server + "?" + args);
	}

	public static String get(String server){
		try{
			if (Callisto.os == Callisto.OS.WIN) server = new String(server.getBytes("UTF-8"), "windows-1251");
			URL url = new URL(server);
			URLConnection connection = url.openConnection();
			connection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String result = reader.readLine();
			reader.close();
			return result;
		}catch (Exception ex){
			ex.printStackTrace();
			return "fail";
		}
	}
}