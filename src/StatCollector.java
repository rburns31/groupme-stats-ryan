import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

// These require the simple json library
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Collects the like/comment ratios for everyone in the groupme group
 * You need an access token, a group id, and a bot id
 * 
 * This is the shared version; the experimental version is Groupme
 * 
 * NOTE:
 * If there are too many people in the group, the message
 * that the bot tries to send will probably be too long and will
 * most likely crash. The max message length is like 450. Also,
 * if there are weird characters like emojis in names it might
 * also crash. 
 * 
 * @author Graham Wright
 */
public class StatCollector {
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int LIKES = 0;
	private static final int LIKES_PER_COMMENT = 1;
	
	private final String BOT_ID = "58e3406f09337706ed30dc3872";
	private final String GROUP_ID = "25931103"; //TestGroup
	private final String ACCESS_TOKEN = "gsg4UrfDIZuRSDQxXAkkIF1033u9LDYpuDeK6b1h";
	
	// Assign it either LIKES or LIKES_PER_COMMENT for which data you want to send
	public static final int CURRENT_OPTION = LIKES;
 
	/*
	 * You can comment out the post part to see what will be posted
	 */
	public static void main(String[] args) throws Exception {
		StatCollector statCollector = new StatCollector();
 
		if (CURRENT_OPTION == LIKES) {
			Map<String, Integer> result = statCollector.getLikes();
			//statCollector.postLikeStatistics(result);
		} else {
			Map<String, Double> result = statCollector.getLikesPerComment();
			//statCollector.postLikesPerCommentStatistics(result);
		}
	}
	
	/*
	 * Calculates the like per comment ratios for everyone
	 */
	private Map<String, Double> getLikesPerComment() throws Exception {
		String urlParameters = "";
		boolean keepGoing = true;
		Long count = new Long(0);
		boolean firstTime = true;
		HashMap<String, Integer> likes = new HashMap<String, Integer>();
		HashMap<String, Integer> comments = new HashMap<String, Integer>();
		HashMap<String, Double> ratios = new HashMap<String, Double>();
		while (keepGoing) {
			String url = "https://api.groupme.com/v3/groups/" + GROUP_ID + "/messages?token=" + ACCESS_TOKEN;
			url += urlParameters;
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
	 
			int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'GET' request to URL : " + url);
			//System.out.println("Response Code : " + responseCode);
	 
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
			JSONObject data = (JSONObject) parser.parse(response.toString());
			JSONObject responses = (JSONObject) data.get("response");
			
			//Set up the count for the number of messages in the group
			if (firstTime) {
				count = (Long) responses.get("count");
				firstTime = false;
			}
			JSONArray messages = (JSONArray) responses.get("messages");
			Iterator iterator = messages.iterator();
			
			//Go to each message
			while(iterator.hasNext() && count > 0) {
				JSONObject message = (JSONObject) iterator.next();
				String text = (String) message.get("text");
				if (text != null) {
					//System.out.println(text);
				}
				String name = (String) message.get("name");
				
				Scanner scan = new Scanner(name);
				String easierName = name;
				
				JSONArray favorites = (JSONArray) message.get("favorited_by");
				Iterator i = favorites.iterator();
				
				//Loop through all the people who hearted the comment and add a count
				while (i.hasNext()) {
					i.next();
					if (likes.get(easierName) == null) {
						likes.put(easierName, 1);
					} else {
						likes.put(easierName, likes.get(easierName) + 1);
					}
				}
				
				//Decrement the overall count of messages
				count--;
				
				//Add a count to the comment
				if (comments.get(easierName) == null) {
					comments.put(easierName, 1);
				} else {
					comments.put(easierName, comments.get(easierName) + 1);
				}
				
				lastMessageID = (String) message.get("id");
			}
			if (count == 0) {
				keepGoing = false;
			}
			urlParameters = "&before_id=" + lastMessageID;
		}
		
		
		//Iterate through the hashmaps to add ratios to the ratio hashmap
		Set<String> keys = comments.keySet();
		Iterator i = keys.iterator();
		
		//Calculate the like to comment ratios for everyone
		while (i.hasNext()) {
			String name = (String) i.next();
			String formatName = name;
			Integer numLikes = likes.get(name);
			Integer numComments = comments.get(name);
			ratios.put(formatName, numLikes != null ? ((double) numLikes) / numComments : 0);
			
		}
		
		System.out.println(likes.toString());
		System.out.println(comments.toString());
		System.out.println(ratios.toString());
		
		return ratios;
	}
	
