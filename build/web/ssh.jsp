<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>SSH</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="css/main-machine.css"/>
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap" rel="stylesheet">
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
            <div class="col ms-5 me-5" style="margin-top: 5%">
                <div class="row">
                    <h4 class="text-center">DỊCH VỤ SSH</h4>
                </div>
                <!--dich vu ssh-->
                <div id="ssh" class="row bg-white border-top p-3"> 
                    <div class="col-4">
                        <div class="card border-0 shadow-sm mb-5">
                            <div class="card-header text-center">
                                <h5 class="mb-0">Đổi cổng dịch vụ SSH</h5>
                                <div class="visible d-block text-end" onclick="toggleDiv('congssh')"><i>Close/Open</i></div>
                            </div>
                            <div class="card-body pb-1" id="congssh">
                                <!-- First form group -->
                                <form action="configSSH" method="post">
                                    <div class="form-group">
                                        <label for="newPort">New Port:</label>
                                        <div class="input-group input-group-sm">
                                            <input type="number" name="newport" min="1" max="65535" required id="newPort" class="form-control" placeholder="Nhập cổng mới">
                                            <div class="input-group-append">
                                                <button class="btn btn-success btn-sm me-2" type="submit" name="control" value="changePort">Enter</button>
                                            </div>
                                        </div>
                                    </div>
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                </form>

                                <!-- Second form group -->
                                <form action="configSSH" method="post">
                                    <div class="form-group">
                                        <label for="portFrom">Change Port:</label>
                                        <div class="input-group input-group-sm">
                                            <input type="number" name="oldport" min="1" max="65535" id="portFrom" class="form-control" placeholder="From" required="">
                                            <input type="number" name="newport" min="1" max="65535" class="form-control ml-2" placeholder="To" required="">
                                            <div class="input-group-append ml-2">
                                                <button class="btn btn-success btn-sm me-2" type="submit" name="control" value="changeOldPort">Enter</button>
                                            </div>
                                        </div>
                                    </div>
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                </form>
                                <div class="mt-3">
                                    <p><i>Lưu ý: hãy xem file cấu hình trước khi đổi cổng.</i></p>
                                </div>
                            </div>
                        </div>

                        <div class="card border-0 shadow-sm mb-3">
                            <div class="card-header">
                                <div class="text-center ">
                                    <h5>Người dùng đang sử dụng SSH</h5>
                                </div>
                            </div>
                            <div class="card-body pt-0">
                                <table class="table">
                                    <thead>
                                        <tr>
                                            <th>User</th>
                                            <!--<th>Loại thiết bị</th>-->
                                            <th>Ngày </th>
                                            <th>Giờ</th>
                                            <th>Máy kết nối</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        <c:forEach var="line" items="${useSSH}">
                                            <tr class="border-bottom">
                                                <c:set var="row" value="${fn:split(line, ' ')}" />
                                                <c:forEach var="col" items="${row}" varStatus="status">
                                                    <c:if test="${status.index != 1}"> <!-- Bỏ qua phần tử thứ 2 -->
                                                        <td>${col}</td>
                                                    </c:if>
                                                </c:forEach>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>

                            </div>
                        </div>

                    </div>
                    <div class="col-4">
                        <div class="card border-0 shadow-sm mb-3">
                            <div class="card-header text-center">
                                <h5>File cấu hình dịch vụ SSH</h5>
                            </div>
                            <div class="card-body pt-3" >
                                <form action="configSSH" method="post">
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                    <div id="fileSSH" style="width: 100%; max-height: 400px; overflow-y: auto;">
                                        <% int ssh = 1;%>
                                        <c:forEach var="line" items="${seeSSH}">
                                            <div class="lh-lg">
                                                <c:choose>
                                                    <c:when test="${fn:contains(line, '0') || fn:contains(line, '1') || fn:contains(line, '2') || fn:contains(line, '3') || 
                                                                    fn:contains(line, '4') || fn:contains(line, '5') || fn:contains(line, '6') || fn:contains(line, '7') || 
                                                                    fn:contains(line, '8') || fn:contains(line, '9')}">

                                                        <!--<input class="w-100 text-danger border-0" name="line" type="text" value="${line}">-->
                                                            <div class="d-flex align-items-center border-bottom">
                                                                <label class="text-danger border-0 me-3" for="ssh<%=ssh%>" >${fn:substring(line, 0, fn:indexOf(line, ' ')+1)}</label>
                                                                <input type="hidden" name="line0" value='${fn:substring(line, 0, fn:indexOf(line, ' ')+1)}'>
                                                                <input id="ssh<%=ssh++%>" class="text-danger border-0 w-100" type="text" name="line1" value='${fn:substring(line, fn:indexOf(line, ' ')+1, fn:length(line))}'>
                                                            </div>

                                                    </c:when>
                                                    <c:otherwise>
                                                        <!--<input class="w-100 border-0" name="line" type="text" value="${line}">-->
                                                        <div class="d-flex align-items-center border-bottom">
                                                            <c:if test="${not empty fn:substringBefore(line, ' ') && not empty fn:substringAfter(line, ' ')}">
                                                                <label class="me-3" for="sshx<%=ssh%>" >${fn:substring(line, 0, fn:indexOf(line, ' ')+1)}</label>
                                                                <input type="hidden" name="line0" value='${fn:substring(line, 0, fn:indexOf(line, ' ')+1)}'>
                                                                <input id="sshx<%=ssh++%>" class="border-0 w-100" type="text" name="line1" value='${fn:substring(line, fn:indexOf(line, ' ')+1, fn:length(line))}'>
                                                            </c:if>
                                                            <c:if test="${ empty fn:substringBefore(line, ' ') && empty fn:substringAfter(line, ' ')}">
                                                                <!--<input class="border-0 w-100" type="text" name="endline" value="${line}">-->
                                                                <label class="me-3" for="sshx<%=ssh%>" >${fn:substring(line, 0, fn:indexOf(line, '/'))}</label>
                                                                <input type="hidden" name="line0" value='${fn:substring(line, 0, fn:indexOf(line, '/'))}'>
                                                                <input id="sshx<%=ssh++%>" class="border-0 w-100" type="text" name="line1" value='${fn:substring(line, fn:indexOf(line, '/'), fn:length(line))}'>
                                                            </c:if>
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <%ssh++;%>
                                        </c:forEach>
                                    </div>
                                    <div class="d-flex">
                                        <input class="w-100 border-0" type="text" name="line0" placeholder="Tên tham số cấu hình">
                                        <input class="w-100 border-0" type="text" name="line1" placeholder="Giá trị của tham số">
                                    </div>

                                    <div class="btn-group-sm mt-1">
                                        <button class="btn btn-success m-2" type="submit" name="control" value="edit">Update</button>
                                        <button class="btn btn-info m-2" type="submit" name="control" value="see">Show</button>
                                    </div>
                                </form>
                            </div>
                        </div>

                    </div>
                    <div class="col-4">
                        <div class="card border-0 shadow-sm mb-5">
                            <div class="card-header d-flex justify-content-center pt-2">
                                <h5>Điều khiển dich vụ SSH</h5>
                            </div>
                            <div class="card-body">
                                <form action="configSSH" method="post">
                                    <div class="btn-group d-flex justify-content-sm-around">
                                        <button class="btn btn-success me-2" type="submit" name="control" value="start">Start SSH</button>
                                        <button class="btn btn-warning me-2" type="submit" name="control" value="restart">Restart SSH</button>
                                        <button class="btn btn-info me-2" type="submit" name="control" value="status">Status SSH</button>
                                        <!--<button class="btn btn-secondary me-2" type="submit" name="control" value="see">Xem file cấu hình SSH</button>-->
                                    </div>
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                </form> 
                            </div>

                        </div>

                        <div class="card border-0 shadow-sm">
                            <div class="card-header text-center">
                                <h5>Deny User</h5>
                            </div>
                            <div class="card-body">

                                <table class="table table-container text-center">
                                    <thead>
                                        <tr>
                                            <th scope="col">#</th>
                                            <th scope="col">Username</th>
                                            <th scope="col">Choose</th>
                                            <th>Allow</th>
                                            <th>Deny</th>
                                            <th></th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                    <form action="configSSH" method="post">
                                        <input type="hidden" name="host" value="<%=u.getHost()%>">
                                        <input type="hidden" name="port" value="<%=u.getPort()%>">
                                        <input type="hidden" name="user" value="<%=u.getUser()%>">
                                        <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                        <%
                                            String list = (String) ss.getAttribute("ds");
                                            if (list != null) {
                                                String[] lines = list.split("\n");
                                                int i = 1;
                                                for (String row : lines) {
                                                    if (!row.contains(u.getUser())) {
                                                        String[] dsu = row.split(" ");
                                                        String line = dsu[0];

                                        %>
                                        <tr>
                                            <th scope="row"><%=i%></th>
                                            <td>
                                                <label for="<%=line%><%=i%>"><%=line%></label> 
                                            </td>
                                            <td><input id="<%=line%><%=i%>" type="radio" name="useredit" value="<%=line%>" required=""></td>
                                            <td><button class="btn btn-secondary btn-sm text-bg-success" type="submit" name="control" value="allow"><i class="fa-solid fa-user-check"></i></button></td>
                                            <td><button class="btn btn-secondary btn-sm text-bg-danger" type="submit" name="control" value="deny"><i class="fa-solid fa-user-slash"></i></button></td>
                                            <td><button class="btn btn-secondary btn-sm" type="reset" ><i class="fa-solid fa-rotate-right"></i></button></td>
                                        </tr>
                                        <%
                                                        i++;
                                                    }
                                                }
                                            }
                                        %>
                                    </form> 
                                    </tbody>
                                </table>



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
        <%
            }
        %>

        <script>
            //ẩn hiện thẻ div
            function toggleDiv(divId) {
                const div = document.getElementById(divId);
                div.classList.toggle("d-none");
            }
        </script>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>
</html>
