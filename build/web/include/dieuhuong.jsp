<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Dieu huong</title>
        <script src="https://kit.fontawesome.com/2cbc3b080b.js" crossorigin="anonymous"></script>
        <style>
            .dieuhuong {
                display: flex;
                justify-content: space-between;
                align-items: center;
                background-color: #333;
                padding: 10px 20px;
                color: white;
                border-bottom: 2px solid #444;
                font-weight: bold;
            }

            .dieuhuong-left {
                display: flex;
            }

            .hello {
                font-size: 16px;
            }

            .dieuhuong-right {
                font-size: 16px;
            }

            .nav-btn {
                background: none;
                color: #fff;
                padding: 8px 15px;
                margin-left: 10px;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                font-size: 16px;
                transition: background-color 0.3s ease;
            }
        </style>
    </head>
    <body>
        <div class="dieuhuong">
            <div class="dieuhuong-left">
                <form action="<%=request.getContextPath()%>/nav" method="post">
                    <button class="nav-btn" type="submit" name="nav" value="mainmachine"><i class="fa-solid fa-house-user"></i></button>
                    <button class="nav-btn" type="submit" name="nav" value="listuser">Quản lý người dùng</button>
                    <button class="nav-btn" type="submit" name="nav" value="listService">Danh sách dịch vụ</button>
                    <button class="nav-btn" type="submit" name="nav" value="ssh">SSH</button>
                    <button class="nav-btn" type="submit" name="nav" value="ftp">FTP</button>
                    <button class="nav-btn" type="submit" name="nav" value="http">HTTP</button>
                    <input type="hidden" name="host" value="${user.host}">
                    <input type="hidden" name="port" value="${user.port}">
                    <input type="hidden" name="user" value="${user.user}">
                    <input type="hidden" name="password" value="${user.password}">
                </form>
            </div>
            <div class="dieuhuong-right">
                <span class="hello">Hi, <%=u.getUser()%> | <%=u.getHost()%></span>
            </div>
        </div>
    </body>
</html>
