FROM public.ecr.aws/docker/library/eclipse-temurin:25-jre

WORKDIR /app

COPY target/*.jar /app/app.jar

EXPOSE 8080

CMD [ "java", "-jar", "app.jar" ]