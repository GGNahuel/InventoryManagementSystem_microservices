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

### Estructura y fundamentación general
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

### Sobre el token
El mismo contiene los siguientes claims (pueden ser nulos depende de los logins o logouts que se hayan hecho):
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

### Permisos (los que estarían en el permissions dentro del userPerms)
- addProducts: El sub-usuario con este permiso está habilitado tanto a crear nuevos productos y agregarlos a un inventario, como a copiar de un inventario y pasarlo a otro.

- editProducts: El sub-usuario con este permiso puede editar la información principal de un producto en un inventario. Por ejemplo el nombre, marca, categorías, etc. 
- editProductReferences: El sub-usuario con este permiso puede editar directamente la referencia a un producto. Afectando así a todos los inventarios que contengan la misma referencia en alguno de sus productos.
  
- deleteProducts: El sub-usuario con este permiso puede borrar los productos en un inventario, pero la referencia a la información principal se mantiene si ese producto también se encuentra en otro inventario.
- deleteProductReferences: El sub-usuario con este permiso puede borrar no solo los productos en inventarios, sino también sus referencias. Haciendo que si otro inventario compartía la referencia en un producto ésta se elimine.
  
- editInventory: El sub-usuario con este permiso puede editar lo relacionado a las características de los productos dentro de un inventario, es decir el stock y disponibilidad.
 
> El **sub-usuario admin** incluye todos estos permisos y además puede
> - Crear, editar y eliminar sub-usuarios.
> - Asignar, editar y quitar permisos.
> - Crear y borrar inventarios.
> - Borrar la cuenta y todos los datos asociados a ella en las bases de datos.

Si se ordenaran de menor a mayor, según qué tanto las acciones que permiten esos permisos influyen a los datos de una cuenta, el resultado sería el siguiente:
1. editInventory
2. editProducts
3. addProducts, deleteProducts
4. editProductReferences, deleteProductReferences
5. permisos del admin

