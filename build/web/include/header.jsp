<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Header</title>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>

        <style>
            /*header*/
            body {
                font-family: Arial, sans-serif;
            }
            .navbar {
                background: linear-gradient(#ee4d2d,#ff7337);
                color: white;
                display: flex;
                align-items: center;
                width: 100%;
                border-radius: 5px 0 0 0;
            }
            .navbar .logo {
                font-size: 24px;
                font-weight: bold;
                margin-right: auto;
                margin-left: 50px;
            }
            .navbar .logo a {
                text-decoration: none;
                color: white;
            }

            /* Danh sách trong thanh điều hướng */
            .menu {
                list-style-type: none;
                display: flex; /* Xếp hàng ngang */
                justify-content: center; /* Chia đều khoảng cách giữa các mục */
            }

            /* Các liên kết */
            .menu li a {
                align-content: center;
                color: white; /* Màu chữ trắng */
                text-decoration: none; /* Loại bỏ gạch chân */
                padding: 0px 20px;
                display: block;
            }


        </style>
    </head>
    <body>
        <div class="navbar fixed-top">
            <div class="logo">
                <a href="./trangchu.jsp">
                    TRANG CHỦ
                </a>
            </div>
            <div>
                <ul class="menu mt-2 mb-2">
                    <li><a href="./StorageControl">Quản lý người dùng</a></li>
                    <li><a href="#">Giới thiệu</a></li>
                    <!--<li><a href="#">Hỗ trợ</a></li>-->
                    <li><a href="https://zalo.me/0794220424">Liên hệ</a></li>
                    <li>
                        <a
                           onclick="return confirm('Bạn có chắc chắn muốn đăng xuất không?')"
                           href="StorageServlet?action=logout">
                            <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
                        </a>
                    </li>
<!--                    <li>
                        <form action="logout" method="get">
                            <button class="btn btn-link d-block text-white text-decoration-none p-0 px-3 text-center" type="submit" name="action" value="logoutAll" onclick="return confirm('Bạn có chắc chắn muốn đăng xuất tất cả không?');">
                                Đăng xuất tất cả
                            </button>
                        </form>
                    </li>-->
                </ul>
            </div>
        </div>
    </body>
</html>
