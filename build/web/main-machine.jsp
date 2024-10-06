<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản lý máy chủ</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>
        <style>
            .terminal {
                width: 800px;
                height: 400px;
                background-color: #282a36; /* Màu nền tối phổ biến */
                border: 1px solid #444;
                font-family: monospace;
                color: #f8f8f2; /* Màu chữ sáng */
            }

            .terminal-header {
                background-color: #686868;
                color: #fff;
                padding: 10px;
                text-align: center;
            }

            .terminal-body {
                height: calc(100% - 30px); /* Trừ đi chiều cao header */
                overflow: auto;
            }

            .command-line {
                background-color: #383a42;
                padding: 10px;
                display: flex;
            }

            .command-line input {
                flex-grow: 1;
                background-color: transparent;
                border: none;
                color: inherit;
            }
        </style>
    </head>
    <body>
        <%
            InforUser u = (InforUser) session.getAttribute("user");
            HttpSession ss = request.getSession();
            if (ss == null || u == null) {
                response.sendRedirect("./Connect.jsp");
            } else {
        %>
        <header>
            <%@include file="include/header.jsp" %>
            <%@include file="include/dieuhuong.jsp" %>
        </header>
        <main>
            <h2 class="text-center">Welcome to Machine Manager</h2>
            <div class="terminal">
                <div class="terminal-header">Terminal</div>
                <div class="terminal-body">
                    <div class="command-line">
                        <form action="<%=request.getContextPath()%>/terminal" method="post">
                            <input type="text" name="str" placeholder="Nhap lenh o day">
                            <button type="submit">Send</button>
                            <input type="hidden" name="host" value="${user.host}">
                            <input type="hidden" name="port" value="${user.port}">
                            <input type="hidden" name="user" value="${user.user}">
                            <input type="hidden" name="password" value="${user.password}">
                        </form>
                    </div>
                    <div class="output">
                        <c:forEach var="line" items="${outputLines}">
                            <p>${line}</p>
                        </c:forEach>
                        <br/>
                        <p>${exitStatus}</p>

                    </div>
                </div>
            </div>
        </main>
        <%}%>
    </body>
</html>
