package com.jeremyhaberman.swingsetweb;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class PlaygroundServlet extends HttpServlet {

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		String name = req.getParameter("name");
		String description = req.getParameter("description");
//		double latitude = Double.parseDouble(req.getParameter("latitude"));
//		double longitude = Double.parseDouble(req.getParameter("longitude"));
		int latitudeE6 = Integer.parseInt(req.getParameter("latitude"));
		int longitudeE6 = Integer.parseInt(req.getParameter("longitude"));
		
//		int latitudeE6 = (int) (latitude * 1E6);
//		int longitudeE6 = (int) (longitude * 1E6);
		
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

//			resp.sendRedirect("/playgrounds.jsp");
		}

	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
	    String query = "select from " + Playground.class.getName();
	    List<Playground> playgrounds = (List<Playground>) pm.newQuery(query).execute();
	    Gson gson = new Gson();
	    
	    String allPlaygrounds = gson.toJson(playgrounds);
	    
	    resp.setContentType("application/json");
	    resp.getWriter().append(allPlaygrounds);	
	}

}
