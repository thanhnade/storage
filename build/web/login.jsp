<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đăng nhập - Đăng ký</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png" />
        <link rel="stylesheet" href="css/connect.css" />
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css" />
        <style>
            body {
                background: #ee4d2d;
                background: linear-gradient(#ee4d2d, #ff7337);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .gradient-custom-2 {
                background: linear-gradient(to right, #1a2980, #26d0ce);
            }

            .card {
                border-radius: 1rem;
                box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
                width: 100%;
                max-width: 40%;
                margin: 0 auto;
            }

            .form-control {
                border-radius: 0.5rem;
                padding: 0.75rem 1rem;
            }

            .btn {
                border-radius: 0.5rem;
                padding: 0.5rem 1rem;
                font-weight: 500;
            }

            .btn-primary,
            .btn-success {
                background: #fb5533;
                background: linear-gradient(#ee4d2d, #ff7337);
                border-color: #fb5533;
            }

            .btn-primary:hover,
            .btn-success:hover {
                background: #ff7337;
                background: linear-gradient(#ff7337, #ee4d2d);
                border-color: #ff7337;
            }

            .register-link {
                color: #ee4d2d;
                text-decoration: none;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.3s ease;
            }

            .register-link:hover {
                color: #ff7337;
            }

            .login-section,
            .register-section {
                transition: all 0.5s ease;
                padding: 2rem;
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                width: 100%;
            }

            .hidden {
                display: none;
            }

            .logo-container {
                margin-bottom: 1.5rem;
            }

            .logo-container img {
                width: 100px;
                height: auto;
            }

            .form-title {
                margin-bottom: 1.5rem;
                color: #ee4d2d;
            }

            .form-container {
                max-width: 100%;
                margin: 0 auto;
            }

            .alert {
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 1000;
                min-width: 300px;
            }

            .text-muted {
                color: #6c757d !important;
            }

            .card-container {
                position: relative;
                min-height: 600px;
            }

            .form-outline {
                margin-bottom: 1rem;
            }
        </style>
    </head>

    <body>
        <c:choose>
            <c:when test="${nguoi_dung == null}">
            </c:when>
            <c:otherwise>
                <% //response.sendRedirect("./StorageServlet");%>
            </c:otherwise>
        </c:choose>

        <div class="container">
            <div class="row justify-content-center">
                <div class="col-12">
                    <div class="card">
                        <div class="card-container">
                            <!-- Phần đăng nhập -->
                            <div class="login-section">
                                <div class="form-container">
                                    <div class="text-center logo-container">
                                        <a href="login.jsp"><img src="img/logo1.png" alt="logo" /></a>
                                        <h4 class="form-title">Đăng nhập hệ thống</h4>
                                    </div>

                                    <form action="loginServlet" method="post">
                                        <p class="text-muted mb-3">Vui lòng đăng nhập để tiếp tục</p>
                                        <div class="form-outline">
                                            <label class="form-label">Tài khoản</label>
                                            <input type="text" name="username" class="form-control"
                                                   placeholder="Nhập tài khoản" required />
                                        </div>

                                        <div class="form-outline">
                                            <label class="form-label">Mật khẩu</label>
                                            <input type="password" name="password" class="form-control"
                                                   placeholder="Nhập mật khẩu" required />
                                        </div>

                                        <div class="text-center pt-1 mb-3">
                                            <button class="btn btn-sm btn-primary btn-block fa-lg mb-1 me-2"
                                                    type="submit">Đăng nhập</button>
                                            <button class="btn btn-sm btn-secondary btn-block fa-lg mb-1"
                                                    type="reset">Hủy</button>
                                        </div>

                                        <div class="text-center">
                                            <p class="mb-0">Chưa có tài khoản?
                                                <span class="register-link" onclick="toggleForms()">Đăng ký
                                                    ngay</span>
                                            </p>
                                        </div>
                                    </form>
                                </div>
                            </div>

                            <!-- Phần đăng ký -->
                            <div class="register-section hidden">
                                <div class="form-container">
                                    <div class="text-center logo-container">
                                        <a href="login.jsp"><img src="img/logo1.png" alt="logo" /></a>
                                        <h4 class="form-title">Đăng ký tài khoản mới</h4>
                                    </div>

                                    <form action="registerServlet" method="post">
                                        <div class="form-outline">
                                            <label class="form-label">Tài khoản</label>
                                            <input type="text" name="username" class="form-control"
                                                   placeholder="Nhập tài khoản mới" required />
                                        </div>

                                        <div class="form-outline">
                                            <label class="form-label">Mật khẩu</label>
                                            <input type="password" name="password" class="form-control"
                                                   placeholder="Nhập mật khẩu" required />
                                        </div>

                                        <div class="form-outline">
                                            <label class="form-label">Xác nhận mật khẩu</label>
                                            <input type="password" name="confirm_password" class="form-control"
                                                   placeholder="Nhập lại mật khẩu" required />
                                        </div>

                                        <div class="text-center pt-1 mb-3">
                                            <button class="btn btn-success btn-block fa-lg mb-1 me-2" type="submit">Đăng
                                                ký</button>
                                            <button class="btn btn-secondary btn-block fa-lg mb-1" type="reset">Hủy</button>
                                        </div>

                                        <div class="text-center">
                                            <p class="mb-0">Đã có tài khoản?
                                                <span class="register-link" onclick="toggleForms()">Đăng nhập
                                                    ngay</span>
                                            </p>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Thông báo -->
        <div>
            <c:if test="${not empty error}">
                <div id="message" class="alert alert-danger alert-dismissible">
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    <strong>Thất bại! </strong> ${error}
                </div>
            </c:if>
            <c:if test="${not empty success}">
                <div id="message" class="alert alert-success alert-dismissible">
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    <strong>Thành công! </strong> ${success}
                </div>
            </c:if>
        </div>

        <script>
            function toggleForms() {
                const loginSection = document.querySelector('.login-section');
                const registerSection = document.querySelector('.register-section');
                if (loginSection.classList.contains('hidden')) {
                    loginSection.classList.remove('hidden');
                    registerSection.classList.add('hidden');
                } else {
                    loginSection.classList.add('hidden');
                    registerSection.classList.remove('hidden');
                }
            }
            setTimeout(function () {
                const message = document.getElementById("message");
                if (message) {
                    message.style.display = "none";
                }
            }, 10000);
        </script>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    </body>

</html>