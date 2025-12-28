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
        <link rel="stylesheet" href="css/main-machine.css"/>
        <style>
            .listuser{
                display: grid;
                grid-template-columns: repeat(3, 1fr);
            }
        </style>


    </head>
    <body>
        <%
            InforUser u = (InforUser) session.getAttribute("user");
            List<InforUser> users = (List<InforUser>) session.getAttribute("users");
            HttpSession ss = request.getSession();
            if (ss == null || u == null || users == null) {
                response.sendRedirect("./Connect.jsp");
            } else {
        %>
        <%@include file="include/nav.jsp" %>
        <%@include file="include/background.jsp" %>
        <div class="container-fluid">
            <div class="col ms-5 me-5 text-center" style="margin-top: 6%">
                <!--danh sach nguoi dung-->
                <div class="col-10 ms-auto me-auto">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header p-1">
                            <div class="">
                                <div class="row">
                                    <div class="col-4">

                                    </div>
                                    <div class="col-4">
                                        <h4>Danh sách người dùng</h4>
                                    </div>
                                    <div class="col-4 text-end">
                                        <!-- Modal hiện add User -->
                                        <button id="addUser" type="button" class="btn btn-sm btn-secondary" data-bs-toggle="modal" data-bs-target="#staticBackdropaddUser">
                                            <i class="fa-solid fa-user-plus"></i> Add User
                                        </button>
                                    </div>
                                </div>
                                <div class="text-center">
                                    <p><i>Bạn có thể xem và chỉnh sửa người dùng.</i></p>
                                </div>
                            </div>
                        </div>
                        <div class="card-body listuser">
                            <%
                                String list = (String) ss.getAttribute("ds");
                                if (list != null) {
                                    String[] lines = list.split("\n");
                                    int i = 1;
                                    for (String index : lines) {
                                        String[] dsu = index.split(" ");
                                        String line = dsu[0];
                                        String email = dsu[1];
                            %>

                            <div>
                                <label for="<%= line%>">
                                    <img alt="avt" class="img-fluid" height="150" src="img/user.jpg" width="150"/>
                                    <h5>
                                        <%= line%>
                                        <input type="hidden" name="useredit" value="<%= line%>" readonly>
                                    </h5>
                                </label>
                                <!-- Modal hiện edit -->
                                <div>
                                    <button id="<%= line%>" type="button" class="btn btn-sm btn-secondary" data-bs-toggle="modal" data-bs-target="#staticBackdrop<%=i%>">
                                        <i class="fa-solid fa-user-pen me-1"></i>Chỉnh sửa
                                    </button>
                                </div>

                            </div> 

                            <!--Modal edit user đã ẩn--> 
                            <div class="modal fade" id="staticBackdrop<%=i%>" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabel<%=i%>" aria-hidden="true">
                                <div class="modal-dialog text-start">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <h1 class="modal-title fs-5" id="staticBackdropLabel<%=i%>">Edit User <strong><%= line%></strong></h1>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body col">
                                            <div class="row mt-2">
                                                <div class="text-center">
                                                    <h4>Đổi thông tin</h4>
                                                </div>
                                                <form action="<%=request.getContextPath()%>/editUser" method="post" class="">
                                                    <input type="hidden" name="useredit" value="<%= line%>">
                                                    <input type="hidden" name="host" value="${user.host}">
                                                    <input type="hidden" name="port" value="${user.port}">
                                                    <input type="hidden" name="user" value="${user.user}">
                                                    <input type="hidden" name="password" value="${user.password}">
                                                    <div class="form-group">
                                                        <label for="newusername"><i class="fa-regular fa-user"></i> Username (Tên, thư mục người dùng):</label> 
                                                        <input type="text" name="newusername" class="form-control" id="newusername" placeholder="New username">
                                                        <label for="newemail"><i class="fa-regular fa-envelope"></i> Email:</label> 
                                                        <input type="email" name="newemail" class="form-control" id="newemail" placeholder="Old email: <%=email%>">
                                                    </div>
                                                    <div class="form-group">
                                                        <div class="btn-group-sm text-center mt-3">
                                                            <button type="reset" class="btn btn-secondary">Reset</button>
                                                            <button type="submit" name="choose" value="doiinfo" class="btn btn-primary">Đổi thông tin</button>
                                                        </div>
                                                    </div>
                                                </form>
                                            </div>
                                            <div class="row mt-2 p-2">
                                                <hr>
                                                <div class="text-center">
                                                    <h4>Đổi mật khẩu</h4>
                                                </div>

                                                <form action="<%=request.getContextPath()%>/editUser" method="post" class="was-validated">
                                                    <input type="hidden" name="useredit" value="<%= line%>">
                                                    <input type="hidden" name="host" value="${user.host}">
                                                    <input type="hidden" name="port" value="${user.port}">
                                                    <input type="hidden" name="user" value="${user.user}">
                                                    <input type="hidden" name="password" value="${user.password}">
                                                    <div class="form-group">
                                                        <label for="exampleInputPassword1"><i class="fa-solid fa-lock"></i> Mật khẩu:</label>
                                                        <input type="text" name="newpass" class="form-control" id="exampleInputPassword1" placeholder="Password" required="">
                                                    </div>
                                                    <div class="form-group">
                                                        <label for="exampleInputPassword2"><i class="fa-solid fa-lock"></i> Nhập lại mật khẩu:</label>
                                                        <input type="text" name="renewpass" class="form-control" id="exampleInputPassword2" placeholder="Repassword" required="">
                                                    </div>
                                                    <div class="btn-group-sm text-center mt-3">
                                                        <button type="reset" class="btn btn-secondary">Reset</button> 
                                                        <button type="submit" name="choose" value="doimk" class="btn btn-primary">Đổi mật khẩu</button>
                                                    </div>
                                                </form>
                                            </div>
                                        </div>
                                        <div class="modal-footer">
                                            <p><i>Lưu ý: Hãy thận trọng khi xóa người dùng.</i></p>
                                            <form action="<%=request.getContextPath()%>/editUser" method="post" class="was-validated">
                                                <input type="hidden" name="useredit" value="<%= line%>">
                                                <input type="hidden" name="host" value="${user.host}">
                                                <input type="hidden" name="port" value="${user.port}">
                                                <input type="hidden" name="user" value="${user.user}">
                                                <input type="hidden" name="password" value="${user.password}">
                                                <button class="btn btn-danger btn-sm" type="submit" name="choose" value="delete" onclick="return confirm('Bạn có muốn xóa người dùng <%= line%>')"><i class="fa-solid fa-user-xmark"></i> Delete</button>
                                                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <%
                                        i++;
                                    }
                                }
                            %>                    
                        </div>
                    </div>
                    <!--Modal tao nguoi dung-->
                    <div class="modal fade" id="staticBackdropaddUser" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabeladdUser" aria-hidden="true">
                        <div class="modal-dialog text-start">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <i class="fa-solid fa-user-plus"></i> Add User
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body col">
                                    <div class="card border-0 shadow-sm p-0">
                                        <div class="card-header text-center">
                                            <h5>Tạo người dùng mới</h5>
                                        </div>
                                        <div class="card-body">
                                            <!-- First form group -->
                                            <form action="<%=request.getContextPath()%>/editUser" method="post">
                                                <input type="hidden" name="host" value="${user.host}">
                                                <input type="hidden" name="port" value="${user.port}">
                                                <input type="hidden" name="user" value="${user.user}">
                                                <input type="hidden" name="password" value="${user.password}">
                                                <div class="form-outline mb-4 text-start">
                                                    <label class="form-label" for="form2Example33"><i class="fa-regular fa-user"></i> Username</label>
                                                    <input type="text" name="newuser" id="form2Example33" class="form-control"
                                                           placeholder="Nhập tên user cần thêm" required="" >
                                                </div>
                                                <div class="form-outline mb-4 text-start">
                                                    <label class="form-label" for="form2Example44"><i class="fa-solid fa-lock"></i> Password</label>
                                                    <input type="text" name="newpass" id="form2Example44" class="form-control"
                                                           placeholder="Nhập tên user cần thêm" required="">
                                                </div>

                                                <div> 
                                                    <button class="btn btn-sm btn-primary me-3" type="submit" name="choose" value="add">Thêm người dùng</button>
                                                    <button class="btn btn-sm btn-secondary" type="reset" >Reset</button>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>


                    </div> 
                </div>

            </div>

        </div>
        <div>
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
        </div>
        <%}%>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>
</html>
