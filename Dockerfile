FROM java:8u111-jdk
# !!重要!! 请替换下面的 {targetJarName} 为真实打包出的目标 jar 包
WORKDIR /
EXPOSE 8080
CMD ["java", "-jar", "alertmanager-0.0.1-SNAPSHOT.jar"]