# API para gestión de inventarios de productos
#### Versión 1.0
Api diseñada para que usuarios puedan registrarse y crear distintos inventarios que pueden llenar con productos según lo requieran.

Trabaja con base de datos y seguridad a traves de autenticaciones y autorizaciones. Las cuentas creadas por defecto crean un sub-usuario *admin* que, además de poder crear inventarios, también puede registrar a otros sub-usuarios y otorgarle distintos permisos que pueden variar para cada inventario. Permisos como el de agregar productos, eliminarlos, editar el inventario, entre otros.

> #### Ejemplo de uso de la api: 
>
>  Una cuenta (usuario) representa una cadena de tiendas de ropa. Esta puede tener un inventario por cada sucursal que tenga. Así cada sucursal (inventario) tendrá su propia lista de productos. 
>
>  La información de los productos puede ser, o no ser, compartida entre las sucursales. Pero aún así cada una de estas tendrá info solo asociada a esa sucursal, como el stock de cada uno.

### Índice
- [Instrucciones de instalación y requisitos para ejecución local](#instrucciones-de-instalación-y-requisitos-para-ejecución-local)
- [Autenticación y autorización](#autenticación-y-autorización)

### Casos de uso, funcionalidades
* En primera instancia los usuarios pueden registrar una cuenta y posteriormente iniciar sesión en la API.

* Al registrarla se pide, además de los datos de ingreso de la cuenta, los datos que se usarían para el inicio de sesión del sub-usuario admin.

* Los usuarios pueden crear en su cuenta distintos inventarios y llenar cada uno con productos según lo desee.

* Las cuentas pueden tener sub-usuarios con roles y permisos que servirán para sus inventarios. Ejemplo: admin, general, jefe de x categoría, etc.
  * Por defecto ya viene el sub-usuario de admin creado.
  
* Cada sub-usuario tiene su propio nombre de usuario y contraseña, usados para iniciar sesión en ellos. 

* Si solo se inicia sesión de usuario, pero no de sub-usuario, solo se podrán ver los inventarios y su contenido.

* El sub-usuario admin es el encargado de crear, nombrar y eliminar estos inventarios si lo desea.

* También este admin es quien crea los sub-usuarios asignándoles sus atributos. Como el nombre, la contraseña, y los permisos para cada inventario.

* Finalmente, como admin, también puede hacer todo lo que un sub-usuario podría hacer a través de sus permisos: agregar productos, editarlos, eliminarlos y editar los inventarios.

* Los permisos de cada sub-usuario se asignan por inventario, lo que significa que un sub-usuario puede tener ciertos permisos para uno pero no tenerlos para otro, o tener distintos.

* Los productos registrados en un inventario pueden ser copiados, con sus propiedades, a otro dentro de la misma cuenta. En el inventario al que se copian los productos se debe asignar para cada uno de ellos atributos que corresponden solo a ese inventario, como por ejemplo el stock y disponibilidad.

* Los productos pueden buscarse según sus atributos, como el nombre, marca, stock, categoría y/o disponibilidad.

* El cliente se comunicará con la API Gateway, la cual redirigirá la solicitud al servicio que corresponda.

### Tecnologías y herramientas aplicadas

## Instrucciones de instalación y requisitos para ejecución local
Todos los servicios de la API están envueltos en contenedores de Docker y orquestados mediante él mismo a través de docker compose. Esto simplifica mucho las acciones necesarias para poder levantar los servicios. Aún así se puede hacerlo sin Docker.

### Variables de entorno
Ya sea se ejecute con o sin docker es necesario configurar las variables de entorno. Las mismas son las siguientes. (Respetar el uso de mayúsculas cuando es necesario)

- **MYSQL_ROOT_PASSWORD**: la cuál contiene la contraseña que se asignará al usuario root en la imagen de MySql. Ésta variable es solo necesaria si se usa Docker.
- **MYSQL_USERNAME**: variable usada por cada servicio para poder acceder a MySql.
- **MYSQL_PASSWORD**: variable usada por cada servicio para poder acceder a MySql.
- **MYSQL_URL_USERS**: url a la base de datos que usará el servicio de usuarios.
- **MYSQL_URL_PRODUCTS**: url a la base de datos que usará el servicio de productos.
- **MYSQL_URL_INVENTORIES**: url a la base de datos que usará el servicio de inventarios.
- **jwt_key**: valor de la llave con la que se firmarán y validarán los tokens.

> ***La llave jwt debe ser creada con el algoritmo HS256.***
Puede generarse mediante el comando <code>openssl rand -base64 32</code>. O bien se puede usar la que se muestra como ejemplo a continuación.

#### Ejemplo de valores
**ACLARACIÓN:** Los hosts de las urls de las bases de datos (<code>mysql-database</code>) corresponden al nombre del contenedor de docker, en caso de no usar docker se debería reemplazar eso por <code>localhost</code> y usar el puerto configurado (por defecto 3306).

    MYSQL_ROOT_PASSWORD=root
    MYSQL_USERNAME=root
    MYSQL_PASSWORD=root
    MYSQL_URL_USERS=jdbc:mysql://mysql-database:3306/users?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=GMT&characterEncoding=UTF-8
    MYSQL_URL_PRODUCTS=jdbc:mysql://mysql-database:3306/products?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=GMT&characterEncoding=UTF-8
    MYSQL_URL_INVENTORIES=jdbc:mysql://mysql-database:3306/inventories?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=GMT&characterEncoding=UTF-8

    jwt_key="ExampleSecretKeyForJWT1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ="

### En caso de usar docker
Para empezar es necesario configurar las [**variables de entorno**](#variables-de-entorno). Para esto se crea un archivo .env en la carpeta raíz del proyecto (a la misma altura que se encuentra el archivo docker-compose.yml).

Teniendo docker en el sistema, y ejecutándose, alcanza con el siguiente comando en la ubicación raíz del proyecto. Esto empaquetará los servicios y ejecutará los archivos .jar de cada uno.

    #para la primera vez que se quiere levantar la API
    docker compose up --build  

    #para las siguientes
    docker compose up 

Adicionalmente se puede levantar la API con el perfil de desarrollo. Además de trabajar sin empaquetar la aplicación también, permite ver en tiempo real los cambios que se realicen gracias a spring boot devtools. Los comandos para esto son:

    #para la primera vez que se quiere levantar la API
    docker compose -f docker-compose-dev.yml up --build

    #para las siguientes
    docker compose -f docker-compose-dev.yml up
    
### En caso de no usar docker
Si se quiere levantar la api sin docker es necesario contar con los siguientes recursos:
- Este proyecto requiere instalar y tener correctamente configuradas las variables de entorno para **Java JDK** y **Maven**, tanto en Windows como en sistemas basados en Unix (Linux/macOS).
  #### Windows
  Se debe agregar al `Path` del sistema (Variable de entorno) las rutas a:

  - El bin del **JDK**, en el proyecto se usa la versión 17 (ejemplo: `C:\Program Files\Java\jdk-17\bin`)
  - El bin de **Maven** (ejemplo: `C:\apache-maven-3.9.6\bin`)

  Para esto buscar la opción "Editar las variables de entorno" en el panel de control y hacer click en el botón "Variables de entorno...", ubicado en la pestaña Opciones avanzadas.

  #### Linux / macOS
  En sistemas Unix-like, es necesario agregar las siguientes líneas al archivo de configuración del shell:

      export JAVA_HOME=/ruta/a/tu/jdk
      export PATH=$JAVA_HOME/bin:$PATH

      export MAVEN_HOME=/ruta/a/apache-maven
      export PATH=$MAVEN_HOME/bin:$PATH

  Estos cambios deben agregarse en uno de los siguientes archivos, según el shell que se utilice en el sistema:

  | Shell |	Archivo de configuración |
  | ---- | ---- |
  | Bash |	~/.bashrc o ~/.bash_profile |
  | Zsh (macOS) |	~/.zshrc |
  | Fish | ~/.config/fish/config.fish |

  Después de editarlos, aplicá los cambios con: <code>source ~/.bashrc</code> o <code>~/.zshrc</code> según corresponda

  #### Se puede verificar las configuraciones en una terminal (CMD o PowerShell) ejecutando:
      java -version
      javac -version
      mvn -version

- Tener instalado el server de MySql, al menos la versión 8.0. Los valores que se asignen a la cuenta serán los que deberán ir en las [variables de entorno](#variables-de-entorno).

Posteriormente con las variables de entorno del proyecto se pueden definir como se mencionó anteriormente (pero no en el Path), en el IDE si este lo permite, o directamente en los application.properties de cada servicio como se muestra en el [ejemplo](#ejemplo-de-valores).

Finalmente se debe iniciar cada proyecto de spring ubicado en las carpetas correspondientes a los microservicios.

## Autenticación y autorización
Salvo para los endpoints que refieren al registro e inicio de sesión de una cuenta, **el resto de endpoints requieren autenticación mediante JWT en el header correspondiente**. Éste se obtiene una vez hecho un login exitoso, y se modifica si se hace el login de sub-usuario o algún logout.

Estos métodos devuelven un objeto con una propiedad llamada token de tipo string dentro del body de la respuesta. El valor de la misma es el valor del token. Ejemplo:

```json
{
  "token": "tokenJWTGenerado"
}
```

El valor del mismo se debe incluir en las solicitudes siguientes dentro de los headers de cada request. Específicamente dentro del header de autorización "Bearer". Debería verse así:

    Bearer tokenJWTGenerado

**El espacio entre "Bearer" y el token es importante que se encuentre, caso contrario no será reconocido.**

El token contiene los siguientes claims (pueden ser nulos depende de los logins o logouts que se hayan hecho):
```json
{
  "sub": "nombre de usuario de la cuenta",
  "accountId": "id de la cuenta en formato UUID", // se debe incluir en algunos métodos para poder autorizarlos
  "userName": "nombre de sub-usuario en sesión",
  "userRole": "rol del sub-usuario en sesión",
  "isAdmin": "valor booleano que determina si el sub-usuario es admin o no",
  "userPerms": [
    {
      "idOfInventoryReferenced": "id del inventario en su respectiva base de datos",
      "permissions": ["permiso", "permiso"] // lista de permisos asociados a ese inventario
    }
  ], // esta lista puede estar vacía (o nula)
  "iat": 00000001, // fecha de emisión en ms
  "eat": 00000001 // fecha de expiración en ms
}
```

## Endpoints y operaciones
Todos los endpoints pueden ser accedidos a traves de la gateway. Ubicada en el localhost en el puerto 8080 (http://localhost:8080).

Las operaciones se dividen según el servicio y funcionalidad. 

### Servicio de usuarios
**Administración de los datos de usuarios y sub-usuarios, manejo de autenticación y autorización.**
#### Posibles retornos (en formato typescript para mayor aclaración de los posibles valores)
```typescript
  interface DTO_respuestasGenerales {
    error: null | {
      message: string,
      cause: string,
      type: "warning" | "critical",
      exClass: string
    },
    data: null | object
  }
  
  interface DTO_cuenta {
    id: string,
    username: string,
    inventoryReferenceIds: [string] | null,
    users: [DTO_subUsuario] | null
  }

  interface DTO_subUsuario {
    id: string,
    name: string,
    role: string,
    inventoryPerms: null | [DTO_permisosPorInventario]
  }

  interface DTO_permisosPorInventario {
    permissions: ["addProducts" | "editProducts" | "deleteProducts" | "editInventory"],
    idOfInventoryReferenced: string
  }

  interface DTO_tokenJWT {
    token: string
  }
```

#### Cuentas y sub-usuarios
- **/account/register** Registra cuenta de usuario creando y asignando también el sub-usuario admin. **No requiere token**
  - Método HTTP: POST
  - *Cuerpo requerido*: Todos los campos son obligatorios y las repeticiones de contraseña deben coincidir con la que corresponda para que el método funcione correctamente.
        
        {
          username: string,
          password: string,
          passwordRepeated: string,
          adminPassword: string,
          adminPasswordRepeated: string
        }

  - *Retorno esperado*: Status code 201

        {
          error: null
          data: {
            id: string,
            username: string,
            inventoryReferenceIds: null,
            users: [
              id: string,
              name: "admin",
              role: "admin",
              inventoryPerms: null
            ]
          }
        }

- **/account/id/{id}** Busca en base de datos la cuenta con la id colocada en la misma ruta. (en el lugar de {id} iría la id de la cuenta que se quiere buscar).
  - Método HTTP: GET
  - *Requerido*: la id dentro de la misma ruta.
  - *Retorno esperado*: Status code 200. Se espera un *DTO_cuenta* dentro del data de *DTO_respuestasGenerales*.
  
- **/account/add-user** Registra y asocia un nuevo sub-usuario a la cuenta especificada. 
  - Método HTTP: POST
  - *Requerido*:
    - Como cuerpo de la solicitud:

          {
            name: string,
            role: string,
            password: string,
            passwordRepeated: string,
            inventoryPerms: [DTO_permisosPorInventario] | null
          }

    - Cómo parámetro de la url se debe incluir ***accountId*** (string) de forma obligatoria, ya que si ésta no coincide con la id dentro del token mandado se negará la solicitud.****
  - *Retorno esperado*: Status code 200. *DTO-subUsuario* dentro del data de *DTO_respuestasGenerales*.
  - *Permisos*: Se requiere que sea el sub-usuario admin para poder realizar esta operación sobre la cuenta definida.
  
- **/account/delete** Borra una cuenta y todos los datos asociados a ella, incluido sus inventarios y sus productos.
  - Método HTTP: DELETE
  - *Parámetro requerido*: Se requiere la id de la cuenta a borrar dentro del parámetro de la url "id".
  - *Respuesta esperada*: Status code 200.

        {
          error: null,
          data: "Cuenta eliminada con éxito"
        }

  - *Permisos*: Se requiere el sub-usuario admin.

- 

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