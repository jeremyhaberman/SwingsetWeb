<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.jdo.PersistenceManager" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="com.jeremyhaberman.swingsetweb.Playground" %>
<%@ page import="com.jeremyhaberman.swingsetweb.PMF" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css" />
  </head>
  <body>

<%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
%>
<p>Hello, <%= user.getNickname() %>! (You can
<a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">sign out</a>.)</p>
<%
    } else {
%>
<p>Hello!
<a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>
to include your name with greetings you post.</p>
<%
    }
%>
<p><h1>Playgrounds</h1></p>
<%
    PersistenceManager pm = PMF.get().getPersistenceManager();
    String query = "select from " + Playground.class.getName();
    List<Playground> playgrounds = (List<Playground>) pm.newQuery(query).execute();
    if (playgrounds.isEmpty()) {
%>
<p>There are currently no playgrounds.</p>
<%
    } else {
        for (Playground g : playgrounds) {
            if (g.getName() == null) {
%>
<p>An anonymous person wrote:</p>
<%
            } else {
%>

<%= g.getName() %> (<%= g.getDescription() %>) Lat: <%= g.getLatitude() %>, Long: <%= g.getLongitude() %><br/>
<%
            }
        }
    }
    pm.close();
%>
<br/>
    <form action="/playground" method="post">
      <div><textarea name="name" rows="1" cols="30"></textarea></div>
      <div><textarea name="description" rows="1" cols="60"></textarea></div>
      <div><textarea name="latitude" rows="3" cols="20"></textarea></div>
      <div><textarea name="longitude" rows="3" cols="20"></textarea></div>
      <div><input type="submit" value="Add Playground" /></div>
    </form>

  </body>
</html>