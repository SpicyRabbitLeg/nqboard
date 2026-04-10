FROM registry.cn-hangzhou.aliyuncs.com/dockerhub_mirror/mysql-server:8.0.32

MAINTAINER lengleng(wangiegie@gmail.com)

ENV TZ=Asia/Shanghai

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY nqboard.sql /docker-entrypoint-initdb.d

COPY nqboard_config.sql /docker-entrypoint-initdb.d

COPY nqboard_device.sql /docker-entrypoint-initdb.d

COPY nqboard_seata.sql /docker-entrypoint-initdb.d

EXPOSE 3306
