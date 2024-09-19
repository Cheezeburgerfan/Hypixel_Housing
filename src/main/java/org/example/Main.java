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
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Main {
	static int limit = 104982;
	static int current = 105286;
	static String APIKey = "473e9517-7765-47bc-bc91-4687abc10e02";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ArrayList<ArrayList<String>> res = getPlayers("5d552867-9c1f-4c77-8e23-5005397ee293", 50);
		System.out.println();
		Thread.sleep(1000);
		printList(res);
		ArrayList<ArrayList<String>> newRes = getHouses(res);
		System.out.println();
		Thread.sleep(1000);
		printList(newRes);
		//getPlayerStats(newRes);
    }
	public static ArrayList<ArrayList<String>> getPlayers(String initial, int times) throws ExecutionException, InterruptedException {
		HypixelHttpClient client = new ReactorHttpClient(UUID.fromString(APIKey));
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
			apiReply = hypixelAPI.getPlayerByUuid(res.get(pointers[0]).get(pointers[pointers[0]])).get();
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
					if(limit - week == 45) {
						continue;
					}
					JsonArray thing = housingMeta.getAsJsonArray(key);
					for(JsonElement uuid: thing) {
						//this is to remove the quotation marks around the JSON value
						String suuid = uuid.toString().substring(1, 37);
						//this is to prevent TimeDeo from breaking things
						if(suuid.toString().equals("16751f79-c0b1-4e53-a0b5-90d31fc1d80d")) {
							continue;
						}
						//if this uuid was already seen, check if this time its earlier week, if it is update the dict and res
						else if (Dict.get(suuid.toString()) != null && Dict.get(suuid.toString()) < (limit - week) && Dict.get(suuid)!=0) {
							res.get(limit - week).add(suuid.toString());
							//System.out.println(suuid);
							//System.out.println(Dict.get(suuid));
							//printList(res);
							res.get(Dict.get(suuid.toString())).remove(suuid.toString());
							Dict.replace(suuid.toString(), limit - week);
						//if its not an earlier week, just continue
						} else if (Dict.get(suuid.toString()) != null) {
							continue;
						//this is if its a new name
						} else {
							if (res.get(limit - week) == null) {
								res.set(limit - week, new ArrayList<String>(200));
								pointers[limit - week] = 0;
							}
							res.get(limit - week).add(suuid.toString());
							Dict.put(suuid, limit - week);
							if(limit - week > pointers[0] && limit - week != 45) {
								pointers[0] = limit - week;
								goBack = false;
							}
						}
					}
				}
				/**
				else if(key.equals("packages")) {
					JsonArray packages = housingMeta.get("packages").getAsJsonArray();
					for(int f = 0; f < packages.size(); f++) {
						if(packages.get(f).toString().contains("theme") || packages.get(f).toString().contains("specialoccasion") || packages.get(f).toString().contains("furniture") || packages.get(f).toString().contains("weather")) {
							continue;
						}
						System.out.println(packages.get(f));
					}
				}
				**/
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
		ReactorHttpClient yuh = new ReactorHttpClient(UUID.fromString(APIKey));
		//houseIDs.set(1, null);
		for(ArrayList<String> outer:houseIDs) {
			if(outer == null) {
				continue;
			}
			System.out.println(limit - houseIDs.indexOf(outer) + ":");
			for(int i = 0; i < outer.size(); ++i) {
				HypixelHttpResponse res = yuh.makeAuthenticatedRequest("https://api.hypixel.net/v2/housing/house?house=" + outer.get(i)).get();
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
				String[] parts = res.getBody().split("\"");
				System.out.println(parts[3] + " " + parts[11]);
				/**
				if(Integer.parseInt(parts[20].charAt(1)) > 0) {
					System.out.println("	Above house has a cookie!!");
				}
				**/
			}
		}
		yuh.shutdown();
		return houseIDs;
	}

	public static void getPlayerStats(List<ArrayList<String>> uuids) throws ExecutionException, InterruptedException {
		HypixelHttpClient client = new ReactorHttpClient(UUID.fromString(APIKey));
		HypixelAPI hypixelAPI = new HypixelAPI(client);
		PlayerReply apiReply;
		PlayerReply.Player player;
		long earliest = 1726336930L;
		SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
		for(ArrayList<String> outer:uuids) {
			if(outer == null) {
				continue;
			}
			for(String uuid:outer) {	
				apiReply = hypixelAPI.getPlayerByUuid(uuid).get();
				player = apiReply.getPlayer();
				if (!player.exists()) {
					System.err.println("Player not found!");
					return;
				}
				long lastLogin = player.getLongProperty("lastLogin", 0L);
				long firstLogin = player.getLongProperty("firstLogin", 0L);
				long lastLogout = player.getLongProperty("lastLogout", 0L);
				if(lastLogin > lastLogout) {
					System.out.print("Online Now!");
				}
				if(lastLogin < earliest && lastLogin > 1431645593L) {
					earliest = lastLogin;
				}
				String name = player.getName().toString();
				Date temp = new Date(lastLogin);
				Date first = new Date(firstLogin);
				//System.out.printf("%-16s %-11s %-11s %d%n", name, jdf.format(temp), jdf.format(first), adventint);
				System.out.printf("%-16s %-11s%n", name, jdf.format(temp));
				if(apiReply.getRateLimit().getRemaining() < 1) {
					System.out.println("Rate Limited!");
					return;
				}
			}
		}
		Date earl = new Date(earliest);
		System.out.println("Earliest: " + jdf.format(earl));
		client.shutdown();
		return;
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
