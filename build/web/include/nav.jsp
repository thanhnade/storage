<%@page language="java" contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Dieu huong</title>
        <script src="https://kit.fontawesome.com/2cbc3b080b.js" crossorigin="anonymous"></script>
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css"/>
        <style>
            .dieuhuong{
                /*                position: fixed;
                                top: 0;*/
                background: linear-gradient(#ee4d2d,#ff7337);
                color: white;
                /*display: flex;*/
                align-items: center;
                width: 100%;
                border-radius: 5px 0 0 0;
                /*justify-content: space-between;*/
                padding: 10px 20px;
                /*margin-bottom: 10px;*/
                /*font-weight: bold;*/
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
                font-weight: 400;
                transition: background-color 0.3s ease;
            }
            .nav-btn:hover {
                background-color: #F46A36;  /* Màu nền khi hover */
            }

            .nav-btn.active {
                background-color: #F46A36;
                color: white;
            }

            .dropdown-item-form {
                padding: 0;
                margin: 0;
            }

            .dropdown-item-form .dropdown-item {
                width: 100%;
                text-align: left;
                background: none;
                border: none;
                padding: .25rem 1rem;
                color: rgba(255,255,255,.75);
            }

            .dropdown-item-form .dropdown-item:hover {
                background-color: rgba(255,255,255,.1);
            }

            .dropdown-item i {
                display: inline-block;
                width: 20px;
            }

        </style>
    </head>
    <body>
        <div class="dieuhuong fixed-top">
            <div class="row d-flex">
                <div class="col-4 ps-5 d-flex align-items-center">
                    <a class="text-decoration-none text-white d-flex align-items-center" href="<%=request.getContextPath()%>/connects?choose=trangchu">
                        <!--<img class="me-1" src="<%=request.getContextPath()%>/img/logo1.png" width="30px" height="30px" alt="TRANG CHỦ"/>-->
                        TRANG CHỦ
                    </a>
                </div>
                <div class="col-6 d-flex align-items-center justify-content-around">
                    <div class="d-flex align-items-center">
                        <form class="d-flex" action="<%=request.getContextPath()%>/MachineManager" method="post">
                            <input type="hidden" name="host" value="${user.host}">
                            <input type="hidden" name="port" value="${user.port}">
                            <input type="hidden" name="user" value="${user.user}">
                            <input type="hidden" name="password" value="${user.password}">

                            <!-- Nút Home -->
                            <button class="nav-btn ${'mainmachine' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="mainmachine">
                                <i class="fa-solid fa-house-laptop"></i> My Computer
                            </button>

                            <!-- Nút Quản lý người dùng -->
                            <button class="nav-btn ${'listuser' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="listuser">Quản Lý Người Dùng</button>

                            <!-- Nút SSH -->
                            <button class="nav-btn ${'ssh' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="ssh">SSH</button>

                            <!-- Nút FTP -->
                            <button class="nav-btn ${'ftp' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="ftp">FTP</button>

                            <!-- Nút HTTP -->
                            <button class="nav-btn ${'http' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="http">HTTP</button>

                            <!-- Nút NFS -->
                            <button class="nav-btn ${'nfs' == sessionScope.activeNav ? 'active' : ''}" type="submit" name="nav" value="nfs">NFS</button>

                        </form>
                    </div>
                </div>
                <div class="col-2 d-flex align-items-center justify-content-center">
                    <!--dropdown user-->
                    <div class="btn-group">
                        <button class="btn btn-sm dropdown-toggle text-white" style="background-color: #F46A36" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="fa-regular fa-user me-2"></i><span>Hi, <%=u.getUser()%></span>
                        </button>
                        <ul class="dropdown-menu">
                            <li class="dropdown-item">
                                <span>IP: <%=u.getHost()%></span>
                            </li>
                            <li class="dropdown-item">
                                <span>Port: <%=u.getPort()%></span>
                            </li>
                            <li class="ps-3 pt-1">
                                <a class="text-black" href="<%=request.getContextPath()%>/logout?host=${user.host}&user=${user.user}&port=${user.port}" 
                                   onclick="return confirm('Bạn có chắc chắn muốn đăng xuất?');">Logout <i class="fa-solid fa-arrow-right-from-bracket"></i></a>
                            </li>
                            <li class="ps-3 pt-1">
                                <a class="text-black" href="<%=request.getContextPath()%>/shutdown?host=${user.host}&port=${user.port}&user=${user.user}&password=${user.password}" 
                                   onclick="return confirm('Bạn chắc chắn muốn tắt máy?');">Shut Down <i class="fa-solid fa-power-off"></i></a>

                            </li>

                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
