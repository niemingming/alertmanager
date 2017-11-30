FROM java
# !!重要!! 请替换下面的 {targetJarName} 为真实打包出的目标 jar 包
ADD alertmanager-1.0-SNAPSHOT.jar   /application.jar
WORKDIR /
#设置时区
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' >/etc/timezone \
EXPOSE 8080
CMD ["java", "-jar", "application.jar","--spring.profiles.active=prod"]