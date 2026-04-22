FROM registry.cn-hangzhou.aliyuncs.com/dockerhub_mirror/java:21-anolis

WORKDIR /nqboard-device-biz

ARG JAR_FILE=target/nqboard-device-biz.jar

COPY ${JAR_FILE} app.jar

EXPOSE 6000

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx256m -Djava.security.egd=file:/dev/./urandom"

CMD sleep 60; java $JAVA_OPTS -jar app.jar