## Endpoints y operaciones
Todos los endpoints pueden ser accedidos a traves de la gateway. Ubicada en el localhost en el puerto 8080 (http://localhost:8080).

Las operaciones se dividen según el servicio y funcionalidad.

Tanto el servicio de usuarios como el de productos devuelven el mismo tipo de formato de respuesta. El cual es el siguiente:
```typescript
  interface DTO_respuestasGenerales {
    error: null | {
      message: string,
      cause: string,
      type: "warning" | "critical",
      exClass: string
    },
    data: null | object // esto contendría el objeto resultante de la operación en caso de que lo posea. También puede contener un mensaje adicional en caso de error.
  }
```
> Los retornos y cuerpos de las solicitudes en esta documentación se muestran en formato typescript para mostrar con mayor claridad y simpleza los posibles valores.

> Todas las id son en formato UUID de 16 bytes.

</br>

### Servicio de usuarios
**Administración de los datos de usuarios y sub-usuarios, manejo de autenticación y autorización.**
#### Posibles retornos
```typescript 
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
  - **Método HTTP**: POST
  - **Cuerpo requerido**: Todos los campos son obligatorios y las repeticiones de contraseña deben coincidir con la que corresponda para que el método funcione correctamente.
        
        {
          username: string,
          password: string,
          passwordRepeated: string,
          adminPassword: string,
          adminPasswordRepeated: string
        }

  - **Retorno esperado**: Status code 201

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
  - **Método HTTP**: GET
  - **Requerido**: la id dentro de la misma ruta.
  - **Retorno esperado**: Status code 200. Se espera un *DTO_cuenta* dentro del data de *DTO_respuestasGenerales*.
  
- **/account/add-user** Registra y asocia un nuevo sub-usuario a la cuenta especificada. 
  - **Método HTTP**: POST
  - **Requerido**:
    - Como cuerpo de la solicitud:

          {
            name: string,
            role: string,
            password: string,
            passwordRepeated: string,
            inventoryPerms: [DTO_permisosPorInventario] | null
          }

    - Cómo parámetro de la url se debe incluir ***accountId*** (string) de forma obligatoria, ya que si ésta no coincide con la id dentro del token mandado se negará la solicitud.****
  - **Retorno esperado**: Status code 200. *DTO-subUsuario* dentro del data de *DTO_respuestasGenerales*.
  - **Permisos**: Se requiere que sea el sub-usuario admin para poder realizar esta operación sobre la cuenta definida.
  
- **/account/delete** Borra una cuenta y todos los datos asociados a ella, incluido sus inventarios y sus productos.
  - **Método HTTP**: DELETE
  - *Parámetro requerido*: Se requiere la id de la cuenta a borrar dentro del parámetro de la url "id".
  - **Retorno esperado**: Status code 200.

        {
          error: null,
          data: "Cuenta eliminada con éxito"
        }

  - **Permisos**: Se requiere el sub-usuario admin.

- **/user/id/{id}** Busca en base de datos el sub-usuario con la id colocada en la misma ruta. (en el lugar de {id} iría la id del mismo).
  - **Método HTTP**: GET
  - **Requerido**: la id en la misma ruta y además el parámetro *accountId* para verificar que no se esté intentando acceder a un usuario fuera de la cuenta detallada en el token.
  - **Retorno esperado**: Status code 200. Objeto *DTO_subUsuario* dentro del data de *DTO_respuestasGenerales*.
  
- **/user/edit** Esta operación permite editar los atributos genéricos del usuario, como el nombre y el rol. Para editar los permisos se requiere acceder al endpoint correspondiente.
  - **Método HTTP**: PUT
  - **Requerido**: requiere un *DTO_subUser* con los campos, id, nombre y rol de forma obligatoria. Además el parámetro de url *accountId*.
  - **Retorno esperado**: Status code 200. Objeto *DTO_subUsuario* dentro del data de *DTO_respuestasGenerales*.
  - **Permisos**: Se requiere el sub-usuario admin.

- **/user/add-perm** Operación para agregar nuevos permisos a un sub-usuario ya existente.
  - **Método HTTP**: PATCH
  - **Requerido**: Además del *DTO_permisosPorInventario* como cuerpo de la solicitud, se debe enviar como parámetros de url tanto la id del sub-usuario, como *id*, y la de la cuenta a la que pertenece, como *accountId*.
  - **Retorno esperado**: Status code 200. Objeto *DTO_subUsuario* dentro del data de *DTO_respuestasGenerales*.
  - **Permisos**: Se requiere el sub-usuario admin.
  
- **/user/delete** Borra un usuario en base a su id. También internamente se borra la relación con los inventarios a los que estaba asociado.
  - **Método HTTP**: DELETE
  - **Requerido**: Se debe enviar como parámetros de url tanto la id del sub-usuario, como *id*, y la de la cuenta a la que pertenece, como *accountId*.
  - **Retorno esperado**: Status code 204.
  - **Permisos**: Se requiere el sub-usuario admin.

</br>

#### Autenticaciones. Logins y logouts
Todos estos endpoints usan el método HTTP POST y devuelven un *DTO_tokenJWT* con el token generado y status code 200 en caso de éxito.

En el caso de los que son para login se debe incluir el siguiente cuerpo:
```json
{
  "username": "nombre de usuario, ya sea el de la cuenta o la del sub-usuario según corresponda el login",
  "password": "contraseña de la cuenta o sub-usuario según corresponda"
}
```

- **/authenticate/login/account** Inicia sesión de la cuenta.
- **/authenticate/login/user** Inicia sesión con un sub-usuario. Requiere que primero haya iniciado sesión la cuenta. Solo puede haber un sub-usuario en la sesión.
- **/authenticate/logout/account** Cierra todas las sesiones activas, tanto la de la cuenta como la de sub-usuario.
- **/authenticate/logout/user** Cierra la sesión únicamente del sub-usuario pero permanece la de la cuenta.

</br>

### Servicio de productos
**Operaciones específicas a productos, sin importar el/los inventarios en donde se encuentren ni la info específica en ellos (stock y disponibilidad)**

- **/product/edit** Sobre-escribe los detalles del producto en su base de datos. Todos los inventarios que tengan referencia al producto seleccionado verán los cambios afectados en su contenido.
  - **Método HTTP**: PUT
  - **Requerido**:
    - *Cuerpo de la solicitud* un objeto con el siguiente formato. Se debe incluir incluso los datos que no fueron modificados.
      ```typescript
      interface DTO_producto {
        id: string,
        name: string,
        brand: string,
        model: string | undefined,
        description: string | undefined,
        unitPrice: number, // acepta decimales
        accountId: string,
        categories: [string] | undefined
      }
      ```
    - *Parámetros de url*: se debe indicar la id de la cuenta, la misma del token, en el parámetro *accountId*.
  - **Retorno esperado**: Status code 200. Y el *DTO_producto* reflejando los cambios.
  - **Permiso**: se requiere el permiso *editProductReferences*.

- **/product/delete**: Borra de la base de datos el producto seleccionado por si Id. Todos los inventarios que tengan referencia al producto seleccionado verán los cambios afectados en su contenido.
  - **Método HTTP**: DELETE
  - **Requerido**: el método funcionará solo si se encuentran los 2 siguientes parámetros en la url. *id*: id del producto a borrar, *accountId* id de la cuenta con la que se inició sesión.
  - **Retorno esperado**: Status code 204.
  - **Permisos**: Se requiere el permiso *deleteProductReferences*.

</br>

### Servicio de inventarios
**Encargado de administrar todo lo relacionado a inventarios y los productos dentro.**

Como este servicio es con GraphQl solo existe un endpoint al que se puede acceder <code>/graphql</code>, siendo una solicitud de método POST y con el cuerpo de ella en formato JSON. Estructura del cuerpo:
```json
{
  "query": "query de graphQL",
  "variables": {}, // propiedad opcional en caso de que se quieran usar variables en la query. Estas variables se nombran en la query con un "$" antes del nombre, y de forma normal en este objeto
  "operationName": "nombre de la operación a ejecutar" // propiedad opcional en caso de que se manden varias operaciones en la query
}
```
Ejemplos:

Ejemplo de una query para leer datos
```json
{
  "query": "query {
    getByAccount(accountId: \"00112233-0011-0011-...\") {
      id
      name
      products {
        refId
        name
        unitPrice
        stock
      }
    }
  }"
}
```
> Esta query por ejemplo obtendría todos los inventarios asociados a una cuenta a traves de la id ingresada como parámetro (notar que se escribe de forma literal dentro de la misma query) y devolvería, en este caso, una lista de inventarios con solamente los atributos de id, name y products. 
> 
> Como "products" también es un objeto **se debe** aclarar las propiedades que serán devueltas, en este caso solo refId, name, unitPrice y stock.

Ejemplo de una query para realizar cambios (mutations)
```json
{
  "query": "mutation ($productToAdd: ProductInput, $idOfInventory: ID) {
    addProduct(product: $productToAdd, invId: $idOfInventory) {
      refId
      name
      brand
      categories
      unitPrice
      stock
    }
  }",
  "variables": {
    "productToAdd": {
      "name": "Monitor",
      "brand": "Marca",
      "model": "ABC-123pro",
      "categories": ["electrónicos", "computación"],
      "unitPrice": 120.50,
      "stock": 6
    },
    "idOfInventory": "11223344-1122-1122-..."
  }
}
```
> En esta query se usan variables. Notar que éstas se definen en paréntesis después de nombrar el tipo de query (query / mutation). Luego de nombrarlas se define el tipo de la variable, ejemplo: String, Int, Boolean, [String], ID, o algún objeto personalizado como lo es *ProductInput*. Posteriormente se usan como argumentos en la operación *addProduct*.
>
> Finalmente, a estas variables, se le asigna su valor correspondiente dentro del objeto *variables* del cuerpo del JSON.

A continuación se detallan las operaciones que se pueden realizar en las queries. Para ver los objetos de retorno y de ingreso a ellas ver [los Types e Inputs](#types-e-inputs) respectivamente.

> Si se encuentra un signo de exclamación (**!**) después de la definición de un tipo significa que este es obligatorio. Tanto como en argumento de las operaciones como en las propiedades de los objetos que se pasan o se reciben.

De forma general un retorno se vería de la siguiente forma:
```json
{
  "data": {
    "nombreOperación": "retorno de la misma o null en caso de error"
  },
  "errors": [ // presente en caso de que haya errores únicamente
    {
      "message": "mensaje del error",
      "locations": [
        { "line": 1, "column": 1 }
      ],
      "path": ["nombreOperación con error"],
      "extensions": {} // información adicional
    }
  ]
}
```

#### Queries - operaciones de lectura
- **getById(id: ID!, accountId: ID!)**: Inventory
  
  Busca un inventario por su Id y devuelve el objeto con la información solicitada. En caso de no encontrar devuelve error.
- **getByAccount(accountId: ID!)**: [Inventory]
  
  Devuelve una lista de todos los inventarios asociados a una cuenta según su id.
- **searchProductsInInventories(
    name: String, 
    brand: String, 
    model: String, 
    categories: [String], 
    accountId: ID!
  )**: [Inventory]

  Devuelve una lista de los inventarios que tengan productos que coincidan con la búsqueda, filtrando también estos productos a solo los que concuerden con lo solicitado. Salvo el argumento de la id de la cuenta, el resto son opcionales, pueden ir vacíos o directamente no estar presentes

#### Mutations - operaciones de escritura
- **create(name: String!, accountId: ID!)**: Inventory
  
  Operación para crear inventarios. Se requiere que sea el sub-user admin quien ejecute esa operación. **name** refiere al nombre que se le asignará al inventario.
- **edit(invId: ID!, name: String!, accountId: ID!)**: Boolean
  
  Operación para editar inventarios. Se requiere que sea el sub-user admin quien ejecute esa operación.
- **delete(id: ID!, accountId: ID!)**: Boolean
  
  Operación para eliminar inventarios. Se requiere que sea el sub-user admin quien ejecute esa operación.

- **addProduct(product: ProductInput!, invId: ID!, accountId: ID!)**: ProductInInventory

  Agrega un producto al inventario seleccionado, incluyendo el stock.
- **editProductInInventory(product: EditProductInput!, invId: ID!, accountId: ID!)**: ProductInInventory

  Esta operación editará el producto seleccionado a través de su id de referencia. No edita el de referencia sino la copia que se encuentra en ese inventario. Es decir, si el producto seleccionado también se encuentra en otros inventarios, la edición solo se verá reflejada en el que pertenece. Esta edición no incluye el stock.

  *Si el producto seleccionado se encontraba también en otros inventarios, se creará un nuevo producto de referencia, anulando así la conexión de datos compartida entre los productos*.
- **copyProducts(products: [ProductToCopyInput]!, idTo: ID!, accountId: ID!)**: Boolean

  Copiará todos los productos seleccionados en base a su id de referencia al inventario de destino. También se deberá definir el nuevo stock para cada producto copiado.
  
  Los productos copiados compartirán referencia, lo que significa que si un sub-usuario con los permisos para editar o eliminar productos de referencia realiza alguna acción sobre ellos, aquellos copiados se verán también afectados.
- **editStockOfProduct(relativeNewStock: Int!, productRefId: ID!, invId: ID!, accountId: ID!)**: Boolean

  Método específico para cambiar el stock a través de un número relativo en el inventario seleccionado.

- **deleteProductsInInventory(productRefIds: [ID]!, invId: ID!, accountId: ID!)**: Boolean

  Operación para borrar uno o más productos solo en el inventario seleccionado.

#### Types e inputs
Estos son los objetos que se retornarían y se ingresarían respectivamente a las operaciones.

    type Inventory {
      id: ID
      name: String!
      accountId: ID
      usersIds: [ID]
      products: [ProductInInventory]
    }

    type ProductInInventory {
      refId: ID
      name: String!
      brand: String
      model: String
      description: String
      unitPrice: Int
      categories: [String]
      stock: Int
      isAvailable: Boolean
    }


    input ProductInput {
      name: String!
      brand: String!
      model: String
      description: String
      unitPrice: Int!
      categories: [String]
      stock: Int!
    }

    input EditProductInput {
      refId: ID!
      name: String!
      brand: String!
      model: String
      description: String
      unitPrice: Int!
      categories: [String]
    }

    input ProductToCopyInput {
      refId: ID!
      stock: Int!
    }

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