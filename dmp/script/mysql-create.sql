CREATE DATABASE `dmp`;
CREATE TABLE `p_c_quantity` (
  `data_date` date NOT NULL,
  `province` VARCHAR(40),
  `city` VARCHAR(40),
  `countz` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `area_ad_req` (
  `data_date` date NOT NULL,
  `province` VARCHAR(40),
  `city` VARCHAR(40),
  `orginal_req` bigint(20) DEFAULT NULL,
  `valid_req` bigint(20) DEFAULT NULL,
  `ad_req` bigint(20) DEFAULT NULL,
  `tpi_bid_num` bigint(20) DEFAULT NULL,
  `win_bid_num` bigint(20) DEFAULT NULL,
  `show_ad_master_num` bigint(20) DEFAULT NULL,
  `click_ad_master_num` bigint(20) DEFAULT NULL,
  `show_ad_media_num` bigint(20) DEFAULT NULL,
  `click_ad_media_num` bigint(20) DEFAULT NULL,
  `dsp_ad_xf` double DEFAULT NULL,
  `dsp_ad_cost` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
