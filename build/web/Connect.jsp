<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="java.sql.* , Model.InforUser, java.util.*" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Connect</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <link rel="stylesheet" href="css/connect.css"/>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>
    </head>
    <body>
        <c:choose>
            <c:when test = "${nguoi_dung.role == '#' or nguoi_dung.role == 'admin'}">
            </c:when>
            <c:otherwise>
                <% request.setAttribute("error","Vui lòng liên hệ admin để kích hoạt máy chủ" );%>
                <% request.getRequestDispatcher("./login.jsp").forward(request, response);%>
            </c:otherwise>
        </c:choose>
        <c:if test = "${users == null}">
            <c:set var = "errmessage" value = "Vui lòng kết nối đến máy chủ trước!" />
        </c:if>
        <section class="h-100 gradient-form" style="background-color: #eee;">
            <div class="container py-sm-1 h-100">
                <div class="row d-flex justify-content-center align-items-center h-100">
                    <div class="col-xl-10">
                        <div class="card rounded-3 text-black">
                            <div class="row g-0">
                                <!--trai-->
                                <div class="col-lg-6">
                                    <div class="card-body py-sm-0 mx-md-4">
                                        <div class="text-center">
                                            <a href="Connect.jsp" ><img src="img/logo1.png" height="125px" width="125px;" alt="logo"/></a>
                                            <h4 class="mt-1 mb-5 pb-1">Connect to Ubuntu server</h4>
                                        </div>

                                        <form action="ConnectSSH" method="post">
                                            <p>Please connect to control server</p>
                                            <input type="hidden" name="vitri" value="trangConnect">
                                            <div data-mdb-input-init class="form-outline mb-4">
                                                <label class="form-label" for="ip">IP Address</label>
                                                <input type="text" name="host" id="ip" class="form-control"
                                                       placeholder="Please fill in the ip address" required="" />
                                            </div>
                                            <div data-mdb-input-init class="form-outline mb-4">
                                                <label class="form-label" for="port">Port</label>
                                                <input type="text" name="port" id="port" class="form-control"
                                                       value="22" required="" />
                                            </div>

                                            <div data-mdb-input-init class="form-outline mb-4">
                                                <label class="form-label" for="username">Username</label>
                                                <input type="text" name="user" id="username" class="form-control"
                                                       placeholder="Please fill in the username" required="" />
                                            </div>

                                            <div data-mdb-input-init class="form-outline mb-4">
                                                <label class="form-label" for="password">Password</label>
                                                <input type="password" name="password" id="password" class="form-control" 
                                                       placeholder="Please fill in the password" required="" />
                                            </div>

                                            <div class="text-center pt-1 mb-2 pb-1 ">
                                                <button style="font-weight: 700" data-mdb-button-init data-mdb-ripple-init 
                                                        class="btn btn-primary btn-block fa-lg gradient-custom-2 mb-3 me-1" type="submit">
                                                    Connect
                                                </button>
                                                <button style="font-weight: 700" data-mdb-button-init data-mdb-ripple-init 
                                                        class="btn btn-primary btn-block fa-lg gradient-custom-2 mb-3 ms-1" type="reset">
                                                    Cancel
                                                </button>
                                            </div>
                                        </form>

                                    </div>
                                </div>
                                <!--phai-->
                                <div class="col-lg-6 d-flex align-items-center gradient-custom-1">
                                    <form action="connects" method="post">
                                        <div class="text-center">
                                            <h4 class="mt-0 mb-3 pb-1">List of previously connected devices</h4>
                                            <hr class="mx-auto w-50">
                                        </div>
                                        <div class="machines">
                                            <% int k = 1;%>
                                            <sql:setDataSource var = "taikhoan" driver = "com.mysql.jdbc.Driver"
                                                               url = "jdbc:mysql://localhost:3306/storage?autoReconnect=true&useSSL=false"
                                                               user = "root"  password = ""/>
                                            <sql:query dataSource = "${taikhoan}" var = "result">
                                                SELECT * from tai_khoan;
                                            </sql:query>
                                            <c:forEach var = "row" items = "${result.rows}" begin = "0" end = "8">
                                                <div class="machine p-3 dropdown" onclick="populateForm('${row.host}', '${row.port}', '${row.user}')">
                                                    <div class="dropdown-toggle text-start" id="navbarDropdown" >
                                                        <div><h5>Machine <c:out value="<%=k%>"/></h5></div>
                                                        <div>
                                                            <c:if test = "${row.Disabled > 0}">
                                                                Status: <c:out value = "Disable"/>
                                                            </c:if>
                                                            <c:if test = "${row.isEnabled > 0}">
                                                                <c:out value = "Lost connection"/>
                                                            </c:if>
                                                        </div> 
                                                        <input type="checkbox" name="selected" id="select<%=k%>" value="${row.host}">
                                                        <input type="hidden" name="vitri" value="trangConnect">
                                                        <label for="select<%=k%>">
                                                            <img style="width: 100px; height: 100px;" src="img/pc1<%=k++%>.jpg" alt="alt"/>
                                                        </label>

                                                    </div>
                                                    <div class="dropdown-menu text-sm-start bg-light" aria-labelledby="navbarDropdown">
                                                        <div class="dropdown-item text-center" style="color: #333;font-weight: 700">
                                                            Thông tin máy chủ
                                                        </div>
                                                        <div class="dropdown-item bg-body">
                                                            IP: <input class="border-0" name="host_${row.host}" value="${row.host}" readonly="">
                                                        </div>
                                                        <div class="dropdown-item bg-body">
                                                            Port: <input class="border-0" name="port_${row.host}" value="${row.port}" readonly="">
                                                        </div>
                                                        <div class="dropdown-item bg-body">
                                                            Username: <input class="border-0" name="user_${row.host}" value="${row.user}" readonly="">
                                                        </div>
                                                        <div class="dropdown-item bg-body">
