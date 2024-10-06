<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>FTP Config</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
    </head>
    <body>
        <%  
            InforUser u = (InforUser)session.getAttribute("user");
            HttpSession ss = request.getSession();
               if ( ss == null || u == null) {
                   response.sendRedirect("./Connect.jsp");
               }
               else{
        %>
        <%@include file="include/header.jsp" %>
        <%@include file="include/dieuhuong.jsp" %>

        <h2>Cấu hình FTP</h2>
        <form action="configFTP" method="post">
            <button type="submit" name="control" value="start">Start FTP</button>
            <button type="submit" name="control" value="restart">Restart FTP</button>
            <button type="submit" name="control" value="status">Status FTP</button>
            <button type="submit" name="control" value="stop">Stop FTP</button>
            <button type="submit" name="control" value="see">Xem file cấu hình</button>
            <button type="submit" name="control" value="">Tính năng sắp ra mắt</button>
            <button type="submit" name="control" value="">Up File</button>
            <button type="submit" name="control" value="">Down File</button>


            <input type="hidden" name="host" value="<%=u.getHost()%>">
            <input type="hidden" name="port" value="<%=u.getPort()%>">
            <input type="hidden" name="user" value="<%=u.getUser()%>">
            <input type="hidden" name="password" value="<%=u.getPassword()%>">
        </form>
        <h3 style="color: green">${message}</h3>

        <div><pre>${see}</pre></div>

        <%
            }
        %>
    </body>
</html>
