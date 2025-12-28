-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Máy chủ: 127.0.0.1
-- Thời gian đã tạo: Th5 03, 2025 lúc 06:11 PM
-- Phiên bản máy phục vụ: 10.4.32-MariaDB
-- Phiên bản PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Cơ sở dữ liệu: `nlcs`
--

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `log_history`
--

CREATE TABLE `log_history` (
  `host` varchar(25) NOT NULL,
  `port` varchar(25) NOT NULL,
  `user` varchar(25) NOT NULL,
  `Enabled` int(1) NOT NULL DEFAULT 0,
  `Disabled` int(1) NOT NULL DEFAULT 0,
  `date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `nguoi_dung`
--

CREATE TABLE `nguoi_dung` (
  `tai_khoan` varchar(50) NOT NULL,
  `mat_khau` varchar(255) NOT NULL,
  `thu_muc` varchar(255) DEFAULT NULL,
  `role` varchar(10) DEFAULT 'client' CHECK (`role` in ('admin','client')),
  `storage_limit` bigint(20) DEFAULT 1073741824 COMMENT 'Giới hạn dung lượng cho người dùng (bytes), mặc định 1GB',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Đang đổ dữ liệu cho bảng `nguoi_dung`
--

INSERT INTO `nguoi_dung` (`tai_khoan`, `mat_khau`, `thu_muc`, `role`, `storage_limit`, `created_at`) VALUES
('admin', '1', '/data/b2104824/admin /data/hadoop/admin /data/nlcs/admin /data/admin1/admin /data/test/admin', 'admin', 1073741824, '2025-02-28 07:15:39'),
('demo', '1', '/data/test/demo /data/hadoop/demo /data/nlcs/demo /data/b2104824/demo /data/admin1/demo', 'admin', 1073741824, '2025-04-23 08:54:38'),
('lcn', '1', '/data/test/lcn /data/nlcs/lcn /data/b2104824/lcn /data/admin1/lcn', 'client', 1073741824, '2025-04-28 07:06:06'),
('user1', '1', '/data/hadoop/user1 /data/nlcs/user1 /data/admin1/user1 /data/b2104824/user1 /data/test/user1', 'client', 1073741824, '2025-02-28 07:01:46');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `path_on_server`
--

CREATE TABLE `path_on_server` (
  `tai_khoan` varchar(50) NOT NULL,
  `path_base` varchar(255) NOT NULL COMMENT 'Đường dẫn gốc trên server',
  `path_relative` varchar(255) NOT NULL COMMENT 'Đường dẫn tương đối do người dùng tạo',
  `isDirectory` tinyint(1) NOT NULL DEFAULT 0,
  `size` bigint(20) DEFAULT NULL COMMENT 'Kích thước tập tin',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Đang đổ dữ liệu cho bảng `path_on_server`
--

INSERT INTO `path_on_server` (`tai_khoan`, `path_base`, `path_relative`, `isDirectory`, `size`, `timestamp`) VALUES
('admin', '/data/hadoop/admin/', '.git', 1, 20637936, '2025-04-23 10:34:27'),
('admin', '/data/nlcs/admin/', 'FC ONLINE 2024-10-14 10-37-35.mp4', 0, 51557217, '2025-04-23 05:43:31'),
('admin', '/data/b2104824/admin/', 'hadoop', 1, 112475, '2025-04-18 10:34:59'),
('admin', '/data/nlcs/admin/', 'test_file', 1, 51561313, '2025-04-23 05:08:26'),
('demo', '/data/test/demo/', 'may_test', 1, 4096, '2025-04-28 06:59:07'),
('demo', '/data/test/demo/', 'ThucHanh', 1, 3976732, '2025-04-28 06:59:47'),
('lcn', '/data/b2104824/lcn/', 'baitap1', 1, 4096, '2025-04-28 07:13:20'),
('user1', '/data/hadoop/user1/', '1', 1, 288785, '2025-05-03 15:53:48'),
('user1', '/data/b2104824/user1/', 'Zalo Received Files', 1, 27646320, '2025-04-17 02:36:57');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `tai_khoan`
--

CREATE TABLE `tai_khoan` (
  `host` varchar(25) NOT NULL,
  `port` varchar(25) NOT NULL,
  `user` varchar(25) NOT NULL,
  `isEnabled` int(1) NOT NULL DEFAULT 0,
  `Disabled` int(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Đang đổ dữ liệu cho bảng `tai_khoan`
--

INSERT INTO `tai_khoan` (`host`, `port`, `user`, `isEnabled`, `Disabled`) VALUES
('10.13.130.60', '22', 'nlcs', 0, 1),
('10.13.130.61', '22', 'b2104824', 0, 1),
('10.13.130.62', '22', 'admin1', 0, 1),
('10.13.130.63', '22', 'test', 0, 1),
('172.18.215.245', '22', 'hadoop', 0, 1),
('192.168.1.62', '22', 'nlcs', 0, 1);

--
-- Chỉ mục cho các bảng đã đổ
--

--
-- Chỉ mục cho bảng `log_history`
--
ALTER TABLE `log_history`
  ADD KEY `host` (`host`);

--
-- Chỉ mục cho bảng `nguoi_dung`
--
ALTER TABLE `nguoi_dung`
  ADD PRIMARY KEY (`tai_khoan`),
  ADD UNIQUE KEY `tai_khoan` (`tai_khoan`),
  ADD KEY `tai_khoan_2` (`tai_khoan`),
  ADD KEY `tai_khoan_3` (`tai_khoan`);

--
-- Chỉ mục cho bảng `path_on_server`
--
ALTER TABLE `path_on_server`
  ADD PRIMARY KEY (`tai_khoan`,`path_relative`) USING BTREE;

--
-- Chỉ mục cho bảng `tai_khoan`
--
ALTER TABLE `tai_khoan`
  ADD PRIMARY KEY (`host`);

--
-- Các ràng buộc cho các bảng đã đổ
--

--
-- Các ràng buộc cho bảng `path_on_server`
--
ALTER TABLE `path_on_server`
  ADD CONSTRAINT `path_on_server_ibfk_1` FOREIGN KEY (`tai_khoan`) REFERENCES `nguoi_dung` (`tai_khoan`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
