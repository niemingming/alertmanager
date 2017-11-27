FROM java
# !!重要!! 请替换下面的 {targetJarName} 为真实打包出的目标 jar 包
ADD alertmanager-1.0-SNAPSHOT.jar   /application.jar
WORKDIR /
EXPOSE 8080
CMD ["java", "-jar", "application.jar","--spring.profiles.active=prod"]