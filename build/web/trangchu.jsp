<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@page import="Model.InforUser, Model.NguoiDung, java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Trang chủ</title>
    <link rel="stylesheet" href="css/main.css" />
    <link rel="shortcut icon" type="image/png" href="img/logo1.png" />
    <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css" />
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap" rel="stylesheet">
    <script src="https://kit.fontawesome.com/2cbc3b080b.js" crossorigin="anonymous"></script>
    <style>
        html {
            overflow: auto;
            scrollbar-width: none;
            -ms-overflow-style: none;
        }

        html::-webkit-scrollbar {
            width: 0 !important;
            display: none;
        }
    </style>
</head>

<body style="background-color: #f5f5f5;">
    <% HttpSession ss = request.getSession();
            List<InforUser> users = (List<InforUser>) session.getAttribute("users");
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
            if (ss == null || users == null) {
                response.sendRedirect("./Connect.jsp");
            } else {
        %>

    <c:choose>
        <c:when test="${nguoi_dung.role == 'admin'}">
            <c:if test="${empty sessionScope.users}">
                <% response.sendRedirect("./Connect.jsp"); %>
            </c:if>
        </c:when>
        <c:otherwise>
            <c:if test="${nguoi_dung.role == 'client'}">
                <% request.setAttribute("error", "Bạn không có quyền truy cập Trang Chủ hệ thống"); %>
                <% request.getRequestDispatcher("./StorageServlet").forward(request, response); %>
            </c:if>
            <% response.sendRedirect("./login.jsp"); %>
        </c:otherwise>
    </c:choose>
    <%@include file="include/header.jsp" %>
    <%@include file="include/background.jsp" %>
    <div class="container-fluid text-center">
        <div class="row row-cols-1 ms-5 me-5" style="margin-top: 3%">
            <div class="col mb-3 p-3">
                <div>
                    <div class="row mt-3">
                        <div class="text-center">
                            <div>
                                <h3>DANH SÁCH MÁY CHỦ</h3>
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="d-flex justify-content-end">
                            <button type="button"
                                class="btn btn-outline-primary text-decoration-none pt-0 pb-0 text-center mb-sm-1 me-2"
                                data-bs-toggle="modal" data-bs-target="#connectModal">
                                Kết nối thêm máy chủ
                            </button>
                            <form action="logout" method="get">
                                <button
                                    class="btn btn-outline-danger text-decoration-none pt-0 pb-0 text-center mb-sm-1"
                                    type="submit" name="action" value="logoutAll"
                                    onclick="return confirm('Bạn có chắc chắn muốn đăng xuất tất cả không?');">
                                    Đăng xuất tất cả các máy
                                </button>
                            </form>
                        </div>
                    </div>
                </div>

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
                                <c:if test="${user.isEnabled > 0}">
                                    Trạng thái:
                                    <c:out value="Đang kết nối " />
                                </c:if>
                            </div>

                            <div class="button-group">
                                <form action="MachineManager" method="post">
                                    <button style="width: 345px" name="nav" value="mainmachine" type="submit">Quản
                                        lý máy chủ</button>
                                    <input type="hidden" name="host" value="${user.host}">
                                    <input type="hidden" name="port" value="${user.port}">
                                    <input type="hidden" name="user" value="${user.user}">
                                    <input type="hidden" name="password" value="${user.password}">
                                </form>
                            </div>

                            <div class="footer-links">
                                <a href="<%=request.getContextPath()%>/logout?host=${user.host}&user=${user.user}&port=${user.port}"
                                    onclick="return confirm('Bạn có chắc chắn muốn đăng xuất?');">Logout</a>
                                <a href="<%=request.getContextPath()%>/shutdown?host=${user.host}&port=${user.port}&user=${user.user}&password=${user.password}"
                                    onclick="return confirm('Bạn chắc chắn muốn tắt máy?');">Shut
                                    Down</a>
                            </div>
                        </div>
                        <% i++; %>
                    </c:forEach>
                </div>

            </div>
        </div>
        <div class="col mb-3 p-3">
            <div>
                <div class="text-centert">
                    <h3>DANH SÁCH MÁY ĐÃ KẾT NỐI</h3>
                </div>
                <form action="connects" method="post">
                    <div class="machines p-4">
                        <% int k = 1;%>
                        <sql:setDataSource var="taikhoan" driver="com.mysql.jdbc.Driver"
                            url="jdbc:mysql://localhost:3306/storage?autoReconnect=true&useSSL=false" user="root"
                            password="" />
                        <sql:query dataSource="${taikhoan}" var="result">
                            SELECT * FROM `tai_khoan` WHERE Disabled > 0;
                        </sql:query>

                        <c:forEach var="row" items="${result.rows}">
                            <div class="machine p-3 dropdown row">
                                <div class=" text-center col-6" id="navbarDropdown">
                                    <div class=" text-center" style="color: #333;font-weight: 700">
                                        Machine
                                        <c:out value="<%=k%>" />
                                    </div>
                                    <div class="d-inline-flex">
                                        <input type="hidden" name="vitri" value="trangChu">
                                        <label for="select<%=k%>">
                                            <img style="width: 100px; height: 100px;" src="img/pc1<%=k%>.jpg"
                                                alt="alt" />
                                        </label>
                                        <input type="checkbox" name="selected" id="select<%=k%>" value="${row.host}">
                                    </div>
                                    <%k++;%>
                                </div>
                                <div class="col-6 text-start" aria-labelledby="navbarDropdown">
                                    <div class=" text-center" style="color: #333;font-weight: 700">
                                        Thông tin máy chủ
                                    </div>
                                    <div>
                                        IP: ${row.host}
                                        <input type="hidden" class="border-0" name="host_${row.host}"
                                            value="${row.host}" readonly="">
                                    </div>
                                    <div>
                                        Port: ${row.port}
                                        <input type="hidden" class="border-0" name="port_${row.host}"
                                            value="${row.port}" readonly="">
                                    </div>
                                    <div>
                                        Username: ${row.user}
                                        <input type="hidden" class="border-0" name="user_${row.host}"
                                            value="${row.user}" readonly="">
                                    </div>
                                    <div>
                                        <input type="password" class="w-100" name="password_${row.host}"
                                            placeholder="Nhập mật khẩu ">
                                    </div>
                                    <div class="mt-1">
                                        <button style="font-weight: 700; background-color: #E95420; color: #fff"
                                            class="btn btn-sm" type="submit" name="choose" value="connects">
                                            Connect
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>

                    </div>
                </form>
                <div class="m-3">
                    <form action="connects" method="get"
                        onclick="return confirm('Bạn có chắc chắn muốn xóa các máy đã từng kết nối không?');">
                        <button style="font-weight: 700; background-color: #E95420; color: #fff" class="btn btn-sm"
                            type="submit" name="choose" value="clear">
                            Xóa các máy đã từng kết nối
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <%@include file="include/footer.jsp" %>
    </div>
    </div>
    <button type="button" class="btn btn-floating btn-lg" id="btn-back-to-top">
        <i class="fa-solid fa-arrow-up"></i>
    </button>

    <div>
        <!--thong bao-->
        <c:if test="${errmessage != null}">
            <div id="message" class="alert alert-danger alert-dismissible">
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                <strong>Thất bại! </strong>
                <c:out value="${errmessage}" />
            </div>
        </c:if>
        <c:if test="${message != null}">
            <div id="message" class="alert alert-success alert-dismissible">
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                <strong>Thành công! </strong>
                <c:out value="${message}" />
            </div>
        </c:if>
        <script>
            setTimeout(function() {
                document.getElementById("message").style.display = "none";
            }, 10000);
        </script>
    </div>
    <!-- Modal connects -->
    <div class="modal fade" id="connectModal" tabindex="-1" aria-labelledby="connectModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="connectModalLabel">Kết nối máy chủ mới</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <!-- <form action="connectSSH" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="vitri" value="trangChu">
                        <div class="mb-3">
                            <label for="hostInput" class="form-label">Địa chỉ IP</label>
                            <input type="text" class="form-control" id="hostInput" name="host" required>
                        </div>
                        <div class="mb-3">
                            <label for="portInput" class="form-label">Port</label>
                            <input type="number" class="form-control" id="portInput" name="port" value="22" required>
                        </div>
                        <div class="mb-3">
                            <label for="userInput" class="form-label">Tên đăng nhập</label>
                            <input type="text" class="form-control" id="userInput" name="user" required>
                        </div>
                        <div class="mb-3">
                            <label for="passwordInput" class="form-label">Mật khẩu</label>
                            <input type="password" class="form-control" id="passwordInput" name="password" required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                        <button type="submit" class="btn btn-primary">Kết nối</button>
                    </div>
                </form> -->
                <form action="ConnectSSH" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="vitri" value="trangChu">
                        <div data-mdb-input-init class="form-outline mb-4">
                            <label class="form-label" for="ip">IP Address</label>
                            <input type="text" name="host" id="ip" class="form-control"
                                placeholder="Please fill in the ip address" required="" />
                        </div>
                        <div data-mdb-input-init class="form-outline mb-4">
                            <label class="form-label" for="port">Port</label>
                            <input type="text" name="port" id="port" class="form-control" value="22" required="" />
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
                    </div>
                    <div class="modal-footer">
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                            <button type="submit" class="btn btn-primary">Kết nối</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        //Get the button
        let mybutton = document.getElementById("btn-back-to-top");
        // When the user scrolls down 20px from the top of the document, show the button
        window.onscroll = function() {
            scrollFunction();
        };

        function scrollFunction() {
            if (
                document.body.scrollTop > 20 ||
                document.documentElement.scrollTop > 20
            ) {
                mybutton.style.display = "block";
            } else {
                mybutton.style.display = "none";
            }
        }
        // When the user clicks on the button, scroll to the top of the document
        mybutton.addEventListener("click", backToTop);

        function backToTop() {
            document.body.scrollTop = 0;
            document.documentElement.scrollTop = 0;
        }
    </script>

    <% }%>
    <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
</body>

</html>