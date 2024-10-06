<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Edit User</title>
    </head>
    <body>
        <%
            InforUser u = (InforUser) session.getAttribute("user");
        %>
        <form action="<%=request.getContextPath()%>/editUser" method="post">
            <h1>Edit User ${editUser}</h1>
            <input type="hidden" name="useredit" value="${editUser}">
            <div>
                Mật khẩu:<input type="text" name="newpass">
                Nhập lại khẩu:<input type="text" name="renewpass">
                <button type="submit" name="choose" value="newpass">Đổi mật khẩu</button>
            </div>
            <a>Đổi thông tin</a>
        </form>

    </body>
</html>
