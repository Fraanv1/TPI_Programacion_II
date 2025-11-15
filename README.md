🧩 Trabajo Final Integrador – Programación 2 (UTN)
Este proyecto corresponde al Trabajo Final Integrador (TFI) de la materia Programación 2 (Tecnicatura Universitaria en Programación – UTN).

Consiste en una aplicación de consola en Java que implementa los patrones DAO y Service, gestiona transacciones (commit/rollback) y se conecta a una base de datos MySQL mediante JDBC.
## 👥 Integrantes
* Desiderio Silva Lucas
* Gatti Leandro Agustin
* Vazquez Gabriel Franco
  

## 📖 Dominio Elegido
El dominio seleccionado para el TFI es:

**Usuario → CredencialAcceso**

Relación 1 a 1 unidireccional, donde:

* **Usuario (Clase A):** entidad principal que almacena la información general del usuario (username, email, activo, etc.) y mantiene una referencia directa a su credencial.
* **CredencialAcceso (Clase B):** entidad dependiente que almacena los datos sensibles de autenticación (hashPassword, salt, ultimoCambio), sin referencia de vuelta al usuario.

🔒 La relación 1→1 se garantiza en la base de datos mediante una clave foránea (`credencial_id`) en la tabla `usuarios` que, a su vez, posee una restricción `UNIQUE`.

## 🧱 Arquitectura y Tecnologías
El proyecto está estructurado en capas, asegurando la separación de responsabilidades:

| Capa | Descripción |
| :--- | :--- |
| `config/` | Configuración de la conexión a la base de datos. |
| `entities/` | Clases de dominio (Usuario, CredencialAcceso). |
| `dao/` | Implementación del patrón DAO. Acceso a datos con `PreparedStatement`. |
| `service/` | Lógica de negocio y gestión de transacciones (commit/rollback). |
| `main/` | Entrada principal del programa (AppMenu, MenuHandler). |

### 🔧 Tecnologías y Librerías
* **Lenguaje:** Java 21 (Recomendado por el TFI)
* **Base de Datos:** MySQL 8.0
* **Conector:** JDBC (mysql-connector-j-8.4.0.jar)
* **Patrones:** DAO, Service Layer, Inyección de Dependencias manual
* **Seguridad:** Hashing SHA-256 con Salt (clase `HashingUtil`)

## 📈 Modelo de Datos
```
╔═══════════════════╗             ╔════════════════════╗
║     usuarios      ║             ║  credencial_acceso  ║
╠═══════════════════╣             ╠════════════════════╣
║ id (PK)           ║◄────────────║ id (PK)             ║
║ username (UNIQUE) ║    (1 → 1)  ║ hashPassword        ║
║ email (UNIQUE)    ║             ║ salt                ║
║ activo            ║             ║ ultimoCambio        ║
║ fechaRegistro     ║             ║ requiereReset       ║
║ eliminado         ║             ║ eliminado           ║
║ credencial_id (FK, UNIQUE)      ║                     ║
╚═══════════════════╝             ╚════════════════════╝
```

## 🚀 Requisitos y Ejecución
### 📋 Requisitos previos
* JDK 21 instalado.
* MySQL Server 8.0 o superior.
* Un IDE Java (NetBeans, IntelliJ, Eclipse).
* Driver JDBC incluido en `/drivers/mysql-connector-j-8.4.0.jar`.

### 🗄️ 1. Configuración de la Base de Datos
1.  Abra MySQL Workbench o consola.
2.  Ejecute el script:
    ```sql
    SQL/3_definicion_tablas.sql
    ```
    Esto creará el schema `TP_Integrador_Programacion_II` con las tablas `usuarios` y `credencial_acceso`.
3.  (Opcional pero recomendado) Ejecute:
    ```sql
    SQL/3_carga_de_datos_de_prueba.sql
    ```
    para generar datos de prueba.

### 🔌 2. Configuración de la Conexión
1.  Edite el archivo:
    ```java
    src/config/DatabaseConnection.java
    ```
2.  y modifique los valores:
    ```java
    URL = "jdbc:mysql://localhost:3306/TP_Integrador_Programacion_II";
    USER = "tu_usuario";
    PASSWORD = "tu_contraseña";
    ```

### ▶️ 3. Compilar y Ejecutar
1.  Verifique que el driver MySQL esté agregado como librería del proyecto.
2.  Ejecute la clase:
    ```java
    src/main/Main.java
    ```
3.  La aplicación se iniciará en consola, mostrando el menú CRUD interactivo.

## 🎥 Video Demostración
Enlace a la presentación y demostración del equipo:

 [Aca va el link del video ]

## ✨ Características Destacadas
* **Arquitectura por capas:** Correcta separación de responsabilidades (DAO, Service, Main).
* **Gestión transaccional:** Uso de `commit/rollback` en la capa de Servicio para operaciones compuestas (ej. crear Usuario + Credencial).
* **Hashing seguro:** Las contraseñas se almacenan hasheadas (SHA-256) y con `salt`.
* **CRUD completo:** Implementación de Altas, Bajas (lógicas), Modificaciones y Listados para ambas entidades.
* **Seguridad:** Uso exclusivo de `PreparedStatement` en todas las consultas para prevenir Inyección SQL.
* **Scripts SQL reproducibles:** Se incluyen scripts para crear la estructura y cargar datos de prueba.
* **Código limpio y documentado:** El código sigue una estructura clara y está comentado.
