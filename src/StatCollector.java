import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Collects the total likes and like/comment ratios for everyone in the groupme group
 * You need an access token, a group id, and a bot id
 * 
 * NOTE:
 * If there are too many people in the group, the message
 * that the bot tries to send will probably be too long and will
 * most likely crash. The max message length is like 450. Also,
 * if there are weird characters like emojis in names it might
 * also crash. 
 * 
 * @author Graham Wright (altered by Ryan Burns)
 */
public class StatCollector {
	private static final JSONParser PARSER = new JSONParser();
	private static final String USER_AGENT = "Mozilla/5.0";	
	private static final String BOT_ID = "58e3406f09337706ed30dc3872";
	//private static final String GROUP_ID = "25931103"; //TestGroup
	private static final String GROUP_ID = "18189190"; //MainChat
	private static final String ACCESS_TOKEN = "gsg4UrfDIZuRSDQxXAkkIF1033u9LDYpuDeK6b1h";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		String urlParameters = "";
		Long count = Long.MAX_VALUE;
		Long totalMessages = Long.MAX_VALUE;
		Map<String, Double> likes = new HashMap<>();
		Map<String, Double> comments = new HashMap<>();

		while (count.compareTo(new Long(0)) > 0) {
			System.out.println(count);
			URL url = new URL("https://api.groupme.com/v3/groups/" + GROUP_ID
					+ "/messages?token=" + ACCESS_TOKEN + urlParameters);

			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
			StringBuffer response = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			String lastMessageID = "";

			JSONObject data;
			try {
				data = (JSONObject) PARSER.parse(response.toString());
			} catch (Exception e) {
				break;
			}
			JSONObject responses = (JSONObject) data.get("response");

			if (count.equals(Long.MAX_VALUE)) {
				count = (Long) responses.get("count");
				totalMessages = (Long) responses.get("count");
			}

			JSONArray messages = (JSONArray) responses.get("messages");
			Iterator<JSONObject> messagesIterator = messages.iterator();
			
			// Go through each message
			while (messagesIterator.hasNext()) {
				JSONObject message = messagesIterator.next();
				String name = (String) message.get("name");
				
				JSONArray favorites = (JSONArray) message.get("favorited_by");
				Iterator<JSONObject> likesIterator = favorites.iterator();
				
				// Loop through each favorite
				while (likesIterator.hasNext()) {
					likesIterator.next();
										
					if (likes.get(name) == null) {
						likes.put(name, 1.0);
					} else {
						likes.put(name, likes.get(name) + 1);
					}
				}
				
				// Add a count to the comment
				if (comments.get(name) == null) {
					comments.put(name, 1.0);
				} else {
					comments.put(name, comments.get(name) + 1);
				}
				
				count--;
				lastMessageID = (String) message.get("id");
			}
			urlParameters = "&before_id=" + lastMessageID;
		}

		// Calculate the like to comment ratios for everyone
		Map<String, Double> ratios = new HashMap<String, Double>();
		for (String name: comments.keySet()) {
			Double numLikes = likes.get(name);
			Double numComments = comments.get(name);
			ratios.put(name, numLikes != null ? ((double) numLikes) / numComments : 0);
		}

		likes = combineNames(likes);
		likes = sortByValue(likes);
		
		comments = combineNames(comments);
		comments = sortByValue(comments);
		
		ratios = combineNames(ratios);
		ratios = sortByValue(ratios);

		System.out.println("Likes: " + likes.toString());
		System.out.println("Comments: " + comments.toString());
		System.out.println("Ratios: " + ratios.toString());

		System.out.println("\nProcessed " + totalMessages + " messages in " + (System.currentTimeMillis()- startTime) / 1000 + " seconds");

		//postResults(likes, "\nTotal Likes:\n");
		//postResults(ratios, "\nLikes per Comment:\n");
	}

	/**
	 * Takes in the hashmap with corresponding likes and posts it to the group
	 */
	private static void postResults(Map<String, Double> result, String headerLine) throws Exception {
		URL url = new URL("https://api.groupme.com/v3/bots/post");
		HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
 		httpConnection.setRequestMethod("POST");
		httpConnection.setRequestProperty("User-Agent", USER_AGENT);
		httpConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		
		String formattedResult = headerLine;
		for (String name: result.keySet()) {
			formattedResult += name + ": " + result.get(name) + "\n";
		}
		
		String urlParameters = "bot_id=" + BOT_ID + "&text=" + formattedResult;
 
		// Send post request
		httpConnection.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		int responseCode = httpConnection.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	private static Map<String, Double> combineNames(Map<String, Double> input) {
		Map<String, Double> output = new HashMap<>();
		output.put("Ryan", 0.0);
		output.put("Aaron", 0.0);
		output.put("Ian", 0.0);
		output.put("James", 0.0);
		output.put("Anthony", 0.0);
		output.put("Alex", 0.0);
		output.put("Daniel", 0.0);
		output.put("Matt", 0.0);
		output.put("Nick", 0.0);

		for (String trimmedName: output.keySet()) {
			for (String originalName: input.keySet()) {
				if (originalName.contains(trimmedName)) {
					output.put(trimmedName, output.get(trimmedName) + input.get(originalName));
				}
			}
		}
		output.put("Nick", output.get("Nick") + input.get("Kramer"));
		
		return output;
	}

	/**
	 * Taken from http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
	 * @param map
	 * @return
	 */
	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry: list) {
			result.put(entry.getKey(), entry.getValue());
    	}
    	return result;
	}
}