<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser" %> 
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Danh sách dịch vụ</title>
        <link rel="stylesheet" href="css/listService.css"/>
        <link rel="shortcut icon" type="image/png" href="img/logo1.png"/>
        <style>
            .table-container {
                width: 60%; /* Hoặc kích thước cụ thể như 600px */
                max-height: 450px; /* Kích thước tối đa của bảng */
                overflow-y: auto; /* Thêm thanh cuộn dọc nếu cần */
            }

            table {
                width: 100%;
                border-collapse: collapse;
            }

            th, td {
                border: 1px solid #ddd;
                padding: 8px;
            }

            th {
                background-color: #f2f2f2;
            }
        </style>
    </head>
    <body>
        <%
            InforUser u = (InforUser)session.getAttribute("user");
            HttpSession ss = request.getSession();
               if ( ss == null || u == null) {
                   response.sendRedirect("./Connect.jsp");
               }
               else{

        %>
        <%@include file="include/header.jsp" %>
        <%@include file="include/dieuhuong.jsp" %>
        <h2>Danh sách phần mềm đã cài đặt </h2>
        <div>
            <form action="listService" method="post">
                <button type="submit" name="action" value="show">Show</button>
                <button type="submit" name="action" value="reset">Reset</button>
                <input type="hidden" name="host" value="<%=u.getHost()%>">
                <input type="hidden" name="port" value="<%=u.getPort()%>">
                <input type="hidden" name="user" value="<%=u.getUser()%>">
                <input type="hidden" name="password" value="<%=u.getPassword()%>">
            </form> 
        </div>
        <div class="table-container">
            <table border="1">
                <tr>
                    <th>STT</th>
                    <th>Tên dịch vụ</th>
                    <th>Phiên bản</th>
                </tr>
                <%
                String listService = (String) request.getAttribute("listService");
                if (listService != null) {
                    String[] lines = listService.split("\n");
                    int i = 1;
                    for (String line : lines) {
                        String[] colums = line.split(" ");
                        if (colums.length == 2) {
                %>
                <tr>
                    <td><%= i++ %></td>
                    <td><%= colums[0] %></td>
                    <td><%= colums[1] %></td>
                </tr>
                <%
                            }
                        }
                    }else
                    {
                %>
                <tr>
                    <td colspan="3">Không có dữ liệu để hiển thị.</td>
                </tr>
                <%
                    }
                %>
            </table>
        </div>

        <% 
            } 
        %>

    </body>
</html>
