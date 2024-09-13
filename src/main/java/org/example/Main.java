package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.http.HypixelHttpClient;
import net.hypixel.api.http.HypixelHttpResponse;
import net.hypixel.api.reactor.ReactorHttpClient;
import net.hypixel.api.reply.AbstractReply;
import net.hypixel.api.reply.PlayerReply;
import net.hypixel.api.reply.RateLimitedReply;
import net.hypixel.api.util.ResourceType;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Main {
	static int limit = 104982;
	static int current = 105285;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ArrayList<ArrayList<String>> res = getPlayers("5d552867-9c1f-4c77-8e23-5005397ee293", 40);
		System.out.println();
		printList(res);
		ArrayList<ArrayList<String>> newRes = getHouses(res);
		System.out.println();
		printList(newRes);
    }
	public static ArrayList<ArrayList<String>> getPlayers(String initial, int times) throws ExecutionException, InterruptedException {
		HypixelHttpClient client = new ReactorHttpClient(UUID.fromString("c0d7163f-fd11-4f8a-856f-f724b949bcb0"));
		HypixelAPI hypixelAPI = new HypixelAPI(client);
		HashMap<String, Integer> Dict = new HashMap<String, Integer>(1000);
		ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>(64);
		int[] pointers = new int[64];
		for(int i = 0; i < 63; i++) {
			pointers[i] = -1;
		}
		pointers[63] = 0;
		for(int i = 0; i < 63; i++) {
			res.add(null);
		}
		PlayerReply apiReply;
		PlayerReply.Player player;
		initial = "\"" + initial + "\"";
		res.add(new ArrayList<String>(1));
		res.get(63).add(initial);
		pointers[0] = 63;
		Dict.put(initial, 0);
		int j = 0;
		while(
			//apiReply.getRateLimit().getRemaining() > 296
				j < times
		) {
			boolean goBack = true;
			System.out.print("-");
			System.out.println("Calling " + pointers[0] + " at index " + pointers[pointers[0]] + ": " + res.get(pointers[0]).get(pointers[pointers[0]]));
			apiReply = hypixelAPI.getPlayerByUuid(res.get(pointers[0]).get(pointers[pointers[0]]).substring(1, 37)).get();
			player = apiReply.getPlayer();
			if (!player.exists()) {
				System.err.println("Player not found!");
				return null;
			}
			pointers[pointers[0]]+=1;
			if(pointers[0] == 63) {
				pointers[0] = 0;
			}
			JsonObject housingMeta = player.getObjectProperty("housingMeta");
			for (String key : housingMeta.keySet()) {
				if(key.contains("given") && Integer.parseInt(key.substring(14)) < limit) {
					int week = Integer.parseInt(key.substring(14));
					JsonArray thing = housingMeta.getAsJsonArray(key);
					for(JsonElement uuid: thing) {
						if(uuid.toString().equals("\"16751f79-c0b1-4e53-a0b5-90d31fc1d80d\"")) {
							continue;
						}
						else if (Dict.get(uuid.toString()) != null && Dict.get(uuid.toString()) < (limit - week)) {
							res.get(limit - week).add(uuid.toString());
							res.get(Dict.get(uuid.toString())).remove(uuid.toString());
							Dict.replace(uuid.toString(), limit - week);
						} else if (Dict.get(uuid.toString()) != null) {
							continue;
						} else {
							if (res.get(limit - week) == null) {
								res.set(limit - week, new ArrayList<String>(200));
								pointers[limit - week] = 0;
							}
							res.get(limit - week).add(uuid.toString());
							if(limit - week > pointers[0] && limit - week != 47) {
								pointers[0] = limit - week;
								goBack = false;
							}
						}
					}
				}
			}
			if(goBack) {
				for(int i = pointers[0]; i > 0; i--) {
					if(pointers[i] > -1 && res.get(i).size() > pointers[i]) {
						pointers[0] = i;
						break;
					}
				}
			}
			j++;
		}
		res.set(63, null);
		hypixelAPI.shutdown();
		return res;
	}

	public static ArrayList<ArrayList<String>> getHouses(ArrayList<ArrayList<String>> houseIDs) throws ExecutionException, InterruptedException {
		ReactorHttpClient yuh = new ReactorHttpClient(UUID.fromString("c0d7163f-fd11-4f8a-856f-f724b949bcb0"));
		houseIDs.set(1, null);
		for(ArrayList<String> outer:houseIDs) {
			if(outer == null) {
				continue;
			}
			for(int i = 0; i < outer.size(); ++i) {
				HypixelHttpResponse res = yuh.makeAuthenticatedRequest("https://api.hypixel.net/v2/housing/house?house=" + outer.get(i).substring(1, 37)).get();
				if(res.getRateLimit().getRemaining() < 1) {
					System.out.println("Rate Limited!");
					yuh.shutdown();
					return houseIDs;
				}
				if(res.getStatusCode() == 404) {
					outer.remove(i);
					if(outer.isEmpty()) {
						break;
					}
					--i;
					continue;
				}
				System.out.println(res.getBody());
			}
		}
		yuh.shutdown();
		return houseIDs;
	}

	public static void printList(ArrayList<ArrayList<String>> list) {
		SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
		for(int i = 0; i < 64; i++) {
			if(list.get(i) != null) {
				long durationInMillis = (long)(current - (limit - i)) * 7 * 24 * 60 * 60 * 1000;
				long num = Instant.now().getEpochSecond()*1000 - durationInMillis;
				Date temp = new Date(num);
				System.out.println(jdf.format(temp) + "/" + (limit - i) + "/" + i + "(" + list.get(i).size() + "): " + list.get(i));
			}
		}
	}
}
