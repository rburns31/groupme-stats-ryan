import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class StoreMessages {
	private final String USER_AGENT = "Mozilla/5.0";
	private final String CURRENT_GROUP = "25931103";
	private final String ACCESS_TOKEN = "gsg4UrfDIZuRSDQxXAkkIF1033u9LDYpuDeK6b1h";
 
	public static void main(String[] args) throws Exception {
		StoreMessages http = new StoreMessages();
 
		System.out.println("Testing 1 - Send Http GET request");
		ArrayList<String> allMessages = http.getAllMessages();
		PrintWriter writer = new PrintWriter("daniceMessages.txt", "UTF-8");
		Iterator<String> iter = allMessages.iterator();
		while(iter.hasNext()) {
			writer.println(iter.next());
		}
		writer.close();
	}
	
	/*
	 * Calculates the likes of all messages in the group
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<String> getAllMessages() throws Exception {
		
		ArrayList<String> allMessages = new ArrayList<String>();
		String urlParameters = "";
		long count = 0;
		boolean keepGoing = true;
		boolean firstTime = true;
		while (keepGoing) {
			String url = "https://api.groupme.com/v3/groups/" + CURRENT_GROUP + "/messages?token=" + ACCESS_TOKEN;
			url += urlParameters;
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
	 
			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
	 
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
	 
			//print result
			//System.out.println(response.toString());
			String lastMessageID = "";
			JSONParser parser = new JSONParser();
			JSONObject data = null;
			try {
				data = (JSONObject) parser.parse(response.toString());
			} catch (Exception e){
				System.out.println("ended" + response.toString());
				return allMessages;
			}
			
			JSONObject responses = (JSONObject) data.get("response");
			
			//Set up the count for the number of messages in the group
			if (firstTime) {
				count = (long) responses.get("count");
				System.out.println(count);
				firstTime = false;
			}
			System.out.println(count);

			JSONArray messages = (JSONArray) responses.get("messages");
			Iterator<JSONObject> iterator = messages.iterator();
			
			//Go through each message
			while(iterator.hasNext() && count > 0) {
				JSONObject message = iterator.next();
				lastMessageID = (String) message.get("id");
				String text = (String) message.get("text");
				long date = (long) message.get("created_at");
				if (text != null) {
					allMessages.add((text + "|" + (new Date(date * 1000)).toString()));
				}
				count--;
			}
			if (count <= 0) {
				keepGoing = false;
			}
			urlParameters = "&before_id=" + lastMessageID;
		}
		return allMessages;
	}
}