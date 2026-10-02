## /src/main/resources/application-dev.properties

Database - localhost:3306/ticketsystem (Mariadb)

Default database user - ticketadmin

Default database password - secret

JDK version: 25

## Start backend server

In the folder of the project:

Windows:

    CMD

    mvnw.cmd clean install

    mvnw.cmd spring-boot:run

    PS

    .\mvnw.cmd clean install

    .\mvnw.cmd spring-boot:run

Linux:

    Bash

    ./mvnw clean install

    ./mvnw spring-boot:run