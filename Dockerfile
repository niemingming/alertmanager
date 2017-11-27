FROM hlht/centos7-jdk8
# !!重要!! 请替换下面的 {targetJarName} 为真实打包出的目标 jar 包
ADD alertmanager-0.0.1-SNAPSHOT.jar   /application.jar
WORKDIR /
EXPOSE 8080
CMD ["java", "-jar", "application.jar"]