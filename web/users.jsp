<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản lý người dùng</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
    </head>
    <body>
        <%
            InforUser u = (InforUser) session.getAttribute("user");
            HttpSession ss = request.getSession();
            if (ss == null || u == null) {
                response.sendRedirect("./Connect.jsp");
            } else {
        %>
        <%@include file="include/header.jsp" %>
        <%@include file="include/dieuhuong.jsp" %>
        <h1 class="align-text-top">Welcome to User-Manager</h1>
        <p>- Trang main-machine:Thêm tab "users" cho phép quản lý từng user của máy tính (đổi mật khẩu, đổi thông tin, xóa user, thêm user)</p>

        <form action="<%=request.getContextPath()%>/editUser" method="post">

            <%
                String list = (String) ss.getAttribute("ds");
                if (list != null) {
                    String[] lines = list.split("\n");
                    int i = 1;
                    for (String line : lines) {
            %>
            <div class="d-flex me-3">
                <input type="hidden" name="host" value="${user.host}">
                <input type="hidden" name="port" value="${user.port}">
                <input type="hidden" name="user" value="${user.user}">
                <input type="hidden" name="password" value="${user.password}">

                <div><input type="text" name="useredit" value="<%= line%>" readonly></div>
                <div><button type="submit" name="choose" value="edit">Edit</button></div>
                <div><button type="submit" name="choose" value="delete">Delete</button></div>
            </div>

            <%
                    }
                }
            %>
            <div>
                <input type="text" name="newuser" placeholder="Nhập tên user cần thêm">
                <button type="submit" name="choose" value="add">Add User</button>
            </div>
        </form>
        <h1 style="color: red">${message}</h1>

        <%}%>
    </body>
</html>
