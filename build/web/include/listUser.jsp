<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="Model.InforUser, java.util.*" %> 
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Danh sách người dùng</title>
    </head>
    <body>
        <table border="1">
            <tr>
                <th>STT</th>
                <th>Tên người dùng</th>
                <th>Maintenance</th>
            </tr>
            <% 
                String list = (String) ss.getAttribute("ds");
                if (list != null) {
                    String[] lines = list.split("\n");
                    int i = 1;
                    for (String line : lines) {   
            %>
            <tr>
                <td><%= i++ %></td>
                <td><%= line %></td>
                <td>...</td>
            </tr>
            <%      
                    }
                }else
                {
            %>
            <tr>
                <td colspan="2">Không có dữ liệu hiển thị.</td>

            </tr>
            <%
                }
            %>
        </table>
    </body>
</html>
