## Funcionalidades de la app
* La app sirve para gestionar inventarios con productos en cuentas de usuario

* Permite registrarse e iniciar sesión

* Cada cuenta puede tener varios inventarios
  * Un ejemplo de uso de la app sería: una cuenta representa una cadena de tiendas de ropa por ejemplo. Esta puede tener un inventario por cada sucursal que tenga. Entonces cada sucursal (inventario) tendrá su propia lista de productos. 

    Por cada producto en lista tendrá además info que solo se relaciona a esa sucursal (inventario), como el stock, disponibilidad y nombre de inventario (ej: abrigos, ropa deportiva, etc).

* Estas cuentas pueden crear sub-usuarios con roles y permisos para sus inventarios. Ejemplo: admin, general, jefe de x categoría, etc.
  * Por defecto ya viene el "sub-usuario" de admin creado

* Los productos tienen sus propias categorías y propiedades

* El cliente se comunicará principalmente con el micro-servicio de inventarios y usuarios, salvo para cosas específicas al de productos (como edición)

## Microservicios
### Productos
Api REST con su propia base de datos sql. Se encargará de solicitudes especificas a productos, como edición y eliminación

Tendrá 2 entidades:
* Productos
  * Id
  * Nombre
  * Marca
  * Modelo
  * Descripción
  * Precio unitario
  * Categorías (foreign key, many to many)
* Categoría
  * Id
  * Nombre

Tendrá un crud completo para los productos, y también para las categorías.

### Inventarios
Api con GraphQL también con su propia base de datos sql. Será con graphQL porque es la que se comunica con el cliente, *salvo para cuestiones de autenticación o usuarios, o cosas específicas de los productos*. Por lo que permitiría devolver solo los datos solicitados y no realizar ni over-fetching ni under-fetching.

Entidades y DTOs
* Inventario
  * Id
  * Cuenta asociada (Id de Cuenta, UUID - *MS usuarios*)
  * Lista de AssociatedUsers (One to many)
  * Lista de ProductsInInventory (Many to many)
* AssociatedUsers
  * Id
  * Id del usuario en *MS usuarios*
* ProductsInInventory
  * Id
  * Id del producto en *MS productos*
  * Stock
  * Disponibilidad
  
* Producto DTO
  * ...props del producto en *MS productos*
    * (Categorías como lista de strings con solo sus nombres)
  * Stock
  * Disponibilidad

Tendrá un crud solo para la entidad de inventario y la asignación de productos. Lo que refiere a la relación con cuenta y usuarios no será administrado en este micro servicio.

### Usuarios y cuentas
API REST con implementación de seguridad para autenticación con permisos y roles usando sesiones de usuario

Entidades
* Cuenta
  * id
  * username
  * password (encriptada)
  * Id de AssociatedInventories
  * Usuarios
* AssociatedInventories
  * Id
  * Id del inventario en *MS Inventarios*
* Usuario
  * Id
  * Nombre completo
  * password (encriptada)
  * Nombre del rol
  * Permisos por inventario (one to many)
* Permisos para inventario
  * id
  * associatedInventories (one to one)
  * Permisos (string formado de valores de un enum)

## Flujo de interacción entre micro-servicios
### Relacionado a usuarios
* ### Creación de cuenta
  Se llama al **ms de usuarios** y éste crea la cuenta. Por defecto creará también un usuario/sub-usuario con rol de admin
* ### Creación de usuario/sub-usuario para la cuenta
  Se llama directamente al **ms de usuarios** al método de asignación a la cuenta
* ### Asignación de permisos a sub-usuario en un inventario específico
  LLamando al **ms de usuarios**, pasándole la id del usuario y un dto que incluye la id del inventario a asociar, éste se encargará de crear los permisos según el dto. También llamará al **ms de inventarios** para sumar el usuario a la entidad
* ### Sesiones
  Todo lo que tenga que ver con sesiones, ya sea de la cuenta o del sub-usuario, o para obtenerla, iniciarla o cerrarla, se llamará al **ms de usuarios**
* ### Eliminación de cuenta o de usuarios
  Desde el **ms de usuarios** se invocarán estos métodos. Si se borra una cuenta, este ms se encargará de eliminar también los inventarios y productos asociados a esta llamando a los correspondientes **micro-servicios**. En cambio se se borra un usuario se llamará al **ms de inventarios** para quitar ese usuario de los inventarios asociados a él
### Relacionado a inventarios y productos
* ### Creación de inventario y asignación a cuenta
  Se llama al **ms de inventario** con el body del inventario a crear (nombre del mismo) y el id de la cuenta a asociar, la que está en sesión.
  
  Éste llamará al método de asignación de inventario en el **ms de usuarios**. El cuál retornará el DTO de la cuenta, de allí se extraerá la info de usuarios para asignarlos a la nueva entidad de Inventario
* ### Creación de productos nuevos en inventario
  Llamando al **ms de inventarios** se le pasa los datos del producto a agregar, la id del inventario y de la cuenta en sesión. El cuál asignará el producto al inventario asignado **revisando *antes*, a través de sus props, que este no haya sido registrado previamente en éste**. Y en caso de que no se haya registrado previamente llamará al **ms de productos** para hacer el registro en su respectiva BdD
* ### Asignación de productos ya creados y asignados en otro inventario
  En el **ms de inventarios** habrá un método al que se le pasa una lista de ProductoDTO y dos id de inventario. Una referenciando al inventario de donde se extraen los productos, y la otra al inventario en donde se asignarán. El ms se encargará de crear las entidades y relaciones necesarias.
* ### Edición de productos
  Se llama directamente al **ms de productos** pasando un dto del producto a editar
* ### Eliminación de inventario
  Cuando se elimina un inventario desde el **ms de inventarios**, se busca también aquellos productos que no están asociados a ningún otro de la misma cuenta. Posteriormente se llama al endpoint del **ms de productos** que se encarga de borrar varios elementos dadas ciertas ids. La búsqueda se hace a través de las referencias de id en el **ms de inventarios**
  
## Notas para versiones futuras
- Usar una apiGateway para exponer ciertos endpoints de cada micro-servicio según corresponda
- Usar JWT para la autenticación, autorización y comunicación entre micro-servicios
- Ver la posibilidad de usar base de datos no sql para micro-servicios de inventarios y productos