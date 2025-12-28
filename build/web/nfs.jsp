<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@page import="Model.InforUser, java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>NFS</title>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png" />
        <link rel="stylesheet" href="bootstrap-5.3.3-dist/css/bootstrap.min.css" />
        <link rel="stylesheet" href="css/main-machine.css" />
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap"
              rel="stylesheet">
        <script src="https://kit.fontawesome.com/2cbc3b080b.js" crossorigin="anonymous"></script>
    </head>

    <body>
        <% InforUser u = (InforUser) session.getAttribute("user");
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
                    <h4 class="text-center">DỊCH VỤ NFS</h4>
                </div>
                <div class="row bg-white p-3 border-top">
                    <div class="row">
                        <div class="col-4">
                            <!-- Điều khiển dịch vụ NFS -->
                            <div class="card border-0 shadow-sm mb-4">
                                <div class="card-header text-center">
                                    <h5>Điều khiển dịch vụ NFS</h5>
                                    <c:if test="${nfsStatus ne null}">
                                        <div class="mt-2">
                                            <c:choose>
                                                <c:when test="${nfsStatus eq 'active'}">
                                                    <div class="alert alert-success mb-0 py-2">
                                                        <i class="fas fa-check-circle me-2"></i>
                                                        Dịch vụ NFS đang hoạt động
                                                    </div>
                                                </c:when>
                                                <c:when test="${nfsStatus eq 'inactive'}">
                                                    <div class="alert alert-warning mb-0 py-2">
                                                        <i class="fas fa-exclamation-circle me-2"></i>
                                                        Dịch vụ NFS đang dừng
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="alert alert-danger mb-0 py-2">
                                                        <i class="fas fa-times-circle me-2"></i>
                                                        Không thể xác định trạng thái NFS
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </c:if>
                                </div>
                                <div class="card-body">
                                    <form action="configNFS" method="post">
                                        <div class="d-grid gap-2">
                                            <button class="btn btn-success"
                                                    type="submit" name="action"
                                                    value="start">
                                                Start NFS
                                            </button>
                                            <button class="btn btn-warning text-white"
                                                    type="submit" name="action"
                                                    value="restart">
                                                Restart NFS
                                            </button>
                                            <button class="btn btn-info text-white"
                                                    type="submit" name="action"
                                                    value="status">
                                                Status NFS
                                            </button>
                                            <button class="btn btn-danger" type="submit"
                                                    name="action" value="stop">
                                                Stop NFS
                                            </button>
                                        </div>
                                        <input type="hidden" name="host"
                                               value="<%=u.getHost()%>">
                                        <input type="hidden" name="port"
                                               value="<%=u.getPort()%>">
                                        <input type="hidden" name="user"
                                               value="<%=u.getUser()%>">
                                        <input type="hidden" name="password"
                                               value="<%=u.getPassword()%>">
                                    </form>
                                </div>
                            </div>
                        </div>

                        <div class="col-8">
                            <!-- Thư mục /home/user -->
                            <div class="card border-0 shadow-sm mb-3">
                                <div class="card-header">
                                    <div
                                        class="d-flex justify-content-between align-items-center">
                                        <h5 class="mb-0">Danh sách thư mục máy chủ</h5>

                                    </div>
                                </div>
                                <div class="card-body">
                                    <div class="table-responsive">
                                        <table class="table table-hover">
                                            <thead>
                                                <tr>
                                                    <th>Máy chủ</th>
                                                    <th>Tên</th>
                                                    <th>Loại</th>
                                                    <th>Thao tác</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <%--<c:forEach var="user"
                                                    items="${users}">--%>
                                                <tr class="table-secondary">
                                                    <td colspan="4">
                                                        <strong>${user.host}
                                                            (${user.user})</strong>
                                                        <form action="configNFS"
                                                              method="post"
                                                              class="d-inline float-end">
                                                            <input type="hidden"
                                                                   name="action"
                                                                   value="listFiles">
                                                            <input type="hidden"
                                                                   name="host"
                                                                   value="${user.host}">
                                                            <input type="hidden"
                                                                   name="port"
                                                                   value="${user.port}">
                                                            <input type="hidden"
                                                                   name="user"
                                                                   value="${user.user}">
                                                            <input type="hidden"
                                                                   name="password"
                                                                   value="${user.password}">
                                                            <button
                                                                class="btn btn-sm btn-outline-primary"
                                                                type="submit">
                                                                Xem thư mục
                                                            </button>
                                                        </form>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="4">
                                                        <div class="btn-group">
                                                            <button
                                                                class="btn btn-success btn-sm"
                                                                onclick="showCreateModal('folder')">
                                                                <i
                                                                    class="fa-solid fa-folder-plus"></i>
                                                                Tạo thư mục
                                                            </button>
                                                            <button
                                                                class="btn btn-primary btn-sm"
                                                                onclick="showCreateModal('file')">
                                                                <i
                                                                    class="fa-solid fa-file-circle-plus"></i>
                                                                Tạo tập tin
                                                            </button>
                                                            <button
                                                                class="btn btn-info btn-sm text-white"
                                                                onclick="document.getElementById('uploadFile').click()">
                                                                <i
                                                                    class="fa-solid fa-upload"></i>
                                                                Tải lên
                                                            </button>
                                                            <form id="uploadForm"
                                                                  action="configNFS"
                                                                  method="post"
                                                                  enctype="multipart/form-data"
                                                                  style="display:none">
                                                                <input type="file"
                                                                       id="uploadFile"
                                                                       name="file"
                                                                       onchange="submitUpload()"
                                                                       multiple>
                                                                <input type="hidden"
                                                                       name="action"
                                                                       value="upload">
                                                                <input type="hidden"
                                                                       name="currentPath"
                                                                       value="${currentPath}">
                                                                <input type="hidden"
                                                                       name="host"
                                                                       value="${user.host}">
                                                                <input type="hidden"
                                                                       name="port"
                                                                       value="${user.port}">
                                                                <input type="hidden"
                                                                       name="user"
                                                                       value="${user.user}">
                                                                <input type="hidden"
                                                                       name="password"
                                                                       value="${user.password}">
                                                            </form>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <c:if
                                                    test="${user.host eq selectedHost}">
                                                    <c:forEach var="file"
                                                               items="${fileList}">
                                                        <c:choose>
                                                            <c:when
                                                                test="${file.name eq '..'}">

                                                                <tr>
                                                                    <td></td>
                                                                    <td colspan="3">
                                                                        <c:set
                                                                            var="pathParts"
                                                                            value="${fn:split(currentPath, '/')}" />
                                                                        <c:set
                                                                            var="parentFolder"
                                                                            value="${
                                                                            fn:length(pathParts) > 2 ? 
                                                                                pathParts[fn:length(pathParts)-2] : 
                                                                                'home'
                                                                            }" />

                                                                        <form
                                                                            action="configNFS"
                                                                            method="post"
                                                                            class="d-inline">
                                                                            <input
                                                                                type="hidden"
                                                                                name="action"
                                                                                value="listFiles">
                                                                            <input
                                                                                type="hidden"
                                                                                name="host"
                                                                                value="${user.host}">
                                                                            <input
                                                                                type="hidden"
                                                                                name="port"
                                                                                value="${user.port}">
                                                                            <input
                                                                                type="hidden"
                                                                                name="user"
                                                                                value="${user.user}">
                                                                            <input
                                                                                type="hidden"
                                                                                name="password"
                                                                                value="${user.password}">
                                                                            <input
                                                                                type="hidden"
                                                                                name="path"
                                                                                value="${currentPath}/../">
                                                                            <button
                                                                                class="btn btn-link text-decoration-none p-0"
                                                                                type="submit">
                                                                                <i
                                                                                    class="fa-solid fa-arrow-left"></i>
                                                                                ${parentFolder}
                                                                            </button>
                                                                        </form>
                                                                    </td>
                                                                </tr>
                                                            </c:when>

                                                            <c:otherwise>
                                                                <tr>
                                                                    <td></td>
                                                                    <td>
                                                                        <c:choose>
                                                                            <c:when
                                                                                test="${file.directory eq 'true'}">
                                                                                <form
                                                                                    action="configNFS"
                                                                                    method="post"
                                                                                    class="d-inline">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="action"
                                                                                        value="listFiles">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="host"
                                                                                        value="${user.host}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="port"
                                                                                        value="${user.port}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="user"
                                                                                        value="${user.user}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="password"
                                                                                        value="${user.password}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="path"
                                                                                        value="${currentPath}/${file.name}">
                                                                                    <button
                                                                                        class="btn btn-link text-decoration-none p-0"
                                                                                        type="submit">
                                                                                        📁
                                                                                        ${file.name}
                                                                                    </button>
                                                                                </form>
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                📄
                                                                                ${file.name}
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </td>
                                                                    <td>${file.directory
                                                                          eq 'true' ?
                                                                          'Thư
                                                                          mục' : 'Tập
                                                                          tin'}</td>
                                                                    <td>
                                                                        <div
                                                                            class="btn-group btn-group-sm">
                                                                            <c:if
                                                                                test="${file.directory eq 'true'}">
                                                                                <button
                                                                                    class="btn btn-success btn-sm"
                                                                                    type="button"
                                                                                    onclick="showShareModal('${currentPath}/${file.name}', '${user.host}', '${user.port}', '${user.user}', '${user.password}')"
                                                                                    title="Chia sẻ thư mục">
                                                                                    <i
                                                                                        class="fa-solid fa-share-nodes"></i>
                                                                                </button>

                                                                            </c:if>
                                                                            <c:if
                                                                                test="${file.directory eq 'false'}">
                                                                                <form
                                                                                    action="configNFS"
                                                                                    method="post"
                                                                                    class="d-inline">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="action"
                                                                                        value="download">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="path"
                                                                                        value="${currentPath}/${file.name}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="host"
                                                                                        value="${user.host}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="port"
                                                                                        value="${user.port}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="user"
                                                                                        value="${user.user}">
                                                                                    <input
                                                                                        type="hidden"
                                                                                        name="password"
                                                                                        value="${user.password}">
                                                                                    <button
                                                                                        class="btn btn-primary btn-sm"
                                                                                        type="submit">
                                                                                        ⬇️
                                                                                    </button>
                                                                                </form>
                                                                            </c:if>
                                                                            <form
                                                                                action="configNFS"
                                                                                method="post"
                                                                                class="d-inline"
                                                                                onsubmit="return confirm('Bạn có chắc chắn muốn xóa ${file.name} không?');">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="action"
                                                                                    value="delete">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="fileName"
                                                                                    value="${file.name}">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="currentPath"
                                                                                    value="${currentPath}">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="host"
                                                                                    value="${user.host}">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="port"
                                                                                    value="${user.port}">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="user"
                                                                                    value="${user.user}">
                                                                                <input
                                                                                    type="hidden"
                                                                                    name="password"
                                                                                    value="${user.password}">
                                                                                <button
                                                                                    class="btn btn-danger btn-sm"
                                                                                    type="submit">
                                                                                    🗑️
                                                                                </button>
                                                                            </form>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:forEach>
                                                </c:if>
                                                <%--< /c:forEach>--%>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                            <!-- Danh sách chia sẻ -->
                            <div class="card border-0 shadow-sm">
                                <div class="card-header text-center">
                                    <h5>Danh sách thư mục đang chia sẻ</h5>
                                </div>
                                <div class="card-body">
                                    <div class="table-responsive">
                                        <table class="table table-bordered table-hover">
                                            <thead class="table-light">
                                                <tr>
                                                    <th>Thư mục</th>
                                                    <th>Client</th>
                                                    <th>Quyền</th>
                                                    <th>Tùy chọn</th>
                                                    <th>Hủy chia sẻ</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:if test="${not empty nfsShares}">
                                                    <c:forEach var="share"
                                                               items="${nfsShares}">
                                                        <tr>
                                                            <td>
                                                                <c:set var="pathParts"
                                                                       value="${fn:split(share.path, '/')}" />
                                                                📁
                                                                ${pathParts[fn:length(pathParts)-1]}
                                                                <small
                                                                    class="text-muted d-block">${share.path}</small>
                                                            </td>
                                                            <td>${share.clients}</td>
                                                            <td>
                                                                <span
                                                                    class="badge ${share.permissions == 'rw' ? 'bg-success' : 'bg-warning'}">
                                                                    ${share.permissions}
                                                                </span>
                                                            </td>
                                                            <td>${share.options}</td>
                                                            <td>
                                                                <form action="configNFS"
                                                                      method="post"
                                                                      class="d-inline"
                                                                      onsubmit="return confirm('Bạn có chắc chắn muốn hủy chia sẻ thư mục ${pathParts[fn:length(pathParts)-1]} với ${share.clients} không?');">
                                                                    <input type="hidden"
                                                                           name="action"
                                                                           value="unshare">
                                                                    <input type="hidden"
                                                                           name="path"
                                                                           value="${share.path}">
                                                                    <input type="hidden"
                                                                           name="host"
                                                                           value="${user.host}">
                                                                    <input type="hidden"
                                                                           name="port"
                                                                           value="${user.port}">
                                                                    <input type="hidden"
                                                                           name="user"
                                                                           value="${user.user}">
                                                                    <input type="hidden"
                                                                           name="password"
                                                                           value="${user.password}">
                                                                    <input type="hidden"
                                                                           name="clientHost"
                                                                           value="${share.clients}">
                                                                    <button
                                                                        class="btn btn-danger btn-sm"
                                                                        type="submit"
                                                                        title="Hủy chia sẻ">
                                                                        <i
                                                                            class="fa-solid fa-ban"></i>
                                                                    </button>
                                                                </form>
                                                            </td>
                                                        </tr>
                                                    </c:forEach>
                                                </c:if>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <!-- Danh sách thư mục đang chia sẻ -->
                        <div class="card border-0 shadow-sm mt-4">
                            <div class="card-header text-center">
                                <h5>Danh sách thư mục đang được mount</h5>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-bordered table-hover">
                                        <thead class="table-light">
                                            <tr>
                                                <th>Nguồn</th>
                                                <th>Loại</th>
                                                <th>Dung lượng</th>
                                                <th>Đã dùng</th>
                                                <th>Còn trống</th>
                                                <th>Tỷ lệ</th>
                                                <th>Điểm mount</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="mount" items="${nfsMounts}">
                                                <tr>
                                                    <td>${mount.source}</td>
                                                    <td><span
                                                            class="badge bg-info">${mount.type}</span>
                                                    </td>
                                                    <td>${mount.size}</td>
                                                    <td>${mount.used}</td>
                                                    <td>${mount.available}</td>
                                                    <td>
                                                        <div class="progress"
                                                             style="height: 20px;">
                                                            <div class="progress-bar ${mount.usePercentage.replace('%','') >= 90 ? 'bg-danger' : mount.usePercentage.replace('%','') >= 70 ? 'bg-warning' : 'bg-success'}"
                                                                 role="progressbar"
                                                                 style="width: ${mount.usePercentage}"
                                                                 aria-valuenow="${fn:replace(mount.usePercentage,'%','')}"
                                                                 aria-valuemin="0"
                                                                 aria-valuemax="100">
                                                                ${mount.usePercentage}
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <i
                                                            class="fa-solid fa-folder-open me-1"></i>
                                                        ${mount.mountPoint}
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
        </div>

        <!-- Thông báo -->
        <div>
            <c:if test="${errmessage != null}">
                <div id="message" class="alert alert-danger alert-dismissible">
                    <button type="button" class="btn-close"
                            data-bs-dismiss="alert"></button>
                    <strong>Thất bại! </strong>
                    <c:out value="${errmessage}" />
                </div>
            </c:if>
            <c:if test="${message != null}">
                <div id="message" class="alert alert-success alert-dismissible">
                    <button type="button" class="btn-close"
                            data-bs-dismiss="alert"></button>
                    <strong>Thành công! </strong>
                    <c:out value="${message}" />
                </div>
            </c:if>
        </div>

        <script>
            setTimeout(function () {
                document.getElementById("message").style.display = "none";
            }, 10000);
        </script>

        <%}%>
        <script src="bootstrap-5.3.3-dist/js/bootstrap.bundle.min.js"></script>

        <!-- Modal Share NFS -->
        <div class="modal fade" id="shareModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Chia sẻ thư mục NFS</h5>
                        <button type="button" class="btn-close"
                                data-bs-dismiss="modal"></button>
                    </div>
                    <form action="configNFS" method="post">
                        <div class="modal-body">
                            <input type="hidden" name="action" value="share">
                            <input type="hidden" name="host" id="shareHost">
                            <input type="hidden" name="port" id="sharePort">
                            <input type="hidden" name="user" id="shareUser">
                            <input type="hidden" name="password" id="sharePassword">
                            <input type="hidden" name="path" id="sharePath">

                            <div class="mb-3">
                                <label class="form-label">Thư mục chia sẻ:</label>
                                <input type="text" class="form-control"
                                       id="displayPath" readonly>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Chọn máy client:</label>
                                <div class="list-group mb-3">
                                    <!-- Lấy danh sách máy từ session -->
                                    <% List<InforUser> connectedUsers = (List<InforUser>) session.getAttribute("users");
                                    %>
                                    <c:forEach var="clientUser"
                                               items="${users}">
                                        <!-- Không hiển thị máy chủ hiện tại trong danh sách client -->
                                        <c:if
                                            test="${clientUser.host ne user.host}">
                                            <label
                                                class="list-group-item d-flex justify-content-between align-items-center">
                                                <div>
                                                    <input
                                                        class="form-check-input me-2"
                                                        type="checkbox"
                                                        name="selectedClients"
                                                        value="${clientUser.host}">
                                                    <span>
                                                        <i
                                                            class="fa-solid fa-server me-1"></i>
                                                        ${clientUser.host}
                                                    </span>
                                                    <small
                                                        class="text-muted d-block ms-4">
                                                        <i
                                                            class="fa-solid fa-user me-1"></i>
                                                        ${clientUser.user}
                                                    </small>
                                                </div>
                                                <span
                                                    class="badge bg-primary rounded-pill">
                                                    <i
                                                        class="fa-solid fa-network-wired"></i>
                                                    Port: ${clientUser.port}
                                                </span>
                                            </label>
                                        </c:if>
                                    </c:forEach>
                                </div>

                                <!-- Thông báo khi không có client -->
                                <c:if test="${empty users or users.size() <= 1}">
                                    <div class="alert alert-warning">
                                        <i
                                            class="fa-solid fa-triangle-exclamation me-2"></i>
                                        Chưa có máy client nào được kết nối. Vui
                                        lòng kết nối thêm máy client.
                                    </div>
                                </c:if>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Hoặc nhập địa chỉ IP
                                    client:</label>
                                <input type="text" class="form-control"
                                       name="manualClient"
                                       placeholder="VD: 192.168.1.10, 192.168.1.11 hoặc 192.168.1.0/24">
                                <small class="text-muted">Nhập một hoặc nhiều địa
                                    chỉ IP, phân cách bằng dấu phẩy hoặc khoảng
                                    trắng</small>
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
                                    <input class="form-check-input" type="checkbox"
                                           name="options" value="sync" checked>
                                    <label class="form-check-label">sync (đồng
                                        bộ)</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox"
                                           name="options" value="no_root_squash" checked>
                                    <label class="form-check-label">no_root_squash
                                        (cho phép quyền root)</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox"
                                           name="options" value="no_subtree_check" checked>
                                    <label class="form-check-label">no_subtree_check
                                        (tắt kiểm tra thư mục con)</label>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary"
                                    data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-success">Chia
                                sẻ</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Script xử lý modal -->
        <script>
            function showShareModal(path, host, port, user, password) {
                document.getElementById('sharePath').value = path;
                document.getElementById('displayPath').value = path;
                document.getElementById('shareHost').value = host;
                document.getElementById('sharePort').value = port;
                document.getElementById('shareUser').value = user;
                document.getElementById('sharePassword').value = password;

                var modal = new bootstrap.Modal(document.getElementById('shareModal'));
                modal.show();
            }
        </script>

        <!-- Thêm modal tạo mới -->
        <div class="modal fade" id="createModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="createModalTitle">Tạo mới</h5>
                        <button type="button" class="btn-close"
                                data-bs-dismiss="modal"></button>
                    </div>
                    <form action="configNFS" method="post">
                        <div class="modal-body">
                            <input type="hidden" name="action" id="createAction">
                            <input type="hidden" name="currentPath"
                                   value="${currentPath}">
                            <input type="hidden" name="host" value="${user.host}">
                            <input type="hidden" name="port" value="${user.port}">
                            <input type="hidden" name="user" value="${user.user}">
                            <input type="hidden" name="password"
                                   value="${user.password}">

                            <div class="mb-3">
                                <label class="form-label">Thư mục hiện tại:
                                    ${currentPath}/</label>
                                <input type="text" class="form-control" name="name"
                                       required>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary"
                                    data-bs-dismiss="modal">Hủy</button>
                            <button type="submit"
                                    class="btn btn-success">Tạo</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Thêm script xử lý -->
        <script>
            function showCreateModal(type) {
                document.getElementById('createAction').value = type;
                document.getElementById('createModalTitle').textContent = type === 'folder' ? 'Tạo thư mục mới' : 'Tạo tập tin mới';
                var modal = new bootstrap.Modal(document.getElementById('createModal'));
                modal.show();
            }

            function submitUpload() {
                document.getElementById('uploadForm').submit();
            }
        </script>

    </body>

</html>