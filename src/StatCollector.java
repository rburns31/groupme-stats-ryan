import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Collects the total likes and like/comment ratios for everyone in the groupme
 * group You need an access token, a group id, and a bot id
 * 
 * NOTE: If there are too many people in the group, the message that the bot
 * tries to send will probably be too long and will most likely crash. The max
 * message length is like 450. Also, if there are weird characters like emojis
 * in names it might also crash.
 * 
 * @author Graham Wright (altered by Ryan Burns)
 */
public class StatCollector {
    private static final JSONParser PARSER = new JSONParser();
    private static final String USER_AGENT = "Mozilla/5.0";
    //private static final String BOT_ID = "58e3406f09337706ed30dc3872"; // MainChat
    private static final String BOT_ID = "098c1562a9131c63a182f5a6d9"; // WorkChat
    // private static final String GROUP_ID = "25931103"; //TestGroup
    // private static final String GROUP_ID = "18189190"; // MainChat
    private static final String GROUP_ID = "49864025"; // WorkChat
    private static final String ACCESS_TOKEN = "rP8PR8zBp5mk5p6Kyoy6yDo6b2EGCAGdW4YB9LH3";

    private static final Map<String, String> userIdToName = new HashMap<>();

    static {
        userIdToName.put("646634", "Aaron Adams");
        userIdToName.put("646551", "Ian Trapp");
        userIdToName.put("646563", "Ryan Burns");
        userIdToName.put("12099980", "Anthony Tran");
        userIdToName.put("646566", "Daniel Carlson");
        userIdToName.put("13884832", "Matthew Stowers");
        userIdToName.put("7657741", "James Bottoms");
        userIdToName.put("system", "GroupMe");
        userIdToName.put("355424", "Fantasy Football Bot");
        userIdToName.put("35939738", "Nick Kramer");
        userIdToName.put("6557679", "Alex Ferrara");
        userIdToName.put("259682", "Ryan's Little Helper");
        userIdToName.put("20876117", "Kyle Jones");
        userIdToName.put("10084716", "Nick Kramer");
        userIdToName.put("9446852", "Bryce Rich");
        userIdToName.put("20571181", "Katie Geeslin");
        userIdToName.put("15905102", "David Parsons");
        userIdToName.put("45888519", "Casey Mulroy");
        userIdToName.put("6471992", "Cory Hutson");
        userIdToName.put("30148103", "Peter Price");
        userIdToName.put("26671484", "Lucy Reynolds");
        userIdToName.put("14096462", "Nick Maguire");
        //userIdToName.put("", "");
    }

    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        String urlParameters = "";
        int count = Integer.MAX_VALUE;
        int totalMessages = Integer.MAX_VALUE;
        Map<String, Double> likesReceived = new HashMap<>();
        Map<String, Double> comments = new HashMap<>();
        Map<String, Double> likesGiven = new HashMap<>();
        Map<String, Double> selfLikes = new HashMap<>();

        while (count > 0) {
            URL url = new URL("https://api.groupme.com/v3/groups/" + GROUP_ID + "/messages?token=" + ACCESS_TOKEN
                    + urlParameters);

            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("User-Agent", USER_AGENT);

            BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            StringBuilder response = new StringBuilder();
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

            if (count == Integer.MAX_VALUE) {
                count = ((Long) responses.get("count")).intValue();
                totalMessages = ((Long) responses.get("count")).intValue();
            }

            JSONArray messages = (JSONArray) responses.get("messages");

            // Go through each message
            for (JSONObject message : (Iterable<JSONObject>) messages) {
                String userId = (String) message.get("user_id");
                String name = userIdToName.get(userId);

                if (name == null) {
                    System.out.println(message.get("name") + " - " + message.get("user_id"));
                    continue;
                }

                JSONArray favorites = (JSONArray) message.get("favorited_by");

                if (likesReceived.get(name) == null) {
                    likesReceived.put(name, (double) favorites.size());
                } else {
                    likesReceived.put(name, likesReceived.get(name) + favorites.size());
                }

                for (String favoritor : (Iterable<String>) favorites) {
                    likesGiven.merge(favoritor, 1D, (a, b) -> a + b);

                    if (favoritor.equals(userId)) {
                        selfLikes.merge(userIdToName.get(favoritor), 1D, (a, b) -> a + b);
                    }
                }

                // Add a count to the comment
                if (comments.get(name) == null) {
                    comments.put(name, 1D);
                } else {
                    comments.put(name, comments.get(name) + 1);
                }

                count--;
                lastMessageID = (String) message.get("id");
            }
            urlParameters = "&before_id=" + lastMessageID;
        }

        Map<String, Double> likesGivenByName = new HashMap<>();
        for (Map.Entry<String, Double> entry : likesGiven.entrySet()) {
            likesGivenByName.put(userIdToName.get(entry.getKey()), entry.getValue());
        }

        // Calculate the like to comment ratios for everyone
        Map<String, Double> ratiosForLikesReceived = new HashMap<>();
        for (String name : comments.keySet()) {
            Double numLikes = likesReceived.get(name);
            Double numComments = comments.get(name);
            ratiosForLikesReceived.put(name, numLikes != null ? ((double) numLikes) / numComments : 0);
        }

        Map<String, Double> ratioOfLikesGivenToLikesReceived = new HashMap<>();
        for (String name : likesGivenByName.keySet()) {
            Double selfLikesNullSafe = selfLikes.get(name) == null ? 0D : selfLikes.get(name);
            Double numLikesReceived = likesReceived.get(name) - selfLikesNullSafe;
            Double numLikesGiven = likesGivenByName.get(name);
            ratioOfLikesGivenToLikesReceived.put(name, ((double) numLikesGiven) / numLikesReceived);
        }

        likesReceived = sortByValue(likesReceived);
        comments = sortByValue(comments);
        ratiosForLikesReceived = sortByValue(ratiosForLikesReceived);
        likesGivenByName = sortByValue(likesGivenByName);
        ratioOfLikesGivenToLikesReceived = sortByValue(ratioOfLikesGivenToLikesReceived);
        selfLikes = sortByValue(selfLikes);

        System.out.println("Total Comments: " + comments.toString());
        System.out.println("Total Likes Received: " + likesReceived.toString());
        System.out.println("Likes Received per Comment: " + ratiosForLikesReceived.toString());
        System.out.println("Total Likes Given: " + likesGivenByName.toString());
        System.out.println("Likes Given per Likes Received: " + ratioOfLikesGivenToLikesReceived.toString());
        System.out.println("Self Likes: " + selfLikes.toString());

        System.out.println("\nProcessed " + totalMessages + " messages in "
                + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

        //postResults(comments, "\nTotal Comments:\n");
        //postResults(likesReceived, "\nTotal Likes Received:\n");
        //postResults(ratios, "\nLikes Received per Comment:\n");
        //postResults(likesGivenByName, "\nTotal Likes Given:\n");
        postResults(ratioOfLikesGivenToLikesReceived, "\nLikes Given per Likes Received (Minus Self Likes):\n");
        postResults(selfLikes, "\nSelf Likes:\n");
    }

    private static void postResults(Map<String, Double> result, String headerLine) throws Exception {
        URL url = new URL("https://api.groupme.com/v3/bots/post");
        HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("User-Agent", USER_AGENT);
        httpConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        StringBuilder formattedResult = new StringBuilder(headerLine);
        for (String name : result.keySet()) {
            formattedResult.append(name).append(": ").append(result.get(name)).append("\n");
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
     * Taken from
     * http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
     */
    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
