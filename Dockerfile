FROM maven:3.9.1-amazoncorretto-17-debian

RUN apt update && apt -y install locales && locale-gen en_US.UTF-8
RUN sed -i '/es_ES.UTF-8/s/^# //g' /etc/locale.gen && locale-gen
ENV LANG es_ES.UTF-8
ENV LANGUAGE es_ES:es
ENV LC_ALL es_ES.UTF-8

WORKDIR /home/kg/app
COPY ./ .
RUN mvn clean -DskipTests install

CMD ["java", "-jar", "./target/tracking_telegram-0.0.1-SNAPSHOT.jar"]