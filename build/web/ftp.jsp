<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>FTP</title>
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
                    <h4 class="text-center">DỊCH VỤ FTP</h4>
                </div>
                <!--dich vu ftp-->
                <div id="ftp" class="row bg-white p-3 border-top">                   
                    <div class="col-5 ms-auto">
                        <div class="card border-0 shadow-sm">
                            <div class="card-header text-center">
                                <h5>File cấu hình dịch vụ FTP</h5>
                            </div>
                            <div class="card-body" style="padding-bottom: 7px">
                                <form action="configFTP" method="post">
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                    <div id="fileFTP" style="width: 100%; max-height: 400px; overflow-y: auto;">
                                        <% int ftp = 1;%>
                                        <c:forEach var="line" items="${seeFTP}">
                                            <div class="lh-lg">
                                                <c:choose>
                                                    <c:when test="${fn:contains(line, '0') || fn:contains(line, '1') || fn:contains(line, '2') || fn:contains(line, '3') || 
                                                                    fn:contains(line, '4') || fn:contains(line, '5') || fn:contains(line, '6') || fn:contains(line, '7') || 
                                                                    fn:contains(line, '8') || fn:contains(line, '9')}">
                                                            <div class="d-flex align-items-center border-bottom">
                                                                <label class="text-danger me-3" for="ftp<%=ftp%>" >${fn:substring(line, 0, fn:indexOf(line, '='))}</label>
                                                                <input type="hidden" name="line0" value="${fn:substring(line, 0, fn:indexOf(line, '='))}">
                                                                <input id="ftp<%=ftp++%>" class="text-danger border-0 w-100" type="text" name="line1" value='${fn:substring(line, fn:indexOf(line, '=')+1, fn:length(line))}'>
                                                            </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="d-flex align-items-center border-bottom">
                                                            <label class="me-3" for="ftp<%=ftp%>" >${fn:substring(line, 0, fn:indexOf(line, '='))}</label>
                                                            <input type="hidden" name="line0" value="${fn:substring(line, 0, fn:indexOf(line, '='))}">
                                                            <input id="ftp<%=ftp++%>" class="border-0 w-100" type="text" name="line1" value='${fn:substring(line, fn:indexOf(line, '=')+1, fn:length(line))}'>
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <%ftp++;%>
                                        </c:forEach>
                                    </div>
                                    <div class="d-flex border-bottom">
                                        <input class="w-100 border-0" type="text" name="line0" placeholder="Tên tham số cấu hình">
                                        <input class="w-100 border-0" type="text" name="line1" placeholder="Giá trị của tham số">
                                    </div>

                                    <div class="btn-group-sm mt-1">
                                        <button class="btn btn-success me-2" type="submit" name="control" value="edit">Update</button>
                                        <button class="btn btn-info me-2" type="submit" name="control" value="see">Show</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                    <div class="col-5 me-auto">
                        <div class="card border-0 shadow-sm mb-5">
                            <div class="card-header text-center pt-2">
                                <h5>Điều khiển dich vụ FTP</h5>
                            </div>
                            <div class="card-body">
                                <form action="configFTP" method="post">
                                    <div class="btn-group d-flex justify-content-sm-around">
                                        <button class="btn btn-success me-2" type="submit" name="control" value="start">Start FTP</button>
                                        <button class="btn btn-warning me-2" type="submit" name="control" value="restart">Restart FTP</button>
                                        <button class="btn btn-info me-2" type="submit" name="control" value="status">Status FTP</button>
                                        <button class="btn btn-danger me-2" type="submit" name="control" value="stop">Stop FTP</button>
                                    </div>
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                </form>
                            </div>
                        </div>

                        <div class="card border-0 shadow-sm mb-3">
                            <div class="card-header text-center">
                                <h5>UpLoad/Download file</h5>
                            </div>
                            <div class="card-body">
                                <form method="post" action="configFTP" enctype="multipart/form-data" class="mb-4">
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">

                                    <label for="fileInput" class="form-label">Chọn file để tải lên:</label>
                                    <div class="input-group mb-3">
                                        <input type="file" id="fileInput" name="file" class="form-control" required>
                                        <button type="submit" name="control" value="upfile" class="btn btn-success">Upload File</button>
                                    </div>


                                </form>

                                <form method="post" action="configFTP">
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">

                                    <label class="form-label">Đường dẫn tải xuống:</label>
                                    <div class="input-group mb-3">
                                        <span class="input-group-text bg-light">/home/<%=u.getUser()%>/</span>
                                        <input type="text" name="remoteFile" class="form-control" placeholder="Nhập tên file" required>
                                        <button type="submit" name="control" value="downfile" class="btn btn-info">Down File</button>
                                    </div>
                                    <div class="text-center text-muted">
                                        <small>Lưu ý: File download sẽ nằm ở "C:\Users\acer\Downloads"</small>
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
        <%
            }
        %>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>
</html>
