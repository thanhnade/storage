<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@page import="Model.InforUser,Model.NguoiDung, java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Quản lý người dùng</title>
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

        .storage-card {
            background: white;
            border-radius: 10px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }

        .progress {
            height: 8px;
            border-radius: 4px;
        }

        .user-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: #f0f0f0;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 10px;
        }

        .navbar {
            background: linear-gradient(#ee4d2d, #ff7337);
        }

        .btn-primary {
            background: linear-gradient(#ee4d2d, #ff7337);
            border: none;
        }

        .btn-primary:hover {
            background: #F46A36;
        }

        .btn-outline-primary {
            color: #ee4d2d;
            border-color: #ee4d2d;
        }

        .table th {
            background-color: #f8f9fa;
        }

        .storage-summary {
            border-left: 4px solid #ee4d2d;
            padding-left: 15px;
        }

        .user-actions {
            white-space: nowrap;
        }

        .user-status {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            display: inline-block;
            margin-right: 5px;
        }

        .status-active {
            background-color: #28a745;
        }

        .status-inactive {
            background-color: #dc3545;
        }

        .filter-section {
            background-color: #f8f9fa;
            border-radius: 10px;
            padding: 15px;
            margin-bottom: 20px;
        }
    </style>
</head>

<%
        HttpSession ss = request.getSession();
        NguoiDung nguoiDung = (NguoiDung) ss.getAttribute("nguoi_dung");
        List<InforUser> users = (List<InforUser>) ss.getAttribute("users");

        if (ss == null || nguoiDung == null) {
            request.setAttribute("error", "Vui lòng đăng nhập!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        if (users == null || users.isEmpty()) {
            request.setAttribute("error", "Vui lòng kết nối đến máy chủ trước!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Kiểm tra quyền admin
        if (!"admin".equals(nguoiDung.getRole())) {
            request.setAttribute("error", "Bạn không có quyền truy cập trang này!");
            request.getRequestDispatcher("trangchu.jsp").forward(request, response);
            return;
        }
    %>

<body style="background-color: #f5f5f5;">
    <%@include file="include/header.jsp" %>
    <%@include file="include/background.jsp" %>

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
            <% response.sendRedirect("./login.jsp");%>
        </c:otherwise>
    </c:choose>

    <!-- Database Connection and Query -->
    <sql:setDataSource var="dataSource" driver="com.mysql.jdbc.Driver" url="jdbc:mysql://localhost:3306/storage"
        user="root" password="" />

    <sql:query dataSource="${dataSource}" var="userList">
        SELECT * FROM nguoi_dung ORDER BY created_at DESC;
    </sql:query>

    <!-- Toast Container -->
    <div class="toast-container position-fixed top-0 end-0 p-3">
        <c:if test="${error != null}">
            <div class="toast align-items-center text-white bg-danger border-0" role="alert" aria-live="assertive"
                aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas fa-exclamation-circle me-2"></i>
                        <c:out value="${error}" />
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
                </div>
            </div>
        </c:if>
        <c:if test="${success != null}">
            <div class="toast align-items-center text-white bg-success border-0" role="alert" aria-live="assertive"
                aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas fa-check-circle me-2"></i>
                        <c:out value="${success}" />
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
                </div>
            </div>
        </c:if>
    </div>

    <div class="container">
        <div class="row mb-4" style="margin-top: 5%">
            <div class="col-12">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h2 class="mb-0">Quản lý người dùng</h2>
                </div>
            </div>
        </div>

        <!-- Dashboard Stats -->
        <div class="row mb-4">
            <div class="col-md-3 mb-3">
                <div class="card h-100 border-0 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <h6 class="text-muted mb-1">Tổng người dùng</h6>
                                <h3 class="mb-0">${userList.rowCount}</h3>
                            </div>
                            <div class="rounded-circle bg-light p-3">
                                <i class="fas fa-users fa-2x text-primary"></i>
                            </div>
                        </div>
                        <p class="card-text text-muted mt-3">
                            <i class="fas fa-arrow-up text-success me-1"></i>
                            <span class="text-success">${newUsers}</span> người dùng mới trong 7 ngày qua
                        </p>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card h-100 border-0 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <h6 class="text-muted mb-1">Máy chủ đang quản lý</h6>
                                <h3 class="mb-0">
                                    <c:choose>
                                        <c:when test="${not empty users}">
                                            ${fn:length(users)}
                                        </c:when>
                                        <c:otherwise>
                                            0
                                        </c:otherwise>
                                    </c:choose>
                                </h3>
                            </div>
                            <div class="rounded-circle bg-light p-3">
                                <i class="fas fa-server fa-2x text-success"></i>
                            </div>
                        </div>
                        <p class="card-text text-muted mt-3">
                            <c:choose>
                                <c:when test="${not empty users}">
                                    <i class="fas fa-check-circle text-success me-1"></i>
                                    ${fn:length(users)} máy chủ đang hoạt động
                                </c:when>
                                <c:otherwise>
                                    <i class="fas fa-exclamation-circle text-warning me-1"></i>
                                    Chưa có máy chủ nào
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card h-100 border-0 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <h6 class="text-muted mb-1">Dung lượng đã sử dụng</h6>
                                <h3 class="mb-0">
                                    <c:choose>
                                        <c:when test="${storageStats.totalUsed != null && storageStats.totalUsed > 0}">
                                            <c:choose>
                                                <c:when test="${storageStats.totalUsed >= 1099511627776}">
                                                    <fmt:formatNumber value="${storageStats.totalUsed / 1099511627776}"
                                                        pattern="#,##0.00" /> TB
                                                </c:when>
                                                <c:when test="${storageStats.totalUsed >= 1073741824}">
                                                    <fmt:formatNumber value="${storageStats.totalUsed / 1073741824}"
                                                        pattern="#,##0.00" /> GB
                                                </c:when>
                                                <c:when test="${storageStats.totalUsed >= 1048576}">
                                                    <fmt:formatNumber value="${storageStats.totalUsed / 1048576}"
                                                        pattern="#,##0.00" /> MB
                                                </c:when>
                                                <c:otherwise>
                                                    <fmt:formatNumber value="${storageStats.totalUsed / 1024}"
                                                        pattern="#,##0.00" /> KB
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            0.00 MB
                                        </c:otherwise>
                                    </c:choose>
                                </h3>
                            </div>
                            <div class="rounded-circle bg-light p-3">
                                <i
                                    class="fas fa-hdd fa-2x ${storageStats.usagePercent > 90 ? 'text-danger' : storageStats.usagePercent > 70 ? 'text-warning' : 'text-success'}"></i>
                            </div>
                        </div>
                        <div class="progress mt-3" style="height: 6px;">
                            <c:set var="usagePercent"
                                value="${storageStats.totalCapacity > 0 ? (storageStats.totalUsed / storageStats.totalCapacity) * 100 : 0}" />
                            <div class="progress-bar ${usagePercent > 90 ? 'bg-danger' : usagePercent > 70 ? 'bg-warning' : 'bg-success'}"
                                role="progressbar" style="width: ${usagePercent}%" aria-valuenow="${usagePercent}"
                                aria-valuemin="0" aria-valuemax="100">
                            </div>
                        </div>
                        <p class="card-text text-muted mt-2">
                            <c:choose>
                                <c:when test="${storageStats.totalCapacity > 0}">
                                    <fmt:formatNumber value="${usagePercent}" pattern="#,##0.00" />% của
                                    <c:choose>
                                        <c:when test="${storageStats.totalCapacity >= 1099511627776}">
                                            <fmt:formatNumber value="${storageStats.totalCapacity / 1099511627776}"
                                                pattern="#,##0.00" /> TB
                                        </c:when>
                                        <c:when test="${storageStats.totalCapacity >= 1073741824}">
                                            <fmt:formatNumber value="${storageStats.totalCapacity / 1073741824}"
                                                pattern="#,##0.00" /> GB
                                        </c:when>
                                        <c:otherwise>
                                            <fmt:formatNumber value="${storageStats.totalCapacity / 1048576}"
                                                pattern="#,##0.00" /> MB
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    0.00% của 0.00 MB (Chưa thiết lập dung lượng)
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card h-100 border-0 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center">
                            <div>
                                <h6 class="text-muted mb-1">Tổng tệp tin và thư mục</h6>
                                <h3 class="mb-0">
                                    <c:choose>
                                        <c:when test="${fileStats != null}">
                                            <fmt:formatNumber value="${fileStats.totalFiles + fileStats.totalFolders}"
                                                pattern="#,##0" />
                                        </c:when>
                                        <c:otherwise>
                                            0
                                        </c:otherwise>
                                    </c:choose>
                                </h3>
                            </div>
                            <div class="rounded-circle bg-light p-3">
                                <i class="fas fa-file fa-2x text-info"></i>
                            </div>
                        </div>
                        <p class="card-text text-muted mt-3">
                            <c:choose>
                                <c:when test="${fileStats != null}">
                                    <span class="me-2">
                                        <i class="fas fa-folder text-warning me-1"></i>
                                        <fmt:formatNumber value="${fileStats.totalFolders}" pattern="#,##0" /> thư mục
                                    </span>
                                    <span>
                                        <i class="fas fa-file-alt text-info me-1"></i>
                                        <fmt:formatNumber value="${fileStats.totalFiles}" pattern="#,##0" /> tệp
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <i class="fas fa-info-circle text-muted me-1"></i>
                                    Chưa có dữ liệu
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>
            </div>
        </div>

        <!-- Filter Section -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card border-0 shadow-sm">
                    <div class="card-body filter-section">
                        <div class="row">
                            <div class="col-md-4">
                                <div class="input-group">
                                    <!--                                        <span class="input-group-text"><i class="fas fa-search"></i></span>-->
                                    <input type="text" id="userSearch" class="form-control"
                                        placeholder="Tìm kiếm người dùng...">
                                </div>
                            </div>
                            <div class="col-md-3">
                                <select id="roleFilter" class="form-select">
                                    <option value="">Tất cả vai trò</option>
                                    <option value="admin">Admin</option>
                                    <option value="client">Client</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <select id="sortUsers" class="form-select">
                                    <option value="newest">Mới nhất trước</option>
                                    <option value="oldest">Cũ nhất trước</option>
                                    <option value="username">Theo tên tài khoản</option>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-primary w-100" onclick="refreshStats()">
                                    <i class="fas fa-sync-alt me-1"></i> Làm mới
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- User Management -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">Danh sách người dùng</h5>
                        <div>
                            <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal"
                                data-bs-target="#addUserModal">
                                <i class="fas fa-user-plus me-1"></i> Thêm người dùng mới
                            </button>
                            <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal"
                                data-bs-target="#bulkUpdateModal">
                                <i class="fas fa-edit me-1"></i> Cập nhật hàng loạt
                            </button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover" id="userTable">
                                <thead>
                                    <tr>
                                        <th scope="col" width="40px">
                                            <div class="form-check">
                                                <input class="form-check-input" type="checkbox" id="selectAllUsers">
                                            </div>
                                        </th>
                                        <th scope="col" width="50px">#</th>
                                        <th scope="col">Tài khoản</th>
                                        <th scope="col">Vai trò</th>
                                        <th scope="col">Thư mục lưu trữ</th>
                                        <th scope="col">Dung lượng đã dùng</th>
                                        <th scope="col">Dung lượng giới hạn</th>
                                        <th scope="col">Tỷ lệ sử dụng</th>
                                        <th scope="col">Ngày tạo</th>
                                        <th scope="col" class="text-center">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${userList.rows}" var="user" varStatus="status">
                                        <tr>
                                            <td>
                                                <div class="form-check">
                                                    <input class="form-check-input user-select" type="checkbox"
                                                        name="selectedUsers" value="${user.tai_khoan}">
                                                </div>
                                            </td>
                                            <th scope="row">${status.index + 1}</th>
                                            <td>
                                                <div class="d-flex align-items-center">
                                                    <div class="user-avatar">
                                                        <i class="fas fa-user"></i>
                                                    </div>
                                                    <div>
                                                        ${user.tai_khoan}
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <span
                                                    class="badge ${user.role == 'admin' ? 'bg-danger' : 'bg-primary'}">
                                                    ${user.role}
                                                </span>
                                            </td>
                                            <td>
                                                <small class="text-muted">${user.thu_muc != null ? user.thu_muc : 'Chưa
                                                    có thư mục'}</small>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${userStorageMap[user.tai_khoan] != null}">
                                                        <fmt:formatNumber
                                                            value="${userStorageMap[user.tai_khoan] / 1048576}"
                                                            pattern="#,##0.00" /> MB
                                                    </c:when>
                                                    <c:otherwise>
                                                        0.00 MB
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${user.storage_limit > 0}">
                                                        <fmt:formatNumber value="${user.storage_limit / 1048576}"
                                                            pattern="#,##0.00" /> MB
                                                    </c:when>
                                                    <c:otherwise>
                                                        Không giới hạn
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:set var="userUsagePercent"
                                                    value="${user.storage_limit > 0 ? (userStorageMap[user.tai_khoan] / user.storage_limit) * 100 : 0}" />
                                                <div class="progress" style="height: 6px;">
                                                    <div class="progress-bar ${userUsagePercent > 90 ? 'bg-danger' : userUsagePercent > 70 ? 'bg-warning' : 'bg-success'}"
                                                        role="progressbar" style="width: ${userUsagePercent}%"
                                                        aria-valuenow="${userUsagePercent}" aria-valuemin="0"
                                                        aria-valuemax="100">
                                                    </div>
                                                </div>
                                                <small class="text-muted">
                                                    <c:choose>
                                                        <c:when test="${user.storage_limit > 0}">
                                                            <fmt:formatNumber value="${userUsagePercent}"
                                                                pattern="#,##0.00" />%
                                                        </c:when>
                                                        <c:otherwise>
                                                            0.00%
                                                        </c:otherwise>
                                                    </c:choose>
                                                </small>
                                            </td>
                                            <td>
                                                <fmt:formatDate value="${user.created_at}" pattern="dd/MM/yyyy HH:mm" />
                                            </td>
                                            <td class="text-center user-actions">
                                                <button class="btn btn-sm btn-outline-primary"
                                                    onclick="editUser('${user.tai_khoan}', '${user.role}', '${user.storage_limit}')">
                                                    <i class="fas fa-edit"></i>
                                                </button>
                                                <button class="btn btn-sm btn-outline-warning"
                                                    onclick="resetPassword('${user.tai_khoan}')">
                                                    <i class="fas fa-key"></i>
                                                </button>
                                                <button class="btn btn-sm btn-outline-danger"
                                                    onclick="deleteUser('${user.tai_khoan}')">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Server List -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">Danh sách máy chủ</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th scope="col" width="50px">STT</th>
                                        <th scope="col">Tên máy chủ</th>
                                        <th scope="col">Địa chỉ IP</th>
                                        <!--<th scope="col">Port</th>-->
                                        <th scope="col">Tổng dung lượng</th>
                                        <th scope="col">Đã sử dụng</th>
                                        <th scope="col">Còn trống</th>
                                        <th scope="col">Tỷ lệ sử dụng</th>
                                        <th scope="col">Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${users}" var="server" varStatus="status">
                                        <tr>
                                            <th scope="row">${status.index + 1}</th>
                                            <td>
                                                <div class="d-flex align-items-center">
                                                    <div class="rounded-circle bg-light p-2 me-2">
                                                        <i class="fas fa-server text-primary"></i>
                                                    </div>
                                                    <div>
                                                        ${server.user}
                                                    </div>
                                                </div>
                                            </td>
                                            <td>${server.host}</td>
                                            <!--<td>${server.port}</td>-->
                                            <td>
                                                <c:choose>
                                                    <c:when
                                                        test="${serverStats[server.host].totalStorage >= 1099511627776}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].totalStorage / 1099511627776}"
                                                            pattern="#,##0.00" /> TB
                                                    </c:when>
                                                    <c:when
                                                        test="${serverStats[server.host].totalStorage >= 1073741824}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].totalStorage / 1073741824}"
                                                            pattern="#,##0.00" /> GB
                                                    </c:when>
                                                    <c:otherwise>
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].totalStorage / 1048576}"
                                                            pattern="#,##0.00" /> MB
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when
                                                        test="${serverStats[server.host].usedStorage >= 1099511627776}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].usedStorage / 1099511627776}"
                                                            pattern="#,##0.00" /> TB
                                                    </c:when>
                                                    <c:when
                                                        test="${serverStats[server.host].usedStorage >= 1073741824}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].usedStorage / 1073741824}"
                                                            pattern="#,##0.00" /> GB
                                                    </c:when>
                                                    <c:otherwise>
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].usedStorage / 1048576}"
                                                            pattern="#,##0.00" /> MB
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when
                                                        test="${serverStats[server.host].availableStorage >= 1099511627776}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].availableStorage / 1099511627776}"
                                                            pattern="#,##0.00" /> TB
                                                    </c:when>
                                                    <c:when
                                                        test="${serverStats[server.host].availableStorage >= 1073741824}">
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].availableStorage / 1073741824}"
                                                            pattern="#,##0.00" /> GB
                                                    </c:when>
                                                    <c:otherwise>
                                                        <fmt:formatNumber
                                                            value="${serverStats[server.host].availableStorage / 1048576}"
                                                            pattern="#,##0.00" /> MB
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:set var="usagePercent"
                                                    value="${(serverStats[server.host].usedStorage / serverStats[server.host].totalStorage) * 100}" />
                                                <div class="progress" style="height: 6px;">
                                                    <div class="progress-bar ${usagePercent > 90 ? 'bg-danger' : usagePercent > 70 ? 'bg-warning' : 'bg-success'}"
                                                        role="progressbar" style="width: ${usagePercent}%"
                                                        aria-valuenow="${usagePercent}" aria-valuemin="0"
                                                        aria-valuemax="100">
                                                    </div>
                                                </div>
                                                <small class="text-muted">
                                                    <fmt:formatNumber value="${usagePercent}" pattern="#,##0.00" />%
                                                </small>
                                            </td>
                                            <td>
                                                <span class="badge bg-success">
                                                    <i class="fas fa-check-circle me-1"></i> Hoạt động
                                                </span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- Add User Modal -->
    <div class="modal fade" id="addUserModal" tabindex="-1" aria-labelledby="addUserModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="addUserModalLabel">Thêm người dùng mới</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <form action="StorageControl" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="addUser">
                        <div class="mb-3">
                            <label for="username" class="form-label">Tên tài khoản</label>
                            <input type="text" class="form-control" id="username" name="username" required>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">Mật khẩu</label>
                            <input type="password" class="form-control" id="password" name="password" required>
                        </div>
                        <div class="mb-3">
                            <label for="role" class="form-label">Vai trò</label>
                            <select class="form-select" id="role" name="role" required>
                                <option value="client">Client</option>
                                <option value="admin">Admin</option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Thêm người dùng</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Edit User Modal -->
    <div class="modal fade" id="editUserModal" tabindex="-1" aria-labelledby="editUserModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="editUserModalLabel">Chỉnh sửa thông tin người dùng</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <form action="StorageControl" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="updateUser">
                        <input type="hidden" name="tai_khoan" id="editTaiKhoan">
                        <div class="mb-3">
                            <label for="editUsername" class="form-label">Tên tài khoản</label>
                            <input type="text" class="form-control" id="editUsername" readonly>
                        </div>
                        <div class="mb-3">
                            <label for="editRole" class="form-label">Vai trò</label>
                            <select class="form-select" id="editRole" name="role" required>
                                <option value="client">Client</option>
                                <option value="admin">Admin</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label for="editStorageLimit" class="form-label">Dung lượng giới hạn (MB)</label>
                            <input type="number" class="form-control" id="editStorageLimit" name="storageLimit" min="1"
                                max="5120" required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Lưu thay đổi</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Reset Password Modal -->
    <div class="modal fade" id="resetPasswordModal" tabindex="-1" aria-labelledby="resetPasswordModalLabel"
        aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-warning text-white">
                    <h5 class="modal-title" id="resetPasswordModalLabel">Đặt lại mật khẩu</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <form action="StorageControl" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="resetPassword">
                        <input type="hidden" name="tai_khoan" id="resetTaiKhoan">
                        <p>Đặt lại mật khẩu cho tài khoản: <strong id="resetUsername"></strong></p>
                        <div class="mb-3">
                            <label for="newPassword" class="form-label">Mật khẩu mới</label>
                            <input type="password" class="form-control" id="newPassword" name="newPassword" required>
                        </div>
                        <div class="mb-3">
                            <label for="confirmPassword" class="form-label">Xác nhận mật khẩu</label>
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                                required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-warning">Đặt lại mật khẩu</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Delete User Modal -->
    <div class="modal fade" id="deleteUserModal" tabindex="-1" aria-labelledby="deleteUserModalLabel"
        aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title" id="deleteUserModalLabel">Xóa người dùng</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <form action="StorageControl" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="deleteUser">
                        <input type="hidden" name="tai_khoan" id="deleteTaiKhoan">
                        <p>Bạn có chắc chắn muốn xóa tài khoản: <strong id="deleteUsername"></strong>?</p>
                        <div class="alert alert-warning">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            Cảnh báo: Hành động này không thể hoàn tác. Tất cả dữ liệu của người dùng này sẽ bị xóa vĩnh
                            viễn.
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-danger">Xóa người dùng</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Bulk Update Modal -->
    <div class="modal fade" id="bulkUpdateModal" tabindex="-1" aria-labelledby="bulkUpdateModalLabel"
        aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="bulkUpdateModalLabel">Cập nhật hàng loạt</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <form action="StorageControl" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="bulkUpdate">
                        <div class="mb-3">
                            <label class="form-label">Chọn hành động</label>
                            <select class="form-select" name="bulkAction" required>
                                <option value="">-- Chọn hành động --</option>
                                <option value="updateRole">Cập nhật vai trò</option>
                                <option value="deleteUsers">Xóa người dùng</option>
                            </select>
                        </div>
                        <div class="mb-3" id="roleSelectGroup">
                            <label class="form-label">Vai trò mới</label>
                            <select class="form-select" name="role">
                                <option value="client">Client</option>
                                <option value="admin">Admin</option>
                            </select>
                        </div>
                        <div class="alert alert-warning">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            Lưu ý: Hành động này sẽ áp dụng cho tất cả người dùng được chọn trong danh sách.
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Thực hiện</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- JavaScript -->
    <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Hiển thị toast nếu có
        document.addEventListener('DOMContentLoaded', function() {
            var toastElList = [].slice.call(document.querySelectorAll('.toast'));
            var toastList = toastElList.map(function(toastEl) {
                return new bootstrap.Toast(toastEl, {
                    delay: 5000
                }).show();
            });
            // Đảm bảo các modal được khởi tạo đúng cách
            var editUserModal = new bootstrap.Modal(document.getElementById('editUserModal'));
            var resetPasswordModal = new bootstrap.Modal(document.getElementById('resetPasswordModal'));
            var deleteUserModal = new bootstrap.Modal(document.getElementById('deleteUserModal'));
            // Xử lý chọn tất cả người dùng
            document.getElementById('selectAllUsers').addEventListener('change', function() {
                var checkboxes = document.getElementsByClassName('user-select');
                for (var i = 0; i < checkboxes.length; i++) {
                    checkboxes[i].checked = this.checked;
                }
            });
            // Ẩn/hiện trường vai trò dựa trên hành động được chọn
            document.querySelector('select[name="bulkAction"]').addEventListener('change', function() {
                var roleGroup = document.getElementById('roleSelectGroup');
                if (this.value === 'updateRole') {
                    roleGroup.style.display = 'block';
                } else {
                    roleGroup.style.display = 'none';
                }
            });
            // Tìm kiếm người dùng
            document.getElementById('userSearch').addEventListener('keyup', function() {
                var filter = this.value.toLowerCase();
                var table = document.getElementById('userTable');
                var tr = table.getElementsByTagName('tr');
                for (var i = 1; i < tr.length; i++) {
                    var td = tr[i].getElementsByTagName('td');
                    var found = false;
                    for (var j = 0; j < td.length; j++) {
                        if (td[j].textContent.toLowerCase().indexOf(filter) > -1) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        tr[i].style.display = '';
                    } else {
                        tr[i].style.display = 'none';
                    }
                }
            });
            // Lọc theo vai trò
            document.getElementById('roleFilter').addEventListener('change', function() {
                var filter = this.value.toLowerCase();
                var table = document.getElementById('userTable');
                var tr = table.getElementsByTagName('tr');
                for (var i = 1; i < tr.length; i++) {
                    var roleCell = tr[i].getElementsByTagName('td')[2]; // Cột vai trò
                    if (filter === '' || roleCell.textContent.toLowerCase().indexOf(filter) > -1) {
                        tr[i].style.display = '';
                    } else {
                        tr[i].style.display = 'none';
                    }
                }
            });
        });
        // Hàm làm mới thống kê
        function refreshStats() {
            window.location.href = 'StorageControl?action=refreshStats';
        }
        // Hàm hiển thị modal chỉnh sửa người dùng
        function editUser(tai_khoan, role, storageLimit) {
            document.getElementById('editTaiKhoan').value = tai_khoan;
            document.getElementById('editUsername').value = tai_khoan;
            document.getElementById('editRole').value = role;
            document.getElementById('editStorageLimit').value = storageLimit / 1048576; // Chuyển đổi từ byte sang MB
            new bootstrap.Modal(document.getElementById('editUserModal')).show();
        }
        // Hàm hiển thị modal đặt lại mật khẩu
        function resetPassword(tai_khoan) {
            document.getElementById('resetTaiKhoan').value = tai_khoan;
            document.getElementById('resetUsername').textContent = tai_khoan;
            new bootstrap.Modal(document.getElementById('resetPasswordModal')).show();
        }
        // Hàm hiển thị modal xóa người dùng
        function deleteUser(tai_khoan) {
            document.getElementById('deleteTaiKhoan').value = tai_khoan;
            document.getElementById('deleteUsername').textContent = tai_khoan;
            new bootstrap.Modal(document.getElementById('deleteUserModal')).show();
        }
        // Kiểm tra mật khẩu trùng khớp
        document.getElementById('confirmPassword').addEventListener('input', function() {
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = this.value;
            if (newPassword !== confirmPassword) {
                this.setCustomValidity('Mật khẩu không khớp');
            } else {
                this.setCustomValidity('');
            }
        });
    </script>
</body>

</html>