<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Trang chu</title>
        <link rel="stylesheet" href="css/main.css"/>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>

    </head>
    <body>
        <%
            HttpSession ss = request.getSession();
            List<InforUser> users = (List<InforUser>) session.getAttribute("users");
            if (ss == null || users == null) {
                response.sendRedirect("./Connect.jsp");
            } else {
        %>
        <header class="mb-3">
            <%@include file="include/header.jsp" %>
            <c:if test="${empty sessionScope.users}">
                <% response.sendRedirect("./Connect.jsp");%>
            </c:if>
        </header>

        <main>
            <div class="container text-center">
                <div class="row row-cols-1">
                    <div class="col mb-3">
                        <div class="text-center">
                            <h3>DANH SÁCH MÁY CHỦ</h3>
                        </div>
                    </div>
                    <div class="col mb-3">
                        <div class="list-machines">
                            <% int i = 1;%>
                            <c:forEach var="user" items="${sessionScope.users}">
                                <div class="machine">
                                    <h4>Thông tin Máy Chủ <%=i%></h4>
                                    <div>
                                        <label for="ip">IP address</label>
                                        <input type="text" id="ip" name="host" value="${user.host}" disabled>
                                    </div>
                                    <div>
                                        <label for="port">Port</label>
                                        <input type="text" id="port" name="port" value="${user.port}" disabled>
                                    </div>

                                    <div class="status">
                                        <c:if test = "${user.isConnecting > 0}">
                                            Trạng thái: <c:out value = "Đang kết nối "/>
                                        </c:if>
                                    </div>

                                    <div class="button-group">
                                        <form action="<%=request.getContextPath()%>/MachineManager" method="post">
                                            <button style="width: 345px" type="submit">Quản lý máy chủ</button>
                                            <input type="hidden" name="host" value="${user.host}">
                                            <input type="hidden" name="port" value="${user.port}">
                                            <input type="hidden" name="user" value="${user.user}">
                                            <input type="hidden" name="password" value="${user.password}">
                                        </form>
                                    </div>

                                    <div class="footer-links">
                                        <a href="<%=request.getContextPath()%>/logout?host=${user.host}&user=${user.user}" 
                                           onclick="return confirm('Bạn có chắc chắn muốn đăng xuất?');">Logout</a>
                                        <a href="<%=request.getContextPath()%>/shutdown?host=${user.host}&port=${user.port}&user=${user.user}&password=${user.password}" 
                                           onclick="return confirm('Bạn chắc chắn muốn tắt máy?');">Shut Down</a>
                                    </div>
                                </div>
                                <% i++; %>
                            </c:forEach>
                        </div>
                    </div>
                    <div class="col mb-3">
                        <div class="">
                            <form action="connects" method="post">
                                <div class="text-centert">
                                    <h3 class="mt-1 mb-5 pb-1">DANH SÁCH MÁY ĐÃ KẾT NỐI</h3>
                                </div>
                                <div class="machines">
                                    <% int k = 1;%>
                                    <sql:setDataSource var = "taikhoan" driver = "com.mysql.jdbc.Driver"
                                                       url = "jdbc:mysql://localhost:3306/nlcs?autoReconnect=true&useSSL=false"
                                                       user = "root"  password = ""/>
                                    <sql:query dataSource = "${taikhoan}" var = "result">
                                        SELECT * from tai_khoan where Connected="1";
                                    </sql:query>
                                    <c:forEach var = "row" items = "${result.rows}" begin = "1" end = "6">
                                        <div class="machine p-3 dropdown row" >
                                            <div class=" text-start col" id="navbarDropdown">
                                                <div><h5>Machine <c:out value="<%=k%>"/></h5></div>
                                                <div>
                                                    <c:if test = "${row.Connected > 0}">
                                                        Status: <c:out value = "Disable"/>
                                                    </c:if>
                                                    <c:if test = "${row.isConnecting > 0}">
                                                        <c:out value = "Đang kết nối"/>
                                                    </c:if>
                                                </div> 
                                                <div>
                                                    <input type="checkbox" name="selected" id="select<%=k%>" value="${row.host}">
                                                    <label for="select<%=k%>">
                                                        <img style="width: 100px; height: 100px;" src="img/pc1<%=k++%>.jpg" alt="alt"/>
                                                    </label>
                                                </div>
                                                
                                                
                                            </div>
                                            <div class="col" aria-labelledby="navbarDropdown">
                                                <h6>THÔNG TIN MÁY</h6>
                                                <input class="dropdown-item" name="host_${row.host}" value="${row.host}" readonly="">
                                                <input class="dropdown-item" name="port_${row.host}" value="${row.port}">
                                                <input class="dropdown-item" name="user_${row.host}" value="${row.user}" readonly="">
                                                <input class="dropdown-item" name="password_${row.host}" placeholder="Nhập mật khẩu " >
                                                <div class="button-group dropdown-item">
                                                    <button class="btn btn-success btn-sm w-100" type="submit" name="choose" value="connects">Connect</button>
                                                </div> 
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>



        </main>
        <%
            }
        %>
    </body>
</html>
