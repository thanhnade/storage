<%-- 
    Document   : background
    Created on : Nov 9, 2024, 12:56:22 PM
    Author     : acer
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>background</title>
        <style>
            .dropdown-color {
                width: auto;
                position: fixed;
                top: 120px;
                right: -20px;
            }
            .color-box {
                width: 20px;
                height: 20px;
                border-radius: 50%;
                border: 1px solid #ccc;
                margin-right: 10px;
                display: inline-block;
                cursor: pointer;
            }
            .dropdown-menu {
                min-width: auto; /* Giới hạn kích thước */
            }

            /* Tùy chỉnh nút dropdown */
            #dropdownMenuButton {
                background-color: #E95420; /* Màu cam */
                color: white; /* Màu chữ trắng */
                border: none; /* Bỏ viền */
                border-radius: 50%; /* Làm tròn nút */
                width: 50px;
                height: 50px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); /* Hiệu ứng đổ bóng */
                transition: all 0.3s ease;
            }

            #dropdownMenuButton:hover {
                background-color: #F46A36; /* Màu cam đậm hơn khi hover */
                box-shadow: 0 6px 8px rgba(0, 0, 0, 0.2);
            }

            /* Tùy chỉnh menu dropdown */
            .dropdown-menu {
                border-radius: 10px; /* Bo góc menu */
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); /* Hiệu ứng đổ bóng */
            }

            /* Tùy chỉnh hộp màu */
            .color-box {
                width: 30px; /* Tăng kích thước hộp màu */
                height: 30px;
                border-radius: 50%; /* Làm tròn hộp màu */
                border: 2px solid #fff; /* Viền trắng */
                margin-right: 10px;
                cursor: pointer;
                transition: transform 0.2s ease; /* Hiệu ứng phóng to khi hover */
            }

            .color-box:hover {
                transform: scale(1.2); /* Phóng to hộp màu khi hover */
                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2); /* Đổ bóng nhẹ */
            }
        </style>
    </head>
    <body>
        <div class="dropdown dropdown-color">
            <button 
                class="btn btn-secondary dropdown-toggle" 
                type="button" 
                id="dropdownMenuButton" 
                data-bs-toggle="dropdown" 
                aria-expanded="false">
                <i class="fa-solid fa-palette"></i>
            </button>
            <ul class="dropdown-menu text-center" aria-labelledby="dropdownMenuButton">
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #f5f5f5;" data-color="#f5f5f5"></span>
                    </a>
                </li>
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #FFF4E1;" data-color="#FFF4E1"></span> 
                    </a>
                </li>
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #F8F2E7;" data-color="#F8F2E7"></span> 
                    </a>
                </li>
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #E0F7FA;" data-color="#E0F7FA"></span> 
                    </a>
                </li>
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #FCE4EC;" data-color="#FCE4EC"></span> 
                    </a>
                </li>
                <li>
                    <a href="#" class="dropdown-item">
                        <span class="color-box" style="background-color: #D9D9D9;" data-color="#D9D9D9"></span> 
                    </a>
                </li>


            </ul>
        </div>


        <script>
            const dropdownItems = document.querySelectorAll('.dropdown-item');
            const body = document.body;

            // Khôi phục màu nền từ LocalStorage (nếu có)
            const savedColor = localStorage.getItem('backgroundColor');
            if (savedColor) {
                body.style.backgroundColor = savedColor; // Đặt màu nền từ LocalStorage
            }

            dropdownItems.forEach(item => {
                item.addEventListener('click', (event) => {
                    event.preventDefault(); // Ngăn chặn hành động mặc định khi nhấn vào link
                    const selectedColor = item.querySelector('.color-box').getAttribute('data-color');

                    // Cập nhật màu nền
                    body.style.backgroundColor = selectedColor;

                    // Lưu màu nền vào LocalStorage
                    localStorage.setItem('backgroundColor', selectedColor);
                });
            });
        </script>

    </body>
</html>
