<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản lý máy chủ</title>
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
                    <h4 class="text-center">QUẢN LÝ MÁY CHỦ</h4>
                </div>
                <!--quan ly may chu-->
                <div id="qlmc" class="row bg-white mb-3 p-0 border-top" >
                    <!--terminal-->
                    <div class="col-6 p-3">
                        <div class="terminal">
                            <div class="terminal-header">
                                <div class="dot red"></div>
                                <div class="dot yellow"></div>
                                <div class="dot green"></div>
                            </div>
                            <div class="terminal-body">
                                <span class="d-inline-flex align-items-center">
                                    ${user.user}@ubuntu:~$  
                                    <form action="<%=request.getContextPath()%>/terminal" method="post">
                                        <span><input class="text-white bg-transparent border-0 ms-3" type="text" name="str" placeholder="Nhap lenh o day"></span>
                                        <span><button class="btn btn-sm text-white" type="submit"><i class="fa-regular fa-paper-plane"></i></button></span>
                                        <input type="hidden" name="host" value="${user.host}">
                                        <input type="hidden" name="port" value="${user.port}">
                                        <input type="hidden" name="user" value="${user.user}">
                                        <input type="hidden" name="password" value="${user.password}">
                                    </form>
                                </span>
                                <c:forEach var="line" items="${outputLines}">
                                    <p class="text-white">${line}</p>
                                </c:forEach>
                                <p>${exitStatus}</p>

                            </div>
                        </div>
                    </div>  
                    <!--listService-->
                    <div class="col-6 p-3"> 
                        <div class="terminal-header d-flex justify-content-evenly">                        
                            Danh sách dịch vụ 
                            <div>
                                <form action="listService" method="post">
                                    <button class="btn btn-sm btn-secondary me-2" type="submit" name="action" value="show">Show</button>
                                    <button class="btn btn-sm btn-secondary" type="submit" name="action" value="reset">Reset</button>
                                    <input type="hidden" name="host" value="<%=u.getHost()%>">
                                    <input type="hidden" name="port" value="<%=u.getPort()%>">
                                    <input type="hidden" name="user" value="<%=u.getUser()%>">
                                    <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                </form> 
                            </div>
                        </div>
                        <div class="table-container">
                            <table class="table table-striped" border="1">
                                <tr>
                                    <th>STT</th>
                                    <th>Tên dịch vụ</th> 
                                    <th>Trạng thái</th>
                                    <th colspan="3"></th>
                                </tr>
                                <% int i = 1;%>
                                <c:forEach var="service" items="${services}">
                                    <tr>
                                    <form action="listService" method="post">
                                        <td><%=i%></td>
                                        <td>${service[1]} <input type="hidden" name="servicename" value="${service[1]}"></td> 
                                        <td>${service[0]}</td>
                                        <td><button type="submit" name="action" value="start">Start</button></td>
                                        <td><button type="submit" name="action" value="stop">Stop</button></td>
                                        <td><button id="btn-see" class="text-decoration-underline text-black" type="submit" name="action" value="seeConfig"><i>File cấu hình</i></button></td>
                                        <input type="hidden" name="host" value="<%=u.getHost()%>">
                                        <input type="hidden" name="port" value="<%=u.getPort()%>">
                                        <input type="hidden" name="user" value="<%=u.getUser()%>">
                                        <input type="hidden" name="password" value="<%=u.getPassword()%>">
                                    </form>
                                    </tr>
                                    <% i++;%>
                                </c:forEach>
                                <tr>
                                    <c:if test = "${services == null}">
                                        <td colspan="4"><c:out value = "Không có dữ liệu để hiển thị."/></td>
                                    </c:if>
                                </tr>
                            </table>
                        </div>

                        <!--file cau hinh dich vu--> 
                        <div id="seeConfig" class="toast 
                             <c:if test="${not empty requestScope.seeConfig}">
                                 show
                             </c:if> 
                             align-items-center text-bg-secondary border-0" role="alert" aria-live="assertive" aria-atomic="true">

                            <div class="toast-header bg-black text-white">
                                <h6>File cấu hình (unit)</h6>
                                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                            </div>
                            <div class="config-line toast-body w-100">
                                <c:forEach var="line" items="${seeConfig}">
                                    <p>${line}</p>
                                </c:forEach>
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
        </div>
    </div>
    <%}%>
    <script>
        // Toggle mobile navigation
        document.querySelector('.mobile-nav-toggle').addEventListener('click', function () {
            document.getElementById('navmenu').classList.toggle('active');
        });

        // Smooth scrolling
        document.querySelectorAll('.navmenu a').forEach(anchor => {
            anchor.addEventListener('click', function (e) {
                e.preventDefault();
                const targetId = this.getAttribute('href').substring(1);
                document.getElementById(targetId).scrollIntoView({
                    behavior: 'smooth'
                });
            });
        });
    </script>

    <script>
        //Get the button
        let mybutton = document.getElementById("btn-back-to-top");

// When the user scrolls down 20px from the top of the document, show the button
        window.onscroll = function () {
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
    <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    
</body>
</html>
