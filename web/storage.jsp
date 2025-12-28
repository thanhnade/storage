<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@page import="Model.InforUser,Model.NguoiDung, java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>My Storage</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <link href="bootstrap-5.3.3-dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        /*thanh cuon*/
        html {
            overflow: auto;
            scrollbar-width: none;
            -ms-overflow-style: none;
        }

        html::-webkit-scrollbar {
            width: 0 !important;
            display: none;
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
            background-color: #F46A36;
        }

        .nav-btn.active {
            background-color: #F46A36;
            color: white;
        }

        .navbar {
            background: linear-gradient(#ee4d2d, #ff7337);
        }

        .dropdown-menu {
            background-color: #fff;
            border: none;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
        }

        .nav-link.active {
            background-color: #F46A36 !important;
        }

        .nav-link:hover {
            background-color: rgba(244, 106, 54, 0.1);
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

        .btn-outline-primary:hover {
            background: linear-gradient(#ee4d2d, #ff7337);
            border-color: transparent;
        }
    </style>
</head>
<% HttpSession ss = request.getSession();
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
    %>
<c:choose>
    <c:when test="${nguoi_dung.role == 'client' or nguoi_dung.role == 'admin'}">
    </c:when>
    <c:otherwise>
        <% response.sendRedirect("./login.jsp");%>
    </c:otherwise>
</c:choose>

<body class="pt-5">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark fixed-top">
        <div class="ms-5">
            <a class="navbar-brand d-flex align-items-center" href="StorageServlet">
                <!--<img src="img/logo1.png" alt="Logo" style="width: 30px; height: 30px;">-->
                <i class="fas fa-hdd me-2"></i>
                <span class="ms-1">STORAGE</span>
            </a>
        </div>

        <div class="collapse navbar-collapse" id="navbarContent">
            <ul class="navbar-nav ms-auto align-items-center text-white">

                <li class="nav-item">
                    <c:if test="${nguoi_dung.role == 'admin'}">
                        <span class="nav-link">
                            <i class="fas fa-server me-2"></i>
                            Máy chủ đang kết nối: ${users.size()}
                        </span>
                    </c:if>
                </li>
                <li class="nav-item">
                    <div>
                        <i class="fa-regular fa-user me-2"></i>
                        <span>Hi, ${nguoi_dung.taiKhoan}</span>
                    </div>

                </li>
                <li class="nav-item ms-3 me-3">
                    <a class="dropdown-item" onclick="return confirm('Bạn có chắc chắn muốn đăng xuất không?')"
                        href="StorageServlet?action=logout">
                        <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
                    </a>
                </li>
            </ul>
        </div>
        </div>
    </nav>

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
        <c:if test="${warning != null}">
            <div class="toast align-items-center text-white bg-warning border-0" role="alert" aria-live="assertive"
                aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        <c:out value="${warning}" />
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
                </div>
            </div>
        </c:if>
    </div>

    <!-- Main Container -->
    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-auto px-0 bg-white border-end min-vh-100 position-fixed" style="width: 260px;">
                <div class="p-3">
                    <!-- Create Button -->
                    <div class="dropdown mb-3">
                        <button class="btn btn-primary w-100 rounded-pill py-2" type="button" id="createNew"
                            data-bs-toggle="dropdown">
                            <i class="fas fa-plus me-2"></i>Tạo mới
                        </button>
                        <ul class="dropdown-menu w-100">
                            <li><a class="dropdown-item" href="#" onclick="showCreateFolderModal()">
                                    <i class="fas fa-folder me-2"></i>Thư mục mới
                                </a></li>
                            <li><a class="dropdown-item" href="#" onclick="showCreateFileModal()">
                                    <i class="fas fa-file me-2"></i>Tập tin mới
                                </a></li>
                        </ul>
                    </div>

                    <!-- Upload Button -->
                    <div class="dropdown mb-3">
                        <button class="btn btn-outline-primary w-100 rounded-pill py-2" type="button" id="uploadButton"
                            data-bs-toggle="dropdown">
                            <i class="fas fa-upload me-2"></i>Tải lên
                        </button>
                        <ul class="dropdown-menu w-100">
                            <li>
                                <form action="StorageServlet" method="post" enctype="multipart/form-data"
                                    class="dropdown-item">
                                    <input type="hidden" name="action" value="uploadFile">
                                    <input type="hidden" name="currentPath" value="${currentPath}">
                                    <input type="hidden" name="server" value="${storage_server.host}">
                                    <label for="fileUpload" class="d-block">
                                        <i class="fas fa-file me-2"></i>Tải tập tin lên
                                    </label>
                                    <input type="file" id="fileUpload" name="files" class="d-none" multiple
                                        onchange="this.form.submit()">
                                </form>
                            </li>
                            <li>
                                <form action="StorageServlet" method="post" enctype="multipart/form-data"
                                    class="dropdown-item">
                                    <input type="hidden" name="action" value="uploadFolder">
                                    <input type="hidden" name="currentPath" value="${currentPath}">
                                    <input type="hidden" name="server" value="${storage_server.host}">
                                    <label for="folderUpload" class="d-block">
                                        <i class="fas fa-folder me-2"></i>Tải thư mục
                                        lên
                                    </label>
                                    <input type="file" id="folderUpload" name="folder" class="d-none" webkitdirectory
                                        directory onchange="this.form.submit()">
                                </form>
                            </li>
                        </ul>
                    </div>

                    <!-- Navigation -->
                    <div class="nav flex-column">
                        <div class="folder-tree">
                            <div class="folder-item">
                                <button
                                    class="btn btn-primary w-100 rounded-pill py-2 d-flex justify-content-between align-items-center"
                                    type="button" data-bs-toggle="collapse" data-bs-target="#driveContent"
                                    aria-expanded="false" aria-controls="driveContent">
                                    <div class="d-flex align-items-center">
                                        <i class="fas fa-hdd me-2"></i>
                                        My Storage
                                    </div>
                                    <i class="fas fa-chevron-down"></i>
                                </button>
                                <div class="collapse mt-2" id="driveContent">
                                    <div class="list-group list-group-flush">
                                        <c:choose>
                                            <c:when test="${not empty fileList}">
                                                <c:forEach items="${fileList}" var="file">
                                                    <c:if test="${file.directory == 'true'}">
                                                        <form action="StorageServlet" method="post"
                                                            class="list-group-item list-group-item-action border-0">
                                                            <input type="hidden" name="action" value="openFolder">
                                                            <input type="hidden" name="path" value="${file.path}">
                                                            <input type="hidden" name="name" value="${file.name}">
                                                            <input type="hidden" name="server" value="${file.server}">
                                                            <button type="submit"
                                                                class="btn btn-link text-decoration-none p-0 w-100 text-start">
                                                                <i
                                                                    class="fas fa-folder text-warning me-2"></i>${file.name}
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </c:forEach>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="list-group-item text-muted">
                                                    Không có thư mục nào!</div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="folder-tree mt-3">
                            <div class="folder-item">
                                <button
                                    class="btn btn-outline-primary w-100 rounded-pill py-2 d-flex justify-content-between align-items-center"
                                    type="button" data-bs-toggle="collapse" data-bs-target="#sharedContent"
                                    aria-expanded="false" aria-controls="sharedContent">
                                    <div class="d-flex align-items-center">
                                        <i class="fas fa-share-alt me-2"></i>
                                        Được chia sẻ với tôi
                                    </div>
                                    <i class="fas fa-chevron-down"></i>
                                </button>
                                <div class="collapse mt-2" id="sharedContent">
                                    <div class="list-group list-group-flush">
                                        <c:forEach items="${nfsMounts}" var="mount">
                                            <c:set var="mountPath" value="${mount.mountPoint}" />
                                            <c:set var="folderName"
                                                value="${fn:split(mountPath, '/')[fn:length(fn:split(mountPath, '/'))-1]}" />
                                            <c:set var="sourceServer" value="${fn:split(mount.source, ':')[0]}" />

                                            <form action="StorageServlet" method="post"
                                                class="list-group-item list-group-item-action border-0">
                                                <input type="hidden" name="action" value="openSharedFolder">
                                                <input type="hidden" name="path" value="${mount.mountPoint}">
                                                <button type="submit"
                                                    class="btn btn-link text-decoration-none p-0 w-100 text-start">
                                                    <i class="fas fa-folder text-success me-2"></i>
                                                    ${folderName}
                                                    <small class="text-muted d-block ms-4">
                                                        từ ${sourceServer}
                                                    </small>
                                                </button>
                                            </form>
                                        </c:forEach>

                                        <!-- Hiển thị thông báo nếu không có thư mục được chia sẻ -->
                                        <c:if test="${empty nfsMounts}">
                                            <div class="list-group-item text-muted">
                                                <i class="fas fa-info-circle me-2"></i>
                                                Chưa có thư mục nào được chia sẻ với bạn
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!--Thong tin luu tru-->
                        <div class="card mt-4 border-0 bg-light">
                            <div class="card-body p-3">
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <h6 class="card-title mb-0 text-muted">Dung lượng
                                        lưu trữ</h6>
                                </div>

                                <!-- Total Storage Progress -->
                                <div class="mb-4">
                                    <div class="progress mb-2" style="height: 4px;">
                                        <div class="progress-bar ${storageInfo.usagePercent < 70 ? 'bg-success' : (storageInfo.usagePercent < 85 ? 'bg-warning' : 'bg-danger')}"
                                            role="progressbar" style="width: ${storageInfo.usagePercent}%"
                                            aria-valuenow="${storageInfo.usagePercent}" aria-valuemin="0"
                                            aria-valuemax="100"></div>
                                    </div>
                                    <div class="d-flex justify-content-between small text-muted">
                                        <span
                                            class="${storageInfo.usagePercent < 70 ? 'text-success' : (storageInfo.usagePercent < 85 ? 'text-warning' : 'text-danger')} fw-medium">
                                            <fmt:formatNumber value="${storageInfo.totalUsed}" pattern="#,##0.00" /> MB
                                        </span>
                                        <span>
                                            <fmt:formatNumber value="${storageInfo.storageLimit}" pattern="#,##0.00" />
                                            MB
                                        </span>
                                    </div>
                                </div>

                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Main Content -->
            <div class="col ps-0 ms-auto" style="margin-left: 260px !important;">
                <!-- Content Area -->
                <div class="p-3">
                    <!-- Folders and Files Section -->
                    <div class="card">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <nav aria-label="breadcrumb" class="d-flex align-items-center">
                                <h5 class="mb-0 me-2">
                                    <a href="StorageServlet" class="text-decoration-none text-dark">
                                        My Storage
                                    </a>
                                </h5>
                                <div class="breadcrumb mb-0 d-flex align-items-center">
                                    <c:if test="${not empty currentPath}">
                                        <span class="text-muted mx-2">></span>
                                        <!-- Tách đường dẫn thành mảng -->
                                        <c:set var="pathParts" value="${fn:split(currentPath, '/')}" />

                                        <!-- Xây dựng đường dẫn từng phần -->
                                        <c:set var="buildPath" value="" />
                                        <c:forEach var="part" items="${pathParts}" varStatus="status">
                                            <c:if test="${not empty part}">
                                                <!-- Thêm phần tử vào đường dẫn -->
                                                <c:set var="buildPath" value="${buildPath}/${part}" />
                                                <!-- Chỉ hiển thị các phần sau /data/<server>/<user> -->
                                                <c:if test="${status.index >= 3}">
                                                    <!-- Form điều hướng -->
                                                    <form action="StorageServlet" method="post" style="display: inline;"
                                                        class="breadcrumb-form">
                                                        <input type="hidden" name="action" value="openFolder">
                                                        <input type="hidden" name="path" value="${buildPath}">
                                                        <input type="hidden" name="name" value="">
                                                        <input type="hidden" name="server"
                                                            value="${storage_server.host}">
                                                        <button type="submit"
                                                            class="btn btn-link p-0 text-muted text-decoration-none">
                                                            ${part}
                                                        </button>
                                                    </form>

                                                    <!-- Thêm dấu > nếu không phải phần tử cuối -->
                                                    <c:if test="${!status.last}">
                                                        <span class="text-muted mx-2">></span>
                                                    </c:if>
                                                </c:if>
                                            </c:if>
                                        </c:forEach>
                                    </c:if>
                                </div>
                            </nav>
                            <!-- Thay đổi nút Delete Multiple và thêm Download Multiple -->
                            <div class="d-flex justify-content-between align-items-end">
                                <div class="d-flex me-1">
                                    <input type="text" class="form-control" id="searchInput"
                                        placeholder="Tìm kiếm file/thư mục...">
                                    <button class="btn btn-primary" type="button" onclick="searchFiles()">
                                        <i class="fas fa-search"></i>
                                    </button>
                                </div>
                                <button id="toggleCheckboxBtn" class="btn btn-primary" onclick="toggleCheckboxes()">
                                    <i class="fas fa-check-square me-2"></i>Chọn nhiều
                                </button>
                                <button id="deleteMultipleBtn" class="btn btn-danger" style="display: none;"
                                    onclick="deleteMultipleItems()">
                                    <i class="fas fa-trash me-2"></i>Xóa đã chọn
                                </button>
                            </div>
                        </div>
                        <div class="card-body">
                            <!-- Thông báo kết quả tìm kiếm -->
                            <div id="searchResultsAlert" class="alert alert-info d-none mb-3">
                                <div class="d-flex justify-content-between align-items-center">
                                    <div>
                                        <i class="fas fa-search me-2"></i>
                                        <span id="searchResultsText">Kết quả tìm kiếm</span>
                                    </div>
                                    <button type="button" class="btn btn-sm btn-outline-secondary"
                                        onclick="clearSearch()">
                                        <i class="fas fa-times me-1"></i>Xóa bộ lọc
                                    </button>
                                </div>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-hover">
                                    <thead>
                                        <tr>
                                            <th>Tên</th>
                                            <th>Loại</th>
                                            <th>Kích thước</th>
                                            <th>Ngày sửa</th>
                                            <th>Thao tác</th>
                                            <th class="checkbox-column" style="display: none;">
                                                <input type="checkbox" id="selectAll" class="form-check-input"
                                                    onclick="toggleAllCheckboxes(this)">
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:choose>
                                            <c:when test="${empty fileList and empty pathOnServer}">
                                                <tr>
                                                    <td colspan="6" class="text-center">
                                                        <div class="alert alert-info mb-0">
                                                            <i class="fas fa-info-circle me-2"></i>Không
                                                            có tệp nào
                                                        </div>
                                                    </td>
                                                </tr>
                                            </c:when>
                                            <c:otherwise>
                                                <c:forEach items="${fileList}" var="file">
                                                    <tr>
                                                        <td>
                                                            <c:choose>
                                                                <c:when test="${file.directory == 'true'}">
                                                                    <form action="StorageServlet" method="post"
                                                                        class="d-inline">
                                                                        <input type="hidden" name="action"
                                                                            value="openFolder">
                                                                        <input type="hidden" name="path"
                                                                            value="${file.path}">
                                                                        <input type="hidden" name="name"
                                                                            value="${file.name}">
                                                                        <input type="hidden" name="server"
                                                                            value="${file.server}">
                                                                        <button type="submit"
                                                                            class="btn btn-link text-decoration-none p-0">
                                                                            <i
                                                                                class="fas fa-folder text-warning me-2"></i>
                                                                            ${file.name}
                                                                            <c:if test="${nguoi_dung.role == 'admin'}">
                                                                                (${file.server})
                                                                            </c:if>
                                                                        </button>
                                                                    </form>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <i class="fas fa-file text-primary me-2"></i>
                                                                    ${file.name}
                                                                    <c:if test="${nguoi_dung.role == 'admin'}">
                                                                        (${file.server})
                                                                    </c:if>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td>${file.directory == 'true' ?
                                                            'Thư
                                                            mục' : 'Tập tin'}</td>
                                                        <td>
                                                            <c:choose>
                                                                <c:when test="${file.directory == 'true'}">
                                                                    <c:out value="${file.size}" />
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:choose>
                                                                        <c:when test="${file.size < 1024}">
                                                                            <c:out value="${file.size}" />
                                                                            B
                                                                        </c:when>
                                                                        <c:when test="${file.size < 1048576}">
                                                                            <fmt:formatNumber
                                                                                value="${file.size / 1024.0}"
                                                                                pattern="#,##0.00" />
                                                                            KB
                                                                        </c:when>
                                                                        <c:when test="${file.size < 1073741824}">
                                                                            <fmt:formatNumber
                                                                                value="${file.size / (1024.0 * 1024.0)}"
                                                                                pattern="#,##0.00" />
                                                                            MB
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <fmt:formatNumber
                                                                                value="${file.size / (1024.0 * 1024.0 * 1024.0)}"
                                                                                pattern="#,##0.00" />
                                                                            GB
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </td>

                                                        <td>${file.modified}</td>
                                                        <td>
                                                            <div class="btn-group">
                                                                <!-- Nút tải xuống cho cả thư mục và tập tin -->
                                                                <form action="StorageServlet" method="post"
                                                                    class="d-inline me-2">
                                                                    <input type="hidden" name="action" value="download">
                                                                    <input type="hidden" name="name"
                                                                        value="${file.name}">
                                                                    <input type="hidden" name="currentPath"
                                                                        value="${file.path}">
                                                                    <input type="hidden" name="server"
                                                                        value="${file.server}">
                                                                    <input type="hidden" name="isDirectory"
                                                                        value="${file.directory}">
                                                                    <button type="submit" class="btn btn-primary btn-sm"
                                                                        title="Tải xuống">
                                                                        <i class="fas fa-download"></i>
                                                                    </button>
                                                                </form>

                                                                <!-- Nút đổi tên -->
                                                                <form action="StorageServlet" method="post"
                                                                    class="d-inline me-2">
                                                                    <input type="hidden" name="action" value="rename">
                                                                    <input type="hidden" name="currentPath"
                                                                        value="${file.path}">
                                                                    <input type="hidden" name="server"
                                                                        value="${file.server}">
                                                                    <input type="hidden" name="oldName"
                                                                        value="${file.name}">
                                                                    <button type="button" class="btn btn-warning btn-sm"
                                                                        onclick="showRenameModal('${file.name}', '${file.path}', '${file.server}')"
                                                                        title="Đổi tên">
                                                                        <i class="fas fa-edit"></i>
                                                                    </button>
                                                                </form>

                                                                <!-- Nút xóa -->
                                                                <form action="StorageServlet" method="post"
                                                                    class="d-inline">
                                                                    <input type="hidden" name="action" value="delete">
                                                                    <input type="hidden" name="name"
                                                                        value="${file.name}">
                                                                    <input type="hidden" name="currentPath"
                                                                        value="${file.path}">
                                                                    <input type="hidden" name="server"
                                                                        value="${file.server}">
                                                                    <button type="submit" class="btn btn-danger btn-sm"
                                                                        onclick="return confirm('Bạn có chắc chắn muốn xóa ${file.directory == 'true' ? 'thư mục' : 'tập tin'} này?')"
                                                                        title="Xóa">
                                                                        <i class="fas fa-trash"></i>
                                                                    </button>
                                                                </form>

                                                                <!-- Thêm nút chia sẻ NFS -->
                                                                <c:if test="${file.directory == 'true'}">
                                                                    <button type="button"
                                                                        class="btn btn-success btn-sm d-none"
                                                                        onclick="showNFSShareModal('${file.name}', '${file.path}', '${file.server}')"
                                                                        title="Chia sẻ qua NFS">
                                                                        <i class="fas fa-share-alt"></i>
                                                                    </button>
                                                                </c:if>
                                                            </div>
                                                        </td>
                                                        <td class="checkbox-column" style="display: none;">
                                                            <input type="checkbox"
                                                                class="form-check-input item-checkbox"
                                                                data-name="${file.name}" data-path="${file.path}"
                                                                data-server="${file.server}"
                                                                data-directory="${file.directory}">
                                                        </td>
                                                    </tr>
                                                </c:forEach>
                                            </c:otherwise>
                                        </c:choose>

                                        <!-- Hiển thị các file/folder từ savedPaths -->
                                        <c:if test="${empty currentPath}">
                                            <c:forEach items="${sessionScope.pathOnServer}" var="entry">
                                                <c:if test="${not entry.value.exists}">
                                                    <tr>
                                                        <td>
                                                            <c:choose>
                                                                <c:when test="${entry.value.isDirectory == true}">
                                                                    <i class="fas fa-folder text-warning me-2"></i>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <i class="fas fa-file text-primary me-2"></i>
                                                                </c:otherwise>
                                                            </c:choose>
                                                            ${entry.key}
                                                        </td>
                                                        <td>
                                                            ${entry.value.isDirectory == true ? 'Thư mục' : 'Tập tin'}
                                                        </td>
                                                        <td>
                                                            <c:choose>
                                                                <c:when test="${entry.value.size < 1024}">
                                                                    <c:out value="${entry.value.size}" />
                                                                    B
                                                                </c:when>
                                                                <c:when test="${entry.value.size < 1048576}">
                                                                    <fmt:formatNumber
                                                                        value="${entry.value.size / 1024.0}"
                                                                        pattern="#,##0.00" />
                                                                    KB
                                                                </c:when>
                                                                <c:when test="${entry.value.size < 1073741824}">
                                                                    <fmt:formatNumber
                                                                        value="${entry.value.size / (1024.0 * 1024.0)}"
                                                                        pattern="#,##0.00" />
                                                                    MB
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <fmt:formatNumber
                                                                        value="${entry.value.size / (1024.0 * 1024.0 * 1024.0)}"
                                                                        pattern="#,##0.00" />
                                                                    GB
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td>
                                                            <span class="badge bg-secondary">Không khả dụng</span>
                                                        </td>
                                                        <td>
                                                            <button class="btn btn-sm btn-outline-secondary" disabled>
                                                                <i class="fas fa-ban"></i>
                                                            </button>
                                                        </td>
                                                    </tr>
                                                </c:if>
                                            </c:forEach>
                                        </c:if>

                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
    <c:if test="${nguoi_dung.role == 'admin'}">
        <div class="position-fixed bottom-0 end-0 w-100">
            <%@include file="include/footer.jsp" %>
        </div>
    </c:if>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Khởi tạo tất cả các toast
            var toastElList = [].slice.call(document.querySelectorAll('.toast'));
            var toastList = toastElList.map(function(toastEl) {
                var toast = new bootstrap.Toast(toastEl, {
                    autohide: true,
                    delay: 5000
                });
                toast.show();
                return toast;
            });
        });
    </script>
    <!-- Modals -->
    <!-- Create Folder Modal -->
    <div class="modal fade" id="createFolderModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header" style="background: linear-gradient(#ee4d2d,#ff7337); color: white;">
                    <h5 class="modal-title">Tạo thư mục mới</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <form action="StorageServlet" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="folder">
                        <input type="hidden" name="currentPath" value="${currentPath}">
                        <input type="hidden" name="server" value="${storage_server.host}">

                        <div class="mb-3">
                            <c:if test="${nguoi_dung.role == 'admin'}">
                                <label class="form-label">Vị trí hiện tại</label>
                                <input type="text" class="form-control" value="${currentPath}" disabled>
                                <input type="text" name="server" value="${storage_server.host}" disabled>
                            </c:if>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Tên thư mục</label>
                            <input class="form-control" type="text" name="name" pattern="^[a-zA-Z0-9_\-\s]+$"
                                title="Tên thư mục chỉ có thể chứa chữ cái, số, dấu gạch dưới (_), dấu gạch ngang (-), và khoảng trắng. Ví dụ: project_files, user_documents, data backup, ..."
                                required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Tạo</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Create File Modal -->
    <div class="modal fade" id="createFileModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header" style="background: linear-gradient(#ee4d2d,#ff7337); color: white;">
                    <h5 class="modal-title">Tạo tập tin mới</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <form action="StorageServlet" method="post">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="file">
                        <input type="hidden" name="currentPath" value="${currentPath}">
                        <input type="hidden" name="server" value="${storage_server.host}">
                        <div class="mb-3">
                            <c:if test="${nguoi_dung.role == 'admin'}">
                                <label class="form-label">Vị trí hiện tại</label>
                                <input type="text" class="form-control" value="${currentPath}" disabled>
                                <input type="text" name="server" value="${storage_server.host}" disabled>
                            </c:if>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Tên tập tin</label>
                            <input class="form-control" type="text" name="name" pattern="^[a-zA-Z0-9_\-\.\s]+$"
                                title="Tên file chỉ có thể chứa chữ cái, số, dấu gạch dưới, dấu gạch ngang, dấu chấm và khoảng trắng. Ví dụ: document.txt, image_2025.jpg, project-report_v1.pdf, moi.txt, ..."
                                required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Tạo</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Rename Modal -->
    <div class="modal fade" id="renameModal" tabindex="-1" aria-labelledby="renameModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header" style="background: linear-gradient(#ee4d2d,#ff7337); color: white;">
                    <h5 class="modal-title" id="renameModalLabel">Đổi tên</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                        aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form id="renameForm" method="post">
                        <input type="hidden" name="action" value="rename">
                        <input type="hidden" name="currentPath" id="renameCurrentPath">
                        <input type="hidden" name="server" id="renameServer">
                        <input type="hidden" name="oldName" id="oldName">
                        <div class="mb-3">
                            <label for="newName" class="form-label">Tên mới</label>
                            <input type="text" class="form-control" id="newName" name="newName">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-primary" onclick="submitRename()">Đổi tên</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Share Folder Modal -->
    <div class="modal fade" id="nfsShareModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header" style="background: linear-gradient(#ee4d2d,#ff7337); color: white;">
                    <h5 class="modal-title">
                        <i class="fas fa-share-alt me-2"></i>
                        Chia sẻ thư mục qua NFS
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <form action="StorageServlet" method="post" id="nfsShareForm" onsubmit="return validateShareForm()">
                    <div class="modal-body">
                        <input type="hidden" name="action" value="shareNFS">
                        <input type="hidden" name="folderPath" id="nfsSharePath">
                        <input type="hidden" name="folderName" id="nfsShareName">
                        <input type="hidden" name="serverHost" id="nfsShareServer">

                        <div class="mb-3">
                            <label class="form-label">Thư mục chia sẻ:</label>
                            <input type="text" class="form-control" id="displayNFSPath" readonly>
                            <small class="form-text text-muted">
                                Đường dẫn đầy đủ của thư mục sẽ được chia sẻ
                            </small>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Nhập tên người dùng:</label>
                            <div class="input-group">
                                <input type="text" class="form-control" name="targetUser" id="targetUser"
                                    placeholder="Nhập tên người dùng cần chia sẻ">
                                <button class="btn btn-outline-primary" type="button" onclick="checkUser()">
                                    <i class="fas fa-search me-1"></i>Kiểm tra
                                </button>
                            </div>
                            <div id="userCheckResult" class="form-text mt-2"></div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Quyền truy cập:</label>
                            <select class="form-select" name="permissions">
                                <option value="rw">Đọc và ghi (rw)</option>
                                <option value="ro">Chỉ đọc (ro)</option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Tùy chọn bổ sung:</label>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="options" value="sync" checked>
                                <label class="form-check-label">Đồng bộ (sync)</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="options" value="no_subtree_check">
                                <label class="form-check-label">Không kiểm tra thư mục
                                    con (no_subtree_check)</label>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            <i class="fas fa-times me-2"></i>Hủy
                        </button>
                        <button type="submit" class="btn btn-success" id="shareButton" disabled>
                            <i class="fas fa-share-alt me-2"></i>Chia sẻ
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function showCreateFolderModal() {
            new bootstrap.Modal(document.getElementById('createFolderModal')).show();
        }

        function showCreateFileModal() {
            new bootstrap.Modal(document.getElementById('createFileModal')).show();
        }

        function handleItemClick(name, isDirectory) {
            if (isDirectory) {
                window.location.href = 'StorageServlet?path=${currentPath}/' + name;
            }
        }

        function toggleCheckboxes() {
            const checkboxColumns = document.querySelectorAll('.checkbox-column');
            const toggleBtn = document.getElementById('toggleCheckboxBtn');
            const deleteBtn = document.getElementById('deleteMultipleBtn');
            // Toggle hiển thị các cột checkbox
            checkboxColumns.forEach(col => {
                if (col.style.display === 'none') {
                    col.style.display = '';
                    toggleBtn.innerHTML = '<i class="fas fa-times me-2"></i>Hủy chọn';
                    deleteBtn.style.display = '';
                } else {
                    col.style.display = 'none';
                    toggleBtn.innerHTML = '<i class="fas fa-check-square me-2"></i>Chọn nhiều';
                    deleteBtn.style.display = 'none';
                    // Bỏ chọn tất cả các checkbox khi ẩn
                    document.querySelectorAll('.item-checkbox').forEach(cb => cb.checked = false);
                    document.getElementById('selectAll').checked = false;
                }
            });
            updateButtons();
        }

        function toggleAllCheckboxes(headerCheckbox) {
            const checkboxes = document.querySelectorAll('.item-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = headerCheckbox.checked;
            });
            updateButtons();
        }

        function updateButtons() {
            const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
            const deleteBtn = document.getElementById('deleteMultipleBtn');
            const headerCheckbox = document.getElementById('selectAll');
            // Cập nhật trạng thái của header checkbox
            headerCheckbox.checked = checkedBoxes.length > 0 && checkedBoxes.length === document.querySelectorAll(
                '.item-checkbox').length;
            headerCheckbox.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < document.querySelectorAll(
                '.item-checkbox').length;
            if (checkedBoxes.length > 0) {
                deleteBtn.innerHTML = `<i class="fas fa-trash me-2"></i>Xóa tất cả`;
            } else {
                deleteBtn.innerHTML = `<i class="fas fa-trash me-2"></i>Xóa đã chọn`;
            }
        }
        document.querySelectorAll('.item-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', updateButtons);
        });

        function deleteMultipleItems() {
            const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
            if (checkedBoxes.length === 0)
                return;
            const items = Array.from(checkedBoxes).map(checkbox => ({
                name: checkbox.dataset.name,
                path: checkbox.dataset.path,
                server: checkbox.dataset.server,
                directory: checkbox.dataset.directory
            }));
            if (confirm('Bạn có chắc chắn muốn xóa ' + items.length + ' mục đã chọn?')) {
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = 'StorageServlet';
                const actionInput = document.createElement('input');
                actionInput.type = 'hidden';
                actionInput.name = 'action';
                actionInput.value = 'deleteMultiple';
                form.appendChild(actionInput);
                const itemsInput = document.createElement('input');
                itemsInput.type = 'hidden';
                itemsInput.name = 'items';
                itemsInput.value = JSON.stringify(items);
                form.appendChild(itemsInput);
                // Thêm đường dẫn hiện tại
                const currentPathInput = document.createElement('input');
                currentPathInput.type = 'hidden';
                currentPathInput.name = 'currentPath';
                currentPathInput.value = '${currentPath}';
                form.appendChild(currentPathInput);
                document.body.appendChild(form);
                form.submit();
            }
        }

        function showRenameModal(name, path, server) {
            // Điền thông tin vào form
            document.getElementById('oldName').value = name;
            document.getElementById('newName').value = name;
            document.getElementById('renameCurrentPath').value = path;
            document.getElementById('renameServer').value = server;
            // Kiểm tra xem là file hay folder để set pattern và title phù hợp
            const isDirectory = path.endsWith('/');
            const newNameInput = document.getElementById('newName');
            if (isDirectory) {
                newNameInput.pattern = "^(?!\\.|.*[<>:\"/\\\\|?*])([a-zA-Z0-9 _\\-]+)$";
                newNameInput.title =
                    "Tên thư mục chỉ có thể chứa chữ cái, số, dấu gạch dưới, dấu gạch ngang và khoảng trắng. Ví dụ: project_files, user_documents, data backup, ...";
            } else {
                newNameInput.pattern = "^(?!\\.{1,2}$)(?!.*[<>:\"/\\\\|?*])([a-zA-Z0-9 _\\-.]+)\\.([a-zA-Z0-9]{1,5})$";
                newNameInput.title =
                    "Tên file phải có định dạng tên.đuôi, không chứa ký tự đặc biệt < > : \" / \\ | ? *. Ví dụ: document.txt, image_2025.jpg";
            }
            // Hiển thị modal
            var renameModal = new bootstrap.Modal(document.getElementById('renameModal'));
            renameModal.show();
        }

        function submitRename() {
            var newName = document.getElementById('newName').value;
            if (!newName) {
                alert('Vui lòng nhập tên mới!');
                return;
            }
            // Gửi form
            document.getElementById('renameForm').submit();
        }

        function showNFSShareModal(name, path, server) {
            document.getElementById('nfsShareName').value = name;
            document.getElementById('nfsSharePath').value = path;
            document.getElementById('nfsShareServer').value = server;
            document.getElementById('displayNFSPath').value = path;
            // Reset form
            document.getElementById('targetUser').value = '';
            document.getElementById('userCheckResult').innerHTML = '';
            document.getElementById('shareButton').disabled = true;
            var modal = new bootstrap.Modal(document.getElementById('nfsShareModal'));
            modal.show();
        }

        function checkUser() { //Share NFS
            const targetUser = document.getElementById('targetUser').value.trim();
            const resultDiv = document.getElementById('userCheckResult');
            const shareButton = document.getElementById('shareButton');
            if (!targetUser) {
                resultDiv.innerHTML =
                    '<span class="text-danger"><i class="fas fa-exclamation-circle me-1"></i>Vui lòng nhập tên người dùng</span>';
                shareButton.disabled = true;
                return;
            }
            // Gửi request kiểm tra người dùng
            const form = new FormData();
            form.append('action', 'checkUser');
            form.append('targetUser', targetUser);
            fetch('StorageServlet', {
                    method: 'POST',
                    body: form
                })
                .then(response => response.text())
                .then(result => {
                    switch (result) {
                        case 'EXISTS':
                            resultDiv.innerHTML =
                                '<span class="text-success"><i class="fas fa-check-circle me-1"></i>Người dùng hợp lệ</span>';
                            shareButton.disabled = false;
                            break;
                        case 'SELF':
                            resultDiv.innerHTML =
                                '<span class="text-danger"><i class="fas fa-exclamation-circle me-1"></i>Không thể chia sẻ cho chính bạn</span>';
                            shareButton.disabled = true;
                            break;
                        case 'NOT_FOUND':
                            resultDiv.innerHTML =
                                '<span class="text-danger"><i class="fas fa-times-circle me-1"></i>Không tìm thấy người dùng</span>';
                            shareButton.disabled = true;
                            break;
                        default:
                            resultDiv.innerHTML =
                                '<span class="text-danger"><i class="fas fa-exclamation-circle me-1"></i>Lỗi kiểm tra người dùng</span>';
                            shareButton.disabled = true;
                    }
                })
                .catch(error => {
                    resultDiv.innerHTML =
                        '<span class="text-danger"><i class="fas fa-exclamation-circle me-1"></i>Lỗi kết nối</span>';
                    shareButton.disabled = true;
                });
        }

        function validateShareForm() {
            const targetUser = document.getElementById('targetUser').value.trim();
            if (!targetUser) {
                alert('Vui lòng nhập tên người dùng!');
                return false;
            }
            const shareButton = document.getElementById('shareButton');
            if (shareButton.disabled) {
                alert('Vui lòng kiểm tra người dùng trước khi chia sẻ!');
                return false;
            }
            return true;
        }
        // Hàm tìm kiếm file và thư mục
        function searchFiles() {
            const searchText = document.getElementById('searchInput').value.toLowerCase().trim();
            const tableRows = document.querySelectorAll('table tbody tr');
            const alertBox = document.getElementById('searchResultsAlert');
            const resultText = document.getElementById('searchResultsText');
            let visibleCount = 0;
            if (!searchText) {
                clearSearch();
                return;
            }
            tableRows.forEach(function(row) {
                const fileNameCell = row.querySelector('td:first-child');
                if (fileNameCell) {
                    const fileName = fileNameCell.textContent.trim().toLowerCase();
                    if (fileName.includes(searchText)) {
                        row.style.display = '';
                        visibleCount++;
                    } else {
                        row.style.display = 'none';
                    }
                }
            });
            // Hiển thị thông báo kết quả tìm kiếm
            alertBox.classList.remove('d-none');
            resultText.textContent = 'Kết quả tìm kiếm cho: "' + searchText + '" (' + visibleCount + ' kết quả)';
            // Kiểm tra nếu không có kết quả nào
            if (visibleCount === 0) {
                const noResultsRow = document.getElementById('noResultsRow');
                if (!noResultsRow) {
                    const emptyRow = document.createElement('tr');
                    emptyRow.id = 'noResultsRow';
                    emptyRow.innerHTML =
                        '<td colspan="6" class="text-center"><div class="alert alert-warning mb-0">Không tìm thấy kết quả nào cho "' +
                        searchText + '"</div></td>';
                    const tbody = document.querySelector('table tbody');
                    tbody.appendChild(emptyRow);
                }
            } else {
                // Xóa thông báo không có kết quả nếu có
                const noResultsRow = document.getElementById('noResultsRow');
                if (noResultsRow) {
                    noResultsRow.remove();
                }
            }
        }
        // Hàm xóa bộ lọc tìm kiếm
        function clearSearch() {
            const searchInput = document.getElementById('searchInput');
            const tableRows = document.querySelectorAll('table tbody tr');
            const alertBox = document.getElementById('searchResultsAlert');
            // Xóa nội dung tìm kiếm
            searchInput.value = '';
            // Hiển thị lại tất cả các hàng
            tableRows.forEach(function(row) {
                row.style.display = '';
            });
            // Ẩn thông báo kết quả tìm kiếm
            alertBox.classList.add('d-none');
            // Xóa thông báo không có kết quả nếu có
            const noResultsRow = document.getElementById('noResultsRow');
            if (noResultsRow) {
                noResultsRow.remove();
            }
        }
        // Xử lý tìm kiếm khi nhấn Enter
        document.addEventListener('DOMContentLoaded', function() {
            const searchInput = document.getElementById('searchInput');
            if (searchInput) {
                searchInput.addEventListener('keyup', function(event) {
                    if (event.key === 'Enter') {
                        searchFiles();
                    }
                });
            }
        });
    </script>
</body>

</html>