# Ejecución del servicio y sus pruebas

Esta guía explica cómo configurar las variables de entorno desde **PowerShell**, iniciar el microservicio Spring Boot y ejecutar individualmente las pruebas de registro, persistencia y controlador HTTP.

> Todos los comandos se ejecutan desde `backend/users-service`, donde se encuentran `pom.xml` y `mvnw.cmd`.

## 1. Requisitos

- JDK compatible con el proyecto (Java 21 o superior; el `pom.xml` configura Java 21).
- PowerShell y Maven Wrapper (`mvnw.cmd`, incluido en el repositorio).
- Para la prueba de repositorio: PostgreSQL accesible desde Windows y una **base exclusiva para pruebas**, `BDAuth_test`.

No es necesario instalar Maven globalmente ni iniciar el servidor Spring Boot para ejecutar las pruebas.

## 2. Configurar variables de entorno en PowerShell

Las variables definidas con `$env:` existen **solo en la terminal actual** y sus procesos hijos. Si abres otra terminal, debes definirlas nuevamente. No se guardan de forma permanente en Windows.

### 2.1. Variables para iniciar el servicio

Sustituye los valores de ejemplo por los de tu entorno:

```powershell
$env:DB_URL = "jdbc:postgresql://192.168.33.17:5432/BDAuth"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "TU_PASSWORD"

$env:MAIL_USERNAME = "TU_CORREO"
$env:MAIL_PASSWORD = "TU_PASSWORD_DE_APLICACION"
```

**Importante:** los nombres exactos de las variables de base de datos y correo deben coincidir con los marcadores `${...}` utilizados en `src/main/resources/application.properties`. Si el proyecto utiliza nombres distintos, conserva los de ese archivo. No subas credenciales reales al repositorio.

Para establecer una clave JWT temporal en la sesión actual:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

La clave se genera de nuevo cada vez que ejecutas el bloque. Para iniciar el servicio, verifica además los nombres de las propiedades JWT requeridas por tu configuración y define las variables correspondientes.

### 2.2. Configuración exclusiva de integración

La prueba `UserRepositoryIntegrationTest` utiliza el perfil `integration`, que debe tener su configuración en:

```text
src/test/resources/application-integration.properties
```

Comprueba que apunte a **`BDAuth_test`**, nunca a `BDAuth`:

```properties
spring.datasource.url=jdbc:postgresql://192.168.33.17:5432/BDAuth_test
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=noreply@example.com
spring.mail.password=not-used-in-this-test
```

Si usas `${DB_PASSWORD}`, define `$env:DB_PASSWORD` en **la misma terminal** desde la que ejecutarás la prueba. Ajusta host, usuario y puerto a tu instalación. El servidor SMTP solo se configura para permitir que arranque el contexto: esta prueba de repositorio **no envía correos**.

> **Precaución:** `create-drop` elimina y vuelve a crear las tablas al iniciar el contexto de prueba y las elimina al cerrarlo. No ejecutes este perfil contra una base con información que debas conservar.

## 3. Ejecutar las pruebas

Desde PowerShell, ubícate en el directorio del microservicio:

```powershell
cd C:\workspace\USI\books-music-platform\backend\users-service
```

### 3.1. Prueba unitaria: `RegisterServiceImplTest`

```powershell
.\mvnw.cmd -Dtest=RegisterServiceImplTest test
```

Comprueba la lógica de registro con **JUnit y Mockito**. Las dependencias, como los repositorios y el servicio de correo, se simulan mediante mocks. **No requiere PostgreSQL ni iniciar Spring Boot.**

### 3.2. Prueba de integración: `UserRepositoryIntegrationTest`

```powershell
.\mvnw.cmd -Dtest=UserRepositoryIntegrationTest test
```

Comprueba la persistencia y consulta de usuarios mediante **Spring Data JPA y PostgreSQL real**. Requiere que PostgreSQL esté disponible, que exista `BDAuth_test` y que las credenciales y demás propiedades necesarias para iniciar el contexto sean válidas.

Durante la ejecución pueden aparecer sentencias SQL como `CREATE TABLE`, `INSERT`, `SELECT` y `DROP TABLE`. Con `create-drop`, el borrado de tablas **es esperado**, pero únicamente debe ocurrir en la base de pruebas.

### 3.3. Prueba del endpoint: `RegisterControllerTest`

```powershell
.\mvnw.cmd -Dtest=RegisterControllerTest test
```

Envía una petición HTTP simulada al controlador mediante **MockMvc** y verifica la respuesta. El servicio de registro se sustituye por un mock (`@MockitoBean`), por lo que **no necesita un servidor HTTP levantado ni acceder a PostgreSQL**. La petición se procesa dentro del contexto de prueba de Spring MVC.

### Resumen

| Prueba | Qué comprueba | ¿PostgreSQL? | ¿Levantar el servidor? |
|---|---|---|---|
| `RegisterServiceImplTest` | Lógica del registro con Mockito | No | No |
| `UserRepositoryIntegrationTest` | Persistencia real con JPA | **Sí** | No |
| `RegisterControllerTest` | Petición y respuesta HTTP con MockMvc | No | No |

> Para ejecutar **solo** la prueba indicada, conserva `-Dtest=NombreDeLaClase`. Si ejecutas `test` sin ese filtro, Maven intentará correr también otras pruebas existentes, como `UsersServiceApplicationTests`, que pueden necesitar configuración adicional.

## 4. Iniciar Spring Boot (opcional)

Si deseas ejecutar el microservicio para probarlo manualmente, primero configura en esa terminal las variables de entorno que utiliza `src/main/resources/application.properties`. Después ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

El proceso permanece activo hasta que lo detengas con **Ctrl + C**. Usa otra terminal para enviar peticiones o ejecutar Maven; recuerda que las variables `$env:` **no se comparten automáticamente** entre terminales.

**No es necesario ejecutar `spring-boot:run` antes de ninguna de las tres pruebas.**

## 5. Interpretar los resultados

Al terminar, Maven muestra un resumen similar a:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **`Failures`**: una aserción no produjo el resultado esperado.
- **`Errors`**: ocurrió una excepción inesperada, por ejemplo al configurar el contexto o conectar con PostgreSQL.
- **`BUILD SUCCESS`**: las pruebas seleccionadas finalizaron satisfactoriamente.

Si falla la prueba de integración, revisa primero la conexión a `BDAuth_test` y las propiedades del perfil `integration`. Un error al crear un componente de Spring no implica necesariamente que haya fallado la lógica de persistencia.
