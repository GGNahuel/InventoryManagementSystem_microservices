# Definición base v0.1
* App para gestionar inventarios con productos en cuentas de usuario
* La app permite registrarse o iniciar sesión
* Cada cuenta tiene un inventario general, pero a su vez se pueden crear otros inventarios si lo desean. Que en realidad son "filtrados" del inventario general según tags o labels
* Estas cuentas pueden crear sub-usuarios con roles y permisos para sus inventarios. Ejemplo: admin, general, jefe de x categoría, etc.
  * Por defecto ya vendría el "sub-usuario" de admin creado
* Los productos tienen sus propias categorías y propiedades, al igual que los inventarios

## Microservicios
### Productos
Api REST con su propia base de datos sql. Será REST por dos cuestiones principalmente: 1) porque no son muchos datos los que posee y 2) porque la idea es que **el cliente se comunique con el ms de inventarios directamente**, y sea ese el cual defina los datos que se mostrarán.

Tendrá 2 entidades:
* Productos
  * Id
  * Nombre
  * Marca
  * Modelo
  * Categoría (foreign key)
  * Descripción
  * Precio unitario
* Categoría
  * Id
  * Nombre

Tendrá un crud completo para los productos, y también para las categorías.

### Inventarios
Api con GraphQL también con su propia base de datos sql. Será con graphQL porque es la que se comunica con el cliente, salvo para cuestiones de autenticación o usuarios. Por lo que permitiría devolver solo los datos solicitados y no realizar ni over-fetching ni under-fetching.

**averiguar**, realizar una apiGateway para que solo exponga los endpoints de este micro servicio, *y el de usuarios*.

Entidad y DTOs
* Inventario
  * Id
  * Label
  * Cuenta asociada (Id de Cuenta)
  * Usuarios asociados (List<Usuario DTO\>)
  * Lista de DTOs de Productos
* Producto DTO
  * ...props del ms de productos
  * Stock
  * Disponibilidad
* Usuario DTO
  * Nombre
  * Rol
    * Nombre
    * Permisos (Enum)

Tendrá un crud solo para la entidad de inventario y la asignación de productos. Lo que refiere a la relación con cuenta y usuarios no será administrado en este micro servicio.

### Usuarios y cuentas
API REST con implementación de seguridad para autenticación con permisos y roles. 

Entidades
* Cuenta
  * id
  * username
  * password (encriptada)
  * Id de inventario asociado
  * Usuarios
* Usuario
  * Id
  * Nombre completo
  * password (encriptada)
  * Roles
    * Nombre
    * Permisos (Enum)