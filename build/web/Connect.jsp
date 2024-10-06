<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="java.sql.* , Model.InforUser" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Connect</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <link rel="stylesheet" href="css/connect.css"/>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>

    </head>
    <body>
        <main>
            <!--test-->
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
                                                <img src="img/logo1.png" height="125px" width="125px;" alt="logo"/>
                                                <h4 class="mt-1 mb-5 pb-1">Connect to Ubuntu server</h4>
                                            </div>

                                            <form action="ConnectSSH" method="post">
                                                <p>Please connect to control server</p>
                                                <div data-mdb-input-init class="form-outline mb-4">
                                                    <label class="form-label" for="form2Example33">IP Address</label>
                                                    <input type="text" name="host" id="form2Example33" class="form-control"
                                                           placeholder="Please fill in the ip address" required="" />
                                                </div>
                                                <div data-mdb-input-init class="form-outline mb-4">
                                                    <label class="form-label" for="form2Example44">Port</label>
                                                    <input type="text" name="port" id="form2Example44" class="form-control"
                                                           value="22" required="" />
                                                </div>

                                                <div data-mdb-input-init class="form-outline mb-4">
                                                    <label class="form-label" for="form2Example11">Username</label>
                                                    <input type="text" name="user" id="form2Example11" class="form-control"
                                                           placeholder="Please fill in the username" required="" />
                                                </div>

                                                <div data-mdb-input-init class="form-outline mb-4">
                                                    <label class="form-label" for="form2Example22">Password</label>
                                                    <input type="password" name="password" id="form2Example22" class="form-control" 
                                                           placeholder="Please fill in the password" required="" />
                                                </div>

                                                <div class="text-center pt-1 mb-2 pb-1 ">
                                                    <button data-mdb-button-init data-mdb-ripple-init 
                                                            class="btn btn-primary btn-block fa-lg gradient-custom-2 mb-3 me-1" type="submit">
                                                        Connect
                                                    </button>
                                                    <button data-mdb-button-init data-mdb-ripple-init 
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
                                                <h4 class="mt-1 mb-5 pb-1">List of connected devices</h4>
                                            </div>
                                            <div class="machines">
                                                <% int k = 1;%>
                                                <sql:setDataSource var = "taikhoan" driver = "com.mysql.jdbc.Driver"
                                                                   url = "jdbc:mysql://localhost:3306/nlcs?autoReconnect=true&useSSL=false"
                                                                   user = "root"  password = ""/>
                                                <sql:query dataSource = "${taikhoan}" var = "result">
                                                    SELECT * from tai_khoan;
                                                </sql:query>
                                                <c:forEach var = "row" items = "${result.rows}" begin = "1" end = "6">
                                                    <div class="machine p-3 dropdown" >
                                                        <div class="dropdown-toggle text-start" id="navbarDropdown">
                                                            <div><h5>Machine <c:out value="<%=k%>"/></h5></div>
                                                            <div>
                                                                <c:if test = "${row.Connected > 0}">
                                                                    Status: <c:out value = "Disable"/>
                                                                </c:if>
                                                                <c:if test = "${row.isConnecting > 0}">
                                                                    <c:out value = "Lost connection"/>
                                                                </c:if>
                                                            </div> 
                                                            <input type="checkbox" name="selected" id="select<%=k%>" value="${row.host}">
                                                            <label for="select<%=k%>">
                                                                <img style="width: 100px; height: 100px;" src="img/pc1<%=k++%>.jpg" alt="alt"/>
                                                            </label>
                                                            
                                                        </div>
                                                        <div class="dropdown-menu text-sm-starts" aria-labelledby="navbarDropdown">
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
                                            <div class="p-2 justify-content-center">
                                                <script>
                                                    setTimeout(function () {
                                                        document.getElementById("message").style.display = "none";
                                                    }, 5000);
                                                </script>
                                                <h6 class="text-center" style="color: #fff" id = message>${message}</h6>
                                                <div class="button-group text-center pt-1 mb-2 pb-1 ">
                                                    <button class="btn btn-success" type="submit" name="choose" value="connects">Multi-machine connection</button>
                                                    <button class="btn btn-secondary" type="submit" name="choose" value="delete">Delete</button>
                                                </div> 
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </main>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>
</html>