	/**
	 * Takes in the hashmap with corresponding likes and posts it to the group
	 */
	private void postLikesPerCommentStatistics(Map<String, Double> result) throws Exception {
		String url = "https://api.groupme.com/v3/bots/post";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
 
		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		
		String FORMATTED_RESULT = "\nLikes per Comment\n";
		Set<String> keys = result.keySet();
		Iterator i = keys.iterator();
		while (i.hasNext()) {
			String name = (String) i.next();
			Double ratio = result.get(name);
			DecimalFormat df = new DecimalFormat("#.##");
			FORMATTED_RESULT += name + ": " + df.format(ratio) + "\n";
		}
		
		System.out.println(FORMATTED_RESULT);
		//System.exit(0);
		
		String urlParameters = "bot_id=" + BOT_ID + "&text=" + FORMATTED_RESULT;
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
	}
	
	/*
	 * Calculates the likes of all messages in the group
	 */
	private Map<String, Integer> getLikes() throws Exception {
		String urlParameters = "";
		Long count = new Long(0);
		boolean firstTime = true;
		Map<String, Integer> likes = new HashMap<>();
		
		boolean keepGoing = true;
		while (keepGoing) {
			String url = "https://api.groupme.com/v3/groups/" + GROUP_ID + "/messages?token=" + ACCESS_TOKEN;
			url += urlParameters;
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
	 
			int responseCode = con.getResponseCode();
			//System.out.println("\nSending 'GET' request to URL : " + url);
			//System.out.println("Response Code : " + responseCode);
	 
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
			JSONObject data = (JSONObject) parser.parse(response.toString());
			JSONObject responses = (JSONObject) data.get("response");
			if (firstTime) {
				count = (Long) responses.get("count");
				firstTime = false;
			}
			JSONArray messages = (JSONArray) responses.get("messages");
			Iterator iterator = messages.iterator();
			
			//Go through each message
			while(iterator.hasNext() && count > 0) {
				JSONObject message = (JSONObject) iterator.next();
				String text = (String) message.get("text");
				if (text != null) {
					//System.out.println(text);
				}
				String name = (String) message.get("name");
				
				JSONArray favorites = (JSONArray) message.get("favorited_by");
				Iterator i = favorites.iterator();
				
				//Loop through each favorite
				while (i.hasNext()) {
					i.next();
					
					Scanner scan = new Scanner(name);
					String easierName = name;
				
					
					if (likes.get(easierName) == null) {
						likes.put(easierName, 1);
					} else {
						likes.put(easierName, likes.get(easierName) + 1);
					}
				}
				
				count--;
				lastMessageID = (String) message.get("id");
			}
			if (count == 0) {
				keepGoing = false;
			}
			urlParameters = "&before_id=" + lastMessageID;
		}
		System.out.println(likes.toString());
		return likes;
	}
	
	/**
	 * Takes in the hashmap with corresponding likes and posts it to the group
	 */
	private void postLikeStatistics(Map<String, Integer> result) throws Exception {
		String url = "https://api.groupme.com/v3/bots/post";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
 
		// Add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		
		String FORMATTED_RESULT = "\n";
		Set<String> keys = result.keySet();
		Iterator i = keys.iterator();
		while (i.hasNext()) {
			String name = (String) i.next();
			FORMATTED_RESULT += name + ": " + result.get(name) + " like" + (result.get(name) == 1 ? "" : "s") + "\n";
		}
		System.out.println(FORMATTED_RESULT);
		
		String urlParameters = "bot_id=" + BOT_ID + "&text=" + FORMATTED_RESULT;
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
	}
}