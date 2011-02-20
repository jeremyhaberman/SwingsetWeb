package com.jeremyhaberman.swingsetweb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gps.utils.LatLonPoint;
import org.gps.utils.LatLonUtils;

import com.google.appengine.api.datastore.Key;
import com.google.gson.Gson;

public class PlaygroundServlet extends HttpServlet {

	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String TYPE = "type";
	private static final String RANGE = "range";

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String name = req.getParameter("name");
		String description = req.getParameter("description");
		int latitudeE6 = Integer.parseInt(req.getParameter("latitude"));
		int longitudeE6 = Integer.parseInt(req.getParameter("longitude"));

		Playground playground = new Playground(name, description, latitudeE6, longitudeE6);

		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.makePersistent(playground);
			resp.setContentType("text/plain");
			resp.getWriter().append("SUCCESS");
		} catch (Exception e) {
			resp.setContentType("text/plain");
			resp.getWriter().append("FAILED");
		} finally {
			pm.close();
		}

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		List<Playground> playgrounds = null;

		try {
			req.getParameter(TYPE);
		} catch (Exception e) {
			// TODO: Something needs to be done here
		}
		
		String rangeStr = req.getParameter(RANGE);
		if(rangeStr != null) {
			String latitudeStr = req.getParameter(LATITUDE);
			String longitudeStr = req.getParameter(LONGITUDE);

			double currentLatitude = Double.parseDouble(latitudeStr);
			double currentLongitude = Double.parseDouble(longitudeStr);
			int range = Integer.parseInt(rangeStr);

			playgrounds = getPlaygroundsWithinRange(new LatLonPoint(currentLatitude, currentLongitude),
					range);
		} else {
			playgrounds = getAllPlaygrounds();
		}

		Gson gson = new Gson();

		String allPlaygrounds = gson.toJson(playgrounds);

		resp.setContentType("application/json");
		resp.getWriter().append(allPlaygrounds);
	}

	List<Playground> getAllPlaygrounds() {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		String query = "select from " + Playground.class.getName();
		return (List<Playground>) pm.newQuery(query).execute();
	}

	List<Playground> getNearbyPlaygrounds(LatLonPoint currentLocation, int maxQuantity) {

		Map<Key, Playground> allPlaygrounds = toMap(getAllPlaygrounds());
		List<Playground> nearbyPlaygrounds = new ArrayList<Playground>();

		LatLonPoint currentPoint = new LatLonPoint(currentLocation.getLatitude(),
				currentLocation.getLongitude());

		Map<Key, Double> tempPlaygrounds = new TreeMap<Key, Double>();

		Set<Key> playgroundKeys = allPlaygrounds.keySet();

		Playground tempPlayground = null;
		Double distance = 0.0;
		for (Key current : playgroundKeys) {
			tempPlayground = allPlaygrounds.get(current);
			distance = getDistance(currentPoint, tempPlayground);
			tempPlaygrounds.put(tempPlayground.getKey(), distance);
		}

		Map<Key, Double> sortedPlaygrounds = sortByValue(tempPlaygrounds);

		int count = 0;
		Set<Key> keys = sortedPlaygrounds.keySet();
		Iterator<Key> keyIterator = keys.iterator();
		while (keyIterator.hasNext() && count < maxQuantity) {
			tempPlayground = allPlaygrounds.get(keyIterator.next());
			nearbyPlaygrounds.add(tempPlayground);
			count++;
		}

		return nearbyPlaygrounds;

	}
	
	List<Playground> getPlaygroundsWithinRange(LatLonPoint currentLocation, int rangeInMiles) {

		Map<Key, Playground> allPlaygrounds = toMap(getAllPlaygrounds());
		List<Playground> nearbyPlaygrounds = new ArrayList<Playground>();

		LatLonPoint currentPoint = new LatLonPoint(currentLocation.getLatitude(),
				currentLocation.getLongitude());

		Map<Key, Double> tempPlaygrounds = new TreeMap<Key, Double>();

		Set<Key> playgroundKeys = allPlaygrounds.keySet();
		
		double range = rangeInMiles * 1609.344;

		Playground tempPlayground = null;
		Double distance = 0.0;
		for (Key current : playgroundKeys) {
			tempPlayground = allPlaygrounds.get(current);
			distance = getDistance(currentPoint, tempPlayground);
			if(distance < range) {
				tempPlaygrounds.put(tempPlayground.getKey(), distance);
			}
		}

		Set<Key> keys = tempPlaygrounds.keySet();
		Iterator<Key> keyIterator = keys.iterator();
		while (keyIterator.hasNext()) {
			tempPlayground = allPlaygrounds.get(keyIterator.next());
			nearbyPlaygrounds.add(tempPlayground);
		}

		return nearbyPlaygrounds;

	}

	private Map<Key, Playground> toMap(List<Playground> allPlaygrounds) {
		Map<Key, Playground> playgroundMap = new HashMap<Key, Playground>();
		Iterator<Playground> playgroundIter = allPlaygrounds.iterator();
		Playground tempPlayground = null;
		while (playgroundIter.hasNext()) {
			tempPlayground = playgroundIter.next();
			playgroundMap.put(tempPlayground.getKey(), tempPlayground);
		}
		return playgroundMap;
	}

	private Double getDistance(LatLonPoint currentPoint, Playground currentPlayground) {

		double playgroundLatitude = currentPlayground.getLatitude() / 1E6;
		double playgroundLongitude = currentPlayground.getLongitude() / 1E6;

		LatLonPoint playgroundPoint = new LatLonPoint(playgroundLatitude, playgroundLongitude);

		return LatLonUtils.getHaversineDistance(currentPoint, playgroundPoint);
	}

	Map sortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2))
						.getValue());
			}
		});

		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private List<Playground> getPlaygroundsWithinBounds(LatLonPoint topLeft, LatLonPoint bottomRight) {
		List<Playground> allPlaygrounds = getAllPlaygrounds();
		List<Playground> playgroundsWithinBounds = new ArrayList<Playground>();

		double maxLat = topLeft.getLatitude();
		double minLat = bottomRight.getLatitude();
		double maxLong = topLeft.getLongitude();
		double minLong = bottomRight.getLongitude();

		double tempLat = 0.0;
		double tempLong = 0.0;

		Iterator<Playground> playgroundIter = allPlaygrounds.iterator();

		Playground tempPlayground = null;
		while (playgroundIter.hasNext()) {
			tempPlayground = playgroundIter.next();
			tempLat = tempPlayground.getLatitude() * 1.0 / 1E6;
			if (isInRange(tempLat, minLat, maxLat)) {
				tempLong = tempPlayground.getLongitude() * 1.0 / 1E6;
				if (isInRange(tempLong, minLong, maxLong)) {
					playgroundsWithinBounds.add(tempPlayground);
				}
			}
		}

		return playgroundsWithinBounds;
	}

	private boolean isInRange(double value, double min, double max) {

		assert (value >= -180.0 && value <= 180.0);
		assert (min >= -180.0 && min <= 180.0);
		assert (max >= -180.0 && max <= 180.0);

		if (value < min) {
			value += 360.0;
		}

		if (max < min) {
			max += 360.0;
		}

		return (value >= min) && (value <= max);
	}

}
