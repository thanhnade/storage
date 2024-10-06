<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Cấu hình HTTP</title>
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
        
        <h2>Cấu hình HTTP</h2>
        <form action="configHTTP" method="post">
            <button type="submit" name="control" value="start">Start HTTP</button>
            <button type="submit" name="control" value="restart">Restart HTTP</button>
            <button type="submit" name="control" value="status">Status HTTP</button>
            <button type="submit" name="control" value="stop">Stop HTTP</button>
            <button type="submit" name="control" value="see">Xem file cấu hình</button>
            <input type="hidden" name="host" value="<%=u.getHost()%>">
            <input type="hidden" name="port" value="<%=u.getPort()%>">
            <input type="hidden" name="user" value="<%=u.getUser()%>">
            <input type="hidden" name="password" value="<%=u.getPassword()%>">
        </form>


        <form action="configHTTP" method="post">
            <div>
                <input type="hidden" name="host" value="<%=u.getHost()%>">
                <input type="hidden" name="port" value="<%=u.getPort()%>">
                <input type="hidden" name="user" value="<%=u.getUser()%>">
                <input type="hidden" name="password" value="<%=u.getPassword()%>">
            </div>
            <table border="1">
                <tr>
                    <th>STT</th>
                    <th>Tên dịch vụ</th>
                    <th>Install</th>
                    <th>Reinstall</th>
                    <th>Remove</th>
                    <th></th>
                </tr>
                <tr>
                    <td>1</td>
                    <td>MySql</td>
                    <td><input type="radio" name="choose" value="install"></td>
                    <td><input type="radio" name="choose" value="reinstall"></td>
                    <td><input type="radio" name="choose" value="remove"></td>
                    <td><button type="submit" name="control" value="mysql">OK</button></td>
                </tr>
                <tr>
                    <td>2</td>
                    <td>PHP</td>
                    <td><input type="radio" name="choose" value="install"></td>
                    <td><input type="radio" name="choose" value="reinstall"></td>
                    <td><input type="radio" name="choose" value="remove"></td>
                    <td><button type="submit" name="control" value="php">OK</button></td>
                </tr>
            </table>
        </form>
        <h3 style="color: green">${message}</h3>

        <div><pre>${see}</pre></div>



        <%}%>
    </body>
</html>