<!--     06/03                                              Password: <input type="password" name="password_${row.host}" placeholder="Nhập mật khẩu " >--> 
                                                            Password: <input type="password" name="password_${row.host}" value="1">
                                                        </div>
                                                        <div class="button-group dropdown-item bg-body">
                                                            <button data-mdb-button-init data-mdb-ripple-init 
                                                                    class="btn btn-primary btn-sm w-100 btn-block fa-lg gradient-custom-2 mb-3 me-1" type="submit" name="choose" value="connects">
                                                                Connect
                                                            </button>
                                                        </div> 
                                                    </div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                        <div class="p-2 justify-content-center">
                                            <div class="text-center pt-1 mb-2 pb-1 ">
                                                <button style="color: #ee4d2d; font-weight: 700" class="btn btn-light" type="submit" name="choose" value="connects">Multi-machine connection</button>
                                                <button style="color: #ee4d2d; font-weight: 100" class="btn btn-light" type="submit" name="choose" value="delete">Delete</button>
                                            </div> 
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div>
                <!--thong bao-->
                <c:if test = "${errmessage != null}">
                    <div id="message" class="alert alert-danger alert-dismissible">
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        <strong>Thất bại! </strong><c:out value ="${errmessage}"/>
                    </div>
                </c:if>
                <c:if test = "${message != null}">
                    <div id="message" class="alert alert-success alert-dismissible">
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                        <strong>Thành công! </strong><c:out value ="${message}"/>
                    </div>
                </c:if>
                <script>
                    setTimeout(function () {
                        document.getElementById("message").style.display = "none";
                    }, 10000);
                </script>
            </div>
        </section>

        <script>
            function populateForm(ip, port, username) {
                document.getElementById('ip').value = ip;
                document.getElementById('port').value = port;
                document.getElementById('username').value = username;
                document.getElementById('password').value = ''; // Clear password for security
            }
        </script>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>
</html>
