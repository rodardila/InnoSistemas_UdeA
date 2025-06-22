# P4F1 – Gestión de Equipos de Desarrollo

Este proyecto forma parte de la actividad **Fábrica Escuela** de la **Universidad de Antioquia** y corresponde al **Proyecto 4 Feature 1 (P4F1)**.  
La aplicación fue desarrollada usando **Spring Boot** y está enfocada en el **registro de usuarios** y la **creación de equipos de desarrollo**.

## Características

- Registro de usuarios con roles definidos.
- Autenticación segura mediante JSON Web Tokens (JWT).
- Creación y gestión de equipos de desarrollo.
- Asignación de usuarios a equipos.
- Arquitectura monolítica modular, siguiendo principios de separación de responsabilidades.

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.3.11
- Spring Security (con JWT)
- Spring Data JPA
- PostgreSQL
- Maven

## Arquitectura

La aplicación está organizada bajo una **arquitectura monolítica modular**, donde cada módulo (usuarios, equipos, roles, etc.) contiene su propia estructura de:

- Entidades
- Controladores
- Servicios
- Repositorios
- DTOs

La comunicación entre los módulos se realiza internamente a través de invocaciones directas en memoria.

## Seguridad

El sistema implementa:

- **Autenticación** mediante JWT.
- **Autorización basada en roles**, protegiendo endpoints sensibles según el perfil del usuario.

Los tokens deben incluirse en el header `Authorization` como `Bearer <token>` para acceder a los recursos protegidos.

