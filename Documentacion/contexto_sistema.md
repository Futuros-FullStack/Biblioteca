# Contexto del Sistema — Biblioteca Facultad de Negocios

> Documento de contexto arquitectónico y de negocio. Es la base para los diagramas y el código que se generen después.
> **Aún no contiene código.** El modelo de datos vigente es `mer-final.drawio` (es el que se usa en el proyecto); la §3 lo describe y la §4 documenta las correcciones que se le aplicaron.

---

## 0. Panorama general

| Aspecto | Descripción |
|---|---|
| **Proyecto** | Sistema de Trazabilidad y Administración Bibliotecaria (PV-BIB-001) |
| **Cliente** | Biblioteca de la Facultad de Negocios, Campus IV — UNACH (Tapachula, Chiapas) |
| **Tipo de aplicación** | Aplicación de escritorio en **JavaFX** con una **base de datos relacional** |
| **Problema** | Los estudiantes no saben si un libro está disponible ni dónde está (pasillo, estante y fila). Los préstamos y devoluciones se anotan a mano, lo que es lento, provoca errores y hace que el registro no coincida con lo que hay en los estantes. |
| **Objetivo** | Consultar al momento la disponibilidad y la ubicación de cada ejemplar, y registrar préstamos y devoluciones de forma ágil y confiable. |

---

## 1. Control de acceso por rol

El sistema arranca con un selector de rol que tiene dos opciones: **Estudiante** y **Administrador**.

| Aspecto | Estudiante | Administrador |
|---|---|---|
| Credenciales | **Ninguna.** Entra directo. | **Correo** y **Password** |
| ¿Tiene cuenta? | **No.** No se crean cuentas de estudiante. | Sí. Es un registro de `Usuario` con rol Administrador. |
| Módulos | Solo **Libros**, en modo consulta | **Libros**, **Préstamo** e **Inventario** |
| Qué puede hacer | Buscar libros por título, autor o ISBN, y ver ISBN, editorial, año, ubicación (pasillo, estante y fila) y ejemplares disponibles | Todo: alta de libros y ejemplares, préstamos, devoluciones y consulta del inventario |
| ¿Solicita préstamos? | **Nunca** | Es **quien captura y registra** el préstamo |

> El acceso como Estudiante **no crea ni consulta** ningún registro de `Usuario`. Es una sesión anónima de solo lectura.

---

## 2. Flujo de negocio

### 2.1 Libros (catálogo y alta)
1. **Agregar libro:** se capturan Título, Autores, ISBN, Editorial, Año de publicación, la Ubicación de los ejemplares y cuántos ejemplares registrar.
   - Se crea el registro en `Libros`.
   - Se asocian sus `Autores` mediante `Escriben`.
   - Se generan N registros en `Ejemplares`, cada uno con código de barras (por ejemplo, BC-001), ubicación y estado **Disponible**.
2. **Agregar ejemplar:** se captura Ubicación y Fecha de adquisición. Crea un nuevo registro en `Ejemplares` del libro, con estado **Disponible**.
3. **Consulta:** un libro muestra sus ejemplares con código, ubicación y estado. Los "disponibles" son los ejemplares del libro cuyo estado es Disponible.

### 2.2 Préstamo en mostrador (solo el Administrador)
El estudiante acude a ventanilla y el **Administrador** captura sus datos y registra el préstamo.

**Paso 1: Buscar al alumno (o registrarlo) y validar su cupo**
1. El Administrador escribe la **Matrícula**.
2. El sistema la busca en `Usuario` con rol **Alumno**. El préstamo siempre se asigna a un alumno ya registrado, así que primero se busca y, si hace falta, se registra.
   - **Si no existe:** el Administrador captura el **Nombre** y el **Correo** del alumno, y se da de alta en `Usuario` con rol Alumno; enseguida se le asigna el préstamo. Es solo un registro de datos, sin Password, así que **no es una cuenta**. El correo sirve únicamente para enviarle avisos de vencimiento (§2.5). El alumno puede llevarse **hasta 2 libros**.
   - **Si existe:** se le agrega un nuevo préstamo a ese alumno. El sistema muestra el nombre y el correo guardados, que el Administrador puede **editar** si cambiaron o están mal, y calcula cuántos **libros tiene en su poder**, es decir, los ejemplares de sus préstamos que aún no tienen `fecha_devolución_real`.

| Libros en su poder | Resultado |
|---|---|
| 0 | Préstamo permitido. Puede llevarse **hasta 2 libros**. |
| 1 | **Aviso:** *"Esta matrícula ya tiene 1 libro prestado: «Título» (entrega: DD/MM/AAAA). Solo puede llevarse 1 libro más."* El préstamo se **limita a 1 libro**. |
| 2 | **Bloqueo:** *"Esta matrícula ya tiene 2 libros prestados. No puede realizar otro préstamo hasta devolver alguno."* Se muestran los títulos y fechas de entrega y **no se permite continuar**. |

Si el alumno está **bloqueado** por un libro perdido (RN-47), el préstamo tampoco se permite hasta que se registre la reposición.

**Paso 2: Selección de ejemplares**
3. El Administrador agrega los ejemplares, hasta el cupo disponible (`2 − libros en su poder`). Cada ejemplar debe:
   - estar en estado **Disponible**;
   - ser de un **libro (título) distinto** a los demás del préstamo **y** a los que el alumno ya tiene en su poder. Por ejemplo, no puede llevarse dos ejemplares de *El Principito*.

**Paso 3: Registro**
4. La **Fecha de préstamo** la toma el sistema (fecha actual, no se edita) y la **Fecha de entrega esperada** la calcula el sistema: **2 días hábiles** después, sin contar fines de semana. No se captura ni se edita; es una sola para todo el préstamo.
5. En una sola transacción:
   - se crea el `Préstamo` con estado **Activo**, ligado al Alumno (*Solicita*) y al **Administrador que tiene la sesión abierta** (*Registra*). El sistema toma al Administrador automáticamente de la sesión, así que no se captura;
   - se crea un `Detalle_prestamo` por cada ejemplar (1 o 2);
   - cada ejemplar cambia a estado **Prestado**.

### 2.3 Seguimiento de préstamos
| Filtro | Criterio |
|---|---|
| Próximas entregas | Préstamos activos, ordenados por la fecha de entrega esperada más cercana |
| Más reciente | Ordenados por fecha de préstamo, de la más reciente a la más antigua |
| Vencidos | Préstamos activos con fecha de entrega esperada anterior a hoy. Se muestra "Vencido hace N días", donde N = hoy − fecha esperada. |
| Devuelto | Préstamos con todos sus ejemplares devueltos |

### 2.4 Registrar devolución
1. La devolución se registra **por ejemplar**, porque puede devolver 1 de sus 2 libros.
2. Se captura la **Fecha de devolución real**, que se guarda en su `Detalle_prestamo`.
3. El Administrador elige el nuevo estado del **Ejemplar**: **Disponible**, **En reparación** o **Perdido**.
4. Al devolverse, ese libro **deja de contar** en los "libros en su poder" del alumno, lo que libera cupo para un nuevo préstamo.
5. Cuando **todos** los detalles del préstamo tienen fecha de devolución real, el `Préstamo` pasa a **Devuelto**.

### 2.5 Aviso por correo de vencimiento próximo
1. Una **tarea programada de Windows** ejecuta cada día el **proceso de envío**, que primero llama a un procedimiento de la base (`p_generar_notificaciones`) que busca los préstamos **Activos** cuya fecha de entrega esperada es **mañana** (o el último día hábil previo, RN-36) y crea para cada uno una notificación en estado **pendiente**, sin duplicar la misma fecha de entrega.
2. El **proceso de envío independiente** (programa Java aparte; no depende de la aplicación JavaFX) lee las notificaciones pendientes y envía a cada alumno un **correo** a la dirección registrada en `Usuario.correo`. El correo incluye su nombre, el título y la fecha de entrega.
3. El proceso actualiza `Notificación` a **exitoso** o **fallido** (con el detalle del error), para no mandarlo repetido (ver §4, C-08). El aviso se envía una sola vez por fecha de entrega.
4. En el apartado de notificaciones el Administrador ve **"Pendiente"**, **"Notificado"** o **"Envío fallido"**; al hacer clic en un fallo se muestra el detalle.
5. Los detalles de operación (cuándo se envía, cada cuánto se repite, con qué cuenta de correo) están en §6.8.

### 2.6 Inventario
Consulta de Ejemplares, con búsqueda por título del libro o código de barras. Muestra código, libro, ubicación (pasillo, estante y fila), fecha de adquisición y estado. **No gestiona alumnos ni usuarios.** El Administrador puede cambiar el estado de un ejemplar (RN-67).

---

## 3. Modelo de datos corregido

### 3.1 Entidades y atributos

| Entidad | Atributos |
|---|---|
| **Rol** | <u>id_rol</u>, nombre_rol |
| **Usuario** *(tabla `Usuarios`)* | <u>id_usuario</u>, nombre, matrícula *(única)*, correo *(único)*, password_hash, bloqueado *(booleano, por defecto falso)*, FK → Rol. Un `CHECK` (`ck_usuario_por_rol`) obliga a que el Administrador tenga password y no matrícula, y el Alumno matrícula y no password. |
| **Autores** | <u>id_autor</u>, nombre |
| **Libros** | <u>id_libro</u>, isbn, título, editorial, año_publicación, descripción |
| **Ubicaciones** | <u>id_ubicación</u>, pasillo, estante, fila |
| **Estado Ejemplares** | <u>id_estado_ejemplar</u>, nombre_estado |
| **Ejemplares** | <u>id_ejemplar</u>, código_barras *(único)*, fecha_adquisición, FK → Libro, FK → Ubicación, FK → EstadoEjemplar |
| **Estado Préstamo** | <u>id_estado_préstamo</u>, nombre_estado |
| **Préstamo** | <u>id_préstamo</u>, fecha_préstamo, **fecha_entrega_esperada**, **renovado** *(booleano, por defecto falso)*, **fecha_renovación** *(vacía si no se renovó)*, observaciones *(opcional; se llena desde el registro del préstamo o la devolución)*, **id_alumno** (FK → Usuario; en la tabla la columna se llama `id_usuario`), **id_admin** (FK → Usuario, el Administrador que registró el préstamo), FK → EstadoPréstamo |
| **Detalle_prestamo** *(entidad débil)* | <u>id_préstamo + id_ejemplar</u> *(clave compuesta de las dos relaciones; en el MER no se dibuja como atributo)*, fecha_devolución_real |
| **Estado Notificación** *(nueva)* | <u>id_estado_noti</u>, nombre_estado_noti *(pendiente, exitoso o fallido)* |
| **Notificación** *(nueva)* | <u>id_notificación</u>, fecha_envío, detalle_error, fecha_entrega_avisada, FK → Estado Notificación, FK → Detalle_prestamo *(id_préstamo + id_ejemplar: una notificación por ejemplar prestado)* |
| **Historial Ubicaciones** *(nueva)* | <u>id_historial</u>, fecha_cambio, motivo, FK → Ejemplares, id_ubicación_anterior *(FK → Ubicaciones, vacía en el primer registro)*, id_ubicación_nueva *(FK → Ubicaciones)*, id_admin_responsable *(FK → Usuario)* |

### 3.2 Relaciones y cardinalidades

> En `mer-final.drawio` la flecha de cada relación apunta a la entidad del lado **1**; *Escriben* (N : M) no lleva flechas y la identificadora de Detalle_prestamo se dibuja con doble línea. El diagrama ya trae etiquetas **1** y **N** en la mayoría de las relaciones; esta tabla es la referencia completa (incluye N : M de *Escriben* y 1 : (1..2) de *Contiene*).

| Relación | Entidades | Cardinalidad |
|---|---|---|
| Contiene | Rol → Usuario | 1 : N |
| **Solicita** | Usuario (Alumno) → Préstamo | 1 : N |
| **Registra** | Usuario (Administrador) → Préstamo | 1 : N |
| Define | Estado Préstamo → Préstamo | 1 : N |
| **Contiene** *(identificadora)* | Préstamo → Detalle_prestamo | **1 : (1..2)** |
| **Aparece en** | Ejemplares → Detalle_prestamo | 1 : N |
| Contiene | Libros → Ejemplares | 1 : N |
| Escriben | Autores ↔ Libros | N : M |
| Se ubica | Ubicaciones → Ejemplares | 1 : N |
| Define | Estado Ejemplares → Ejemplares | 1 : N |
| **Genera** | Detalle_prestamo → Notificación | 1 : N |
| Define | Estado Notificación → Notificación | 1 : N |
| **Reubica** | Ejemplares → Historial Ubicaciones | 1 : N |
| **Responsable** | Usuario (Administrador) → Historial Ubicaciones | 1 : N |

### 3.3 Uso de `Usuario` según el rol

| Atributo | Rol **Administrador** | Rol **Alumno** |
|---|---|---|
| Nombre | Obligatorio | Obligatorio (lo captura el Administrador) |
| Correo | **Obligatorio** (sirve para iniciar sesión) | **Obligatorio** (lo captura el Administrador) y **único**: no puede repetirse entre alumnos. Solo sirve para enviar avisos de vencimiento, **no** para iniciar sesión. |
| Password | **Obligatorio**; debe guardarse cifrado (hash) | **Vacío (NULL).** Sin él no puede iniciar sesión. |
| Matrícula | Vacío (NULL) | **Obligatoria** y única (identifica al alumno en mostrador) |
| ¿Puede iniciar sesión? | Sí | **No.** Es solo un registro de datos del préstamo. Aunque tenga correo, no tiene Password y su rol no es Administrador. |

### 3.4 Catálogos

| Catálogo | Valores |
|---|---|
| Rol | **Administrador**, **Alumno** |
| Estado del ejemplar | Disponible · Prestado · En reparación · Perdido |
| Estado del préstamo | Activo · Atrasado · Devuelto · Cancelado *(`p_actualizar_retraso` pasa a Atrasado los Activos con fecha de entrega esperada anterior a hoy; las vistas de vencidos consideran ambos estados)* |
| Estado de la notificación | Pendiente · Exitoso · Fallido |

> Los valores de los catálogos se guardan en mayúsculas en la base de datos (`DISPONIBLE`, `PRESTADO`, `ACTIVO`, etc.) y se cargan directamente con `INSERT`, igual que Rol.

### 3.6 Objetos de base de datos

| Objeto | Función |
|---|---|
| `p_registrar_prestamo` | Valida alumno existente, no bloqueado y ejemplar Disponible; crea el préstamo y su detalle con la fecha de entrega calculada (recibe el plazo en días hábiles como parámetro, 2 por regla; un ejemplar por llamada); pasa el ejemplar a Prestado. |
| `p_devolucion_ejemplar` | Guarda la fecha de devolución real y las observaciones, cambia el estado del ejemplar y bloquea al alumno si queda Perdido (RN-47). |
| `p_actualizar_retraso` | Pasa a Atrasado los préstamos Activos con fecha de entrega esperada anterior a hoy. |
| `p_renovar_prestamo` | Aplica la renovación (RN-44 y RN-45): solo Activo, no vencido y una sola vez. |
| `fn_sumar_dias_habiles(fecha, días)` | Suma días hábiles saltando sábados y domingos (RN-20). |
| `fn_limite_prestamos` + trigger `tigresito_limite_prestamos` | Antes de insertar un detalle, impide que el alumno pase de 2 libros en su poder (RN-06). |
| `fn_registro_ubicacion` + trigger `tigresito_registro_ubicacion` | Al cambiar la ubicación de un ejemplar, registra la reubicación en `Historial Ubicaciones`. |
| Vistas | `v_catalogo_libros`, `v_prestamos_activos`, `v_inventario_fisico`, `v_ubicaciones`, `v_prestamos_vencidos`, `v_alumnos_bloqueados`, `v_historial_prestamos`. |
| Zona horaria | La base usa `America/Mexico_City`. |

### 3.5 Datos calculados (no se guardan)

| Dato | Cálculo |
|---|---|
| Libros en poder del alumno | Detalles de los préstamos del alumno con `fecha_devolución_real` vacía |
| Cupo del alumno | 2 − libros en su poder |
| Préstamo vencido | Estado Activo y `fecha_entrega_esperada` < hoy |
| Días de atraso | hoy − `fecha_entrega_esperada` |
| Ejemplares disponibles de un libro | Ejemplares del libro con estado Disponible |

---

## 4. Correcciones aplicadas al MER (`mer-final.drawio`)

| # | Error en el MER | Por qué está mal | Corrección |
|---|---|---|---|
| C-01 | Ninguna relación tiene **cardinalidad** | Sin cardinalidades no se puede expresar el límite de 2 libros ni que un alumno tenga varios préstamos. | Definir la cardinalidad de todas las relaciones (§3.2), con **Préstamo 1 : (1..2) Detalle_prestamo**. En el diagrama se marca el lado 1 con flecha y se rotulan 1 y N; M y 1..2 quedan en la §3.2. |
| C-02 | La relación entre Préstamo y Ejemplar está **dibujada dos veces**: el rombo *Involucra* y la entidad débil Detalle_prestamo, unida a Préstamo con una línea directa sin rombo. | Es redundante. Además, Detalle_prestamo no tiene relación con Ejemplares, y sus claves foráneas (id_préstamo, id_ejemplar, id_estado_prestamo) aparecen dibujadas como atributos. | Quitar *Involucra*. Dejar Detalle_prestamo como entidad débil con dos relaciones: la identificadora con Préstamo y *Aparece en* con Ejemplares. Quitar las claves foráneas dibujadas como atributos. |
| C-03 | Hay una sola relación, *Realiza / Autoriza*, entre Usuario y Préstamo | **Es obligatorio** que la tabla Préstamo registre qué Administrador hizo el préstamo, además del alumno. Con una sola relación solo sale **una** FK hacia Usuario, así que el MER no puede guardar a los dos. | Dividirla en dos relaciones: **Solicita** (Usuario Alumno 1 : N Préstamo), que genera `id_alumno`, y **Registra** (Usuario Administrador 1 : N Préstamo), que genera `id_admin`. |
| C-04 | El estado del préstamo aparece **dos veces**: en Préstamo y en Detalle_prestamo | Pueden contradecirse. | El estado vive solo en **Préstamo**. Para cada ejemplar, basta ver si `fecha_devolución_real` está vacía. |
| C-05 | `Fecha EntregaEsperada` está en Detalle_prestamo | Permite fechas límite distintas dentro de un mismo préstamo, y el préstamo no tiene la suya. | Moverla a **Préstamo**. |
| C-06 | La línea de `Fecha_devolución_real` **no está unida** a ninguna entidad | El atributo no pertenece a ninguna entidad. | Unirla a **Detalle_prestamo**. |
| C-07 | Matrícula, Correo y Código de barras **no se marcan como únicos** | La búsqueda por matrícula y el inicio de sesión dependen de que no se repitan. | Declarar **únicos** la Matrícula, el Correo, el Código de barras y el ISBN (RN-55) como restricción `UNIQUE` en la base de datos; el MER los marca subrayados. El Correo es único para todos los usuarios (RN-29). |
| C-08 | El MER **no guarda los avisos enviados** | Sin registro, el sistema no sabría si el alumno fue avisado ni por qué falló un envío (RN-40), ni podría dar un aviso por cada fecha vigente tras una renovación (RN-37). | Agregar la entidad **Notificación** (id, fecha_envío, detalle_error, fecha_entrega_avisada), con su catálogo **Estado Notificación**, ligada a Detalle_prestamo 1 : N (relación **Genera**). |
| C-09 | El MER **no guarda el historial de reubicaciones** | Al cambiar la ubicación de un ejemplar se pierde dónde estaba antes y quién lo movió. | Agregar la entidad **Historial Ubicaciones**, ligada a Ejemplares, a Ubicaciones (anterior y nueva) y al Administrador responsable. Un trigger la llena (RN-70). |

> Las reglas de "máximo 2 libros en su poder" y "no dos ejemplares del mismo libro" **no se pueden expresar solo con la estructura del MER**, porque dependen de préstamos distintos y de datos calculados. Se documentan como reglas de negocio (§5) y se hacen cumplir en la lógica del sistema o con triggers en la base de datos.

---

## 5. Reglas de negocio

| # | Regla |
|---|---|
| RN-01 | Solo existen dos formas de entrar: **Estudiante** (anónimo, solo lectura) y **Administrador** (con Correo y Password). |
| RN-02 | Solo pueden iniciar sesión los registros de `Usuario` con rol **Administrador** y Password. Un Alumno nunca inicia sesión, aunque tenga correo registrado. |
| RN-03 | **No se crean cuentas de estudiante.** Un `Usuario` con rol Alumno es un registro de datos que el Administrador crea al hacer un préstamo. |
| RN-04 | El estudiante **nunca** solicita préstamos. El Administrador captura los datos y registra el préstamo. **La tabla Préstamo guarda qué Administrador lo registró** (`id_admin`), tomado de la sesión. |
| RN-05 | La Matrícula es única. Al prestar, el alumno se busca por Matrícula y se reutiliza si ya existe. Si no, se da de alta con **Nombre, Matrícula y Correo**, todos obligatorios. |
| RN-06 | **Máximo 2 libros en poder del alumno** al mismo tiempo, sumando todos sus préstamos no devueltos. |
| RN-07 | Al capturar la matrícula, el sistema indica los libros que ya tiene en su poder. Con 1, **limita** el préstamo a 1 libro más. Con 2, **bloquea** el préstamo. |
| RN-08 | Un préstamo tiene **1 o 2** ejemplares, sin pasar del cupo del alumno (2 − libros en su poder). |
| RN-09 | **No se permiten dos ejemplares del mismo libro** en poder del alumno: ni dentro del mismo préstamo ni sumando los que ya tiene. Por ejemplo, solo puede tener un *El Principito*. |
| RN-10 | Solo se presta un Ejemplar en estado **Disponible**. Al prestarlo pasa a **Prestado**. |
| RN-11 | La fecha de entrega esperada es **una por préstamo**. |
| RN-12 | La devolución es **por ejemplar**: se guarda la fecha real y el Ejemplar pasa a Disponible, En reparación o Perdido. El libro devuelto libera cupo del alumno. |
| RN-13 | Un préstamo pasa a **Devuelto** cuando todos sus ejemplares fueron devueltos. |
| RN-14 | Un préstamo está vencido cuando está Activo y hoy > fecha de entrega esperada. Días de atraso = hoy − fecha esperada. |
| RN-15 | Un Libro tiene uno o más Ejemplares. Cada Ejemplar tiene un código de barras único y una Ubicación (pasillo, estante y fila). |
| RN-16 | Un Libro puede tener varios Autores y un Autor puede haber escrito varios Libros. |
| RN-17 | El Inventario es de consulta de Ejemplares, con una sola acción de escritura: **Cambiar estado del ejemplar** (RN-67). No gestiona usuarios ni alumnos. |
| RN-18 | El sistema envía un **aviso por correo** al alumno **un día antes** de que venza su préstamo, con su nombre, el título y la fecha de entrega, y registra el envío (RN-36 a RN-42). |
| RN-19 | Un préstamo vencido **no bloquea** por sí solo un préstamo nuevo. El alumno puede llevarse otro libro si le queda cupo. Si ya tiene 2 libros en su poder (aunque estén vencidos), se aplica el bloqueo de RN-07. |
| RN-20 | El plazo del préstamo es de **2 días hábiles** (sin fines de semana) y es también el plazo máximo. El sistema calcula la fecha de entrega esperada automáticamente y no se edita. |
| RN-21 | La fecha de préstamo es la **fecha del sistema** y no se puede editar. |
| RN-22 | La fecha de entrega esperada siempre es posterior a la de préstamo (se deriva de RN-20) y un `CHECK` exige que no sea anterior a hoy. La fecha de devolución real debe estar entre la fecha de préstamo y hoy. |
| RN-23 | Un préstamo se puede **editar o anular solo el mismo día** en que se registró. Al anularlo, los ejemplares vuelven a Disponible y el préstamo toma el estado **Cancelado**. |
| RN-24 | Un préstamo Activo se puede **renovar** (RN-44 y RN-45). Se guarda con `renovado` y `fecha_renovación` en Préstamo (P-10). |
| RN-25 | Físicamente solo existe un Administrador operando el mostrador, así que no se contempla que dos Administradores presten al mismo tiempo. |
| RN-26 | La Matrícula tiene exactamente **9 dígitos numéricos**. Se valida al capturarla y al buscarla. |
| RN-27 | Si la matrícula ya existe, el sistema muestra el nombre y el correo guardados y el Administrador puede **editarlos** antes de continuar con el préstamo. |
| RN-28 | El correo del alumno acepta **cualquier dominio**; solo se valida que tenga formato de correo. |
| RN-29 | El correo es **único** para todos los usuarios (`UNIQUE` en `Usuarios.correo`): dos alumnos no pueden compartir correo. |
| RN-30 | Los alumnos no se dan de baja (excluido por ahora). El único bloqueo que existe es el automático por ejemplar perdido (RN-47). |
| RN-31 | El Administrador inicia sesión **solo con Correo y Password**. No existe nombre de usuario. |
| RN-32 | El Administrador se crea **directamente en la base de datos**. Solo hay **uno**; el sistema no tiene alta ni gestión de Administradores. |
| RN-33 | La contraseña del Administrador se define en la base de datos. El sistema no ofrece cambio ni recuperación de contraseña. |
| RN-34 | Si el correo o la contraseña no son válidos, el sistema **no permite la entrada** hasta que se capturen datos correctos. No hay bloqueo por intentos fallidos ni cierre de sesión por inactividad. |
| RN-35 | Las contraseñas se guardan **con hash**, nunca en texto plano. |
| RN-36 | El sistema envía un **recordatorio por correo un día antes** de la fecha de vencimiento del préstamo. Si ese día no es hábil (vencimiento en lunes), se envía el **último día hábil previo** (viernes). |
| RN-37 | El aviso **no se repite** para una misma fecha de entrega esperada. Si el préstamo se renueva, la nueva fecha vigente tiene **su propio aviso**. |
| RN-38 | El envío es **automático** y **no depende de que la aplicación esté abierta**. Un **proceso de envío independiente** (programa Java aparte, ejecutado una vez al día con el Programador de tareas de Windows) crea las notificaciones *pendientes* llamando a la base, envía los correos por SMTP y registra el resultado. No se usa pgAgent. PostgreSQL no envía el correo. |
| RN-39 | Los correos salen de una **cuenta del sistema**; más adelante se cambiará por una cuenta institucional real. Las credenciales no se escriben en el código. |
| RN-40 | `Notificación` guarda si el envío está pendiente, fue exitoso o falló. En el apartado de envío de notificaciones se muestra **"Notificado"** o **"Envío fallido"**; al hacer clic en un fallo se ve el detalle. |
| RN-41 | El correo informa que **al día siguiente se vence** el préstamo y lleva el **nombre** del alumno, el **título** del libro y la **fecha de entrega**. |
| RN-42 | No se envían correos al registrar un préstamo ni una devolución. |
| RN-43 | Si un envío falla, no hay reintento automático: el Administrador puede **reenviar manualmente** el aviso desde el apartado de notificaciones (pestaña *Próximas entregas* de Préstamos); el aviso vuelve a *pendiente* y se envía en la siguiente ejecución del proceso de envío. |
| RN-44 | Un préstamo se puede renovar **una sola vez**. La renovación suma **2 días hábiles** a la fecha de entrega esperada vigente. |
| RN-45 | Solo se puede renovar un préstamo en estado **Activo** y con hoy ≤ fecha de entrega esperada. Un préstamo vencido no se renueva: el alumno debe acudir al mostrador a regularizarlo. |
| RN-46 | Devolver un préstamo vencido **no tiene multa ni sanción**. Solo se conserva el historial: los días de atraso de cada ejemplar se obtienen como `fecha_devolución_real` − `fecha_entrega_esperada`, sin guardar un dato nuevo. |
| RN-47 | Al registrar una devolución con estado **Perdido**, el procedimiento almacenado marca al alumno como `bloqueado = TRUE`. Un alumno bloqueado **no puede recibir préstamos nuevos** (ni renovar). El libro perdido sí libera cupo (RN-12), pero el bloqueo lo compensa. |
| RN-48 | La reposición se registra con un procedimiento almacenado (ej. `sp_registrar_reposicion`) que da de alta un **ejemplar nuevo** (con su propio código de barras) en estado **Disponible** y quita el bloqueo del alumno (`bloqueado = FALSE`). El ejemplar perdido se conserva como histórico con estado Perdido. |
| RN-49 | Un libro perdido se registra con el **mismo flujo de devolución** (`p_devolucion_ejemplar`): el Administrador elige el estado **Perdido** y la `fecha_devolución_real` se toma como la **fecha en que se reportó la pérdida**. |
| RN-50 | La consulta del estudiante muestra **una sola ubicación** por libro. Se asume que todos los ejemplares de un libro están en el mismo lugar. |
| RN-51 | Si un libro no tiene ejemplares Disponibles, la consulta muestra **"No disponible"**, sin fecha probable de regreso. |
| RN-52 | La consulta del estudiante **no muestra** ejemplares Perdidos ni En reparación; solo cuenta los Disponibles. |
| RN-53 | La búsqueda es por **coincidencia parcial**, sin distinguir mayúsculas ni acentos. |
| RN-54 | Se pueden **editar** los datos del libro y la **ubicación** del ejemplar. **No se elimina** ningún libro ni ejemplar, para conservar el historial. |
| RN-55 | El **ISBN es único**. Si se captura uno que ya existe, el sistema avisa y ofrece *Agregar ejemplar* al libro existente. |
| RN-56 | Las ubicaciones (pasillo, estante y fila) se eligen de un **catálogo predefinido**, no se escriben libremente. |
| RN-57 | Al agregar un libro, todos sus ejemplares iniciales van a la **misma ubicación**. Se reubican después si hace falta. |
| RN-58 | Los autores se **buscan y seleccionan**; si no existe, se crea en ese momento. |
| RN-59 | Al agregar un libro son obligatorios: título, al menos 1 autor, ISBN, año, ubicación y ejemplares ≥ 1. |
| RN-60 | La descripción del libro se puede capturar, pero es **opcional**. |
| RN-61 | El catálogo de ubicaciones (RN-56) se carga **directamente en la base de datos**; el sistema no tiene módulo para mantenerlo. |
| RN-62 | Al reubicar (RN-54), se mueven **todos los ejemplares del libro juntos**, para que la consulta del estudiante siga mostrando una sola ubicación (RN-50). |
| RN-63 | El listado de préstamos muestra **una fila por ejemplar prestado** (alumno, libro, fecha de entrega, estado), con devolución individual. |
| RN-64 | El filtro *Próximas entregas* muestra solo los préstamos **Activos**; *Más reciente* muestra **todos**, incluidos los devueltos. |
| RN-65 | El módulo Préstamo tiene una **barra de búsqueda** por matrícula o nombre del alumno. |
| RN-66 | El Inventario permite **filtrar por estado** del ejemplar, además de la búsqueda. |
| RN-67 | El Administrador puede **cambiar el estado de un ejemplar** desde el Inventario (por ejemplo, de En reparación a Disponible). Un ejemplar Perdido no vuelve a Disponible con esta acción: se repone con RN-48. |
| RN-68 | El bloqueo del alumno por libro perdido (RN-47) **no tiene plazo**: dura hasta que se registre la reposición. |
| RN-70 | Cada cambio de ubicación de un ejemplar se registra en `Historial Ubicaciones` (ubicación anterior y nueva, Administrador responsable, fecha y motivo). Es un historial de reubicaciones, no una bitácora general de actividad. |
| RN-69 | Si un alumno tiene varios libros que vencen el mismo día, recibe **un solo correo** con todos ellos, aunque sean de préstamos distintos. Como `Notificación` se liga a un solo Detalle_prestamo (*Genera*), se crea una notificación por ejemplar y el **proceso de envío las agrupa por alumno** al armar el correo; todas las agrupadas pasan juntas a exitoso o fallido. |

---

## 6. Casos no definidos (pendientes de decisión)

> Cada caso trae una **propuesta** para no frenar el diseño. Hasta que el equipo confirme o cambie la decisión, la propuesta es **provisional** y no forma parte de las reglas de la §5.

### 6.1 Préstamo

**Resueltos** (decididos por el equipo; ya incorporados en §2.2, §3.4, §5 y §7):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| P-01 | ¿Un préstamo vencido bloquea uno nuevo? | **No.** El alumno puede llevarse otro libro aunque tenga uno pendiente. Si debe los dos libros (cupo lleno), el préstamo no se tramita. | RN-19 |
| P-02 | Duración, fecha de entrega, plazo máximo y fines de semana | **2 días**, sin contar fines de semana. El sistema calcula la fecha de entrega automáticamente. Ese plazo es el máximo. | RN-20 |
| P-03 | ¿Se edita la fecha de préstamo? | **No.** Se toma la fecha del sistema. | RN-21 |
| P-04 | Validación de fechas | La entrega se determina con los 2 días transcurridos, así que siempre es posterior al préstamo. Devolución real entre la fecha de préstamo y hoy *(se conserva la propuesta anterior; no se contradijo)*. | RN-22 |
| P-05 | Corregir o anular un préstamo | Se puede **editar o anular solo el mismo día**. Al anular: ejemplares a Disponible y estado Cancelado. | RN-23 |
| P-06 | Concurrencia entre Administradores | **No aplica:** físicamente solo hay un Administrador. | RN-25 |
| P-07 | ¿Se permite renovar? | **Sí.** Sale de las exclusiones y entra al alcance. | RN-24 |

**Derivados de P-07:**

| # | Caso | Decisión | Regla |
|---|---|---|---|
| P-08 | Duración y número de renovaciones | **1 sola renovación** por préstamo, de **2 días hábiles adicionales**. El límite evita que un alumno acapare libros de alta demanda durante semanas. | RN-44 |
| P-09 | Renovar un préstamo vencido | **No.** Solo procede si el préstamo está Activo y hoy ≤ fecha de entrega esperada. Si ya está vencido, el alumno debe ir al mostrador a regularizar su situación. | RN-45 |

**Resuelto:**

| # | Caso | Decisión | Regla |
|---|---|---|---|
| P-10 | ~~¿Qué se guarda de la renovación?~~ **Resuelto:** dos atributos en `Préstamo`: `renovado` y `fecha_renovación`. No se conserva historial, porque solo se permite una renovación (RN-44). La nueva `fecha_entrega_esperada` sobrescribe la anterior. | — | — |

### 6.2 Devolución y estados del ejemplar

| # | Caso no definido | Por qué importa | Propuesta |
|---|---|---|---|
| D-01 | ~~¿Un ejemplar devuelto como "Perdido" libera cupo? ¿Hay consecuencia?~~ **Resuelto:** el alumno queda **bloqueado** hasta que se registre la reposición (RN-47 y RN-48). La reposición registra un ejemplar nuevo y el bloqueo no tiene plazo (RN-68). | — | — |
| D-02 | ~~¿Cómo vuelve a Disponible un ejemplar "En reparación" o "Perdido"?~~ **Resuelto:** acción "Cambiar estado del ejemplar" en el Inventario, solo para el Administrador (RN-67). El Perdido se repone con RN-48. | — | — |
| D-03 | ~~¿Un alumno que perdió el libro se registra con el flujo de devolución (estado "Perdido") aunque no entregue nada?~~ **Resuelto:** sí, con el mismo flujo (RN-49). | — | — |
| D-04 | ~~¿La devolución de un préstamo vencido cambia algo, además de cerrarlo (multa o sanción)?~~ **Resuelto:** no hay multa económica ni sanción. No se modelan tablas de multas o pagos y no hay cambios en la base de datos. Solo queda el historial de días de atraso (RN-46). | — | — |
| D-05 | **Pendiente de implementar:** ¿cómo registra el Administrador la reposición (RN-48) de un alumno bloqueado? Hoy `p_devolucion_ejemplar` lo bloquea al marcar el ejemplar como Perdido, pero no existe `sp_registrar_reposicion` ni pantalla que lo desbloquee. El préstamo perdido ya sale de "Próximas entregas" y la ficha del alumno solo muestra el aviso de bloqueo. | El alumno queda bloqueado sin forma de volver a prestar desde la aplicación. | Botón "Registrar reposición" en la ficha del alumno bloqueado (visible solo si `bloqueado = TRUE`): pide código de barras y ubicación del ejemplar nuevo, y un procedimiento almacenado lo da de alta como Disponible y pone `bloqueado = FALSE`. Mostrar en la ficha qué libro se perdió. A decidir: si la reposición puede ser de otro título, y si "En reparación" también debe bloquear. |

### 6.3 Alumno (registro en `Usuario`)

Todos resueltos (ver reglas RN-26 a RN-30 en §5):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| A-01 | Formato de la matrícula | **Solo números, longitud de 9.** | RN-26 |
| A-02 | Datos que se capturan del alumno | Solo **nombre, matrícula y correo**, todos obligatorios, por ahora. | RN-05 |
| A-03 | Matrícula existente con nombre o correo desactualizados | Se **muestran los datos guardados y se pueden editar** en ese momento. | RN-27 |
| A-04 | Dar de baja o bloquear alumnos | **Excluido** por ahora. | §7 |
| A-05 | Dominio y formato del correo | Se acepta **cualquier dominio**; se valida solo que tenga formato de correo. | RN-28 |
| A-06 | ¿Correo único? | **Sí.** El correo no se puede repetir entre alumnos. | RN-29 |

### 6.4 Libros, ejemplares y ubicaciones

Resueltos (ver RN-54 a RN-60 en §5):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| L-01 | Editar, eliminar o reubicar | **De acuerdo:** se pueden editar los datos del libro y la ubicación del ejemplar. **No se elimina**, para conservar el historial. | RN-54 |
| L-03 | ISBN que ya existe | **De acuerdo:** el ISBN es único; se avisa y se ofrece *Agregar ejemplar* al libro existente. | RN-55 |
| L-04 | Origen de las ubicaciones | **De acuerdo:** catálogo predefinido que se elige de una lista. | RN-56 |
| L-05 | Ubicación de los ejemplares iniciales | **De acuerdo:** todos van a la misma ubicación; se reubican después si hace falta. | RN-57 |
| L-06 | Autores | **De acuerdo:** se busca y selecciona el autor; si no existe, se crea en ese momento. | RN-58 |
| L-07 | Campos obligatorios y validaciones | **De acuerdo:** título, al menos 1 autor, ISBN, año, ubicación y ejemplares ≥ 1. | RN-59 |
| L-08 | Descripción del libro | **Sí se captura**, como campo **opcional**. | RN-60 |

**Pendiente:**

| # | Caso no definido | Por qué importa | Propuesta |
|---|---|---|---|
| L-02 | ¿Cómo se **genera el código de barras**? ¿Es consecutivo global (BC-001, BC-002…) o por libro? ¿Lo captura el Administrador o lo genera el sistema? | Debe ser único. | Lo genera el sistema, consecutivo global con formato `BC-###`. |

### 6.5 Consulta del estudiante

Todos resueltos (ver RN-50 a RN-53 en §5):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| E-01 | Ubicación de un libro con ejemplares en ubicaciones distintas | Se muestra **una sola ubicación**, la donde se encuentran los ejemplares. Todo está en orden: los ejemplares de un libro están juntos. | RN-50 |
| E-02 | Libro con 0 disponibles | Se muestra **"No disponible"**, sin fecha de regreso. | RN-51 |
| E-03 | Ejemplares Perdidos o En reparación | **No se muestran** al estudiante. | RN-52 |
| E-04 | Tipo de búsqueda | **De acuerdo con la propuesta:** coincidencia parcial, sin distinguir mayúsculas ni acentos. | RN-53 |

### 6.6 Administrador y seguridad

Todos resueltos (ver RN-31 a RN-35 en §5):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| S-01 | ¿Con qué se inicia sesión? | **Solo con correo y contraseña**, ya definidos. No hay nombre de usuario. | RN-31 |
| S-02 | Alta de Administradores | Vienen **desde la base de datos** y solo se necesita **uno**: es una biblioteca pequeña. | RN-32 |
| S-03 | Cambio o recuperación de contraseña | La contraseña queda definida desde la base. No hay flujo de cambio ni de recuperación. | RN-33 |
| S-04 | Bloqueo por intentos fallidos o por inactividad | **No hay bloqueo ni cierre por inactividad.** Solo se impide la entrada hasta que los datos sean válidos. | RN-34 |
| S-05 | Almacenamiento de contraseñas | Se guardan con **hash**. | RN-35 |

### 6.7 Seguimiento e inventario

Todos resueltos (ver RN-63 a RN-66 en §5):

| # | Caso | Decisión | Regla |
|---|---|---|---|
| G-01 | Filas del listado de préstamos | **De acuerdo:** una fila por ejemplar prestado (alumno, libro, entrega, estado), con devolución individual. | RN-63 |
| G-02 | Filtros *Próximas entregas* y *Más reciente* | **De acuerdo:** *Próximas entregas* solo muestra los Activos; *Más reciente* muestra todos. | RN-64 |
| G-03 | Búsqueda de préstamos por alumno | **De acuerdo:** barra de búsqueda por matrícula o nombre en el módulo Préstamo. | RN-65 |
| G-04 | Filtros del Inventario | **De acuerdo:** filtro por estado del ejemplar. | RN-66 |

### 6.8 Aviso por correo de vencimiento

**Resueltos** (ver RN-36 a RN-42 en §5). El aviso pasó de ser "préstamo vencido" a **recordatorio previo al vencimiento**:

| # | Caso | Decisión | Regla |
|---|---|---|---|
| N-01 | ¿Cuándo se envía? | **Un día antes** de la fecha de vencimiento. | RN-36 |
| N-02 | ¿Se repite? | **No.** Se envía una sola vez por **fecha de entrega esperada** de cada préstamo (si se renueva, la nueva fecha tiene su propio aviso; ver N-12). | RN-37 |
| N-03 | ¿Cómo se dispara? | Una **tarea programada de Windows** lanza un **proceso de envío independiente** que crea las notificaciones pendientes (llamando a la base) y las envía. Base de datos: **PostgreSQL**. | RN-38 |
| N-04 | ¿Automático o manual? | **Automático.** | RN-38 |
| N-05 | ¿Desde qué cuenta se envía? | Con una **cuenta del sistema**; más adelante se cambiará a una institucional real. | RN-39 |
| N-06 | ¿Qué pasa si el correo falla? | En el apartado de envío de notificaciones se muestra **"Notificado"** o **"Envío fallido"**; al darle clic a un fallido se muestra el detalle del fallo. | RN-40 |
| N-07 | ¿Qué contenido lleva el correo? | Indica que **al día siguiente se vence su préstamo**. Lleva **nombre, título y fecha de entrega**. | RN-41 |
| N-09 | ¿Aviso en préstamo o devolución? | **No.** Fuera del alcance por ahora. | §7 |
| N-10 | Vencimiento en lunes: "un día antes" caería en domingo | Se envía el **último día hábil previo** al vencimiento (viernes para un lunes). | RN-36 |
| N-11 | Fallo del correo: ¿reintento o reenvío? | Se permite **reenviar manualmente** desde el apartado de notificaciones. | RN-43 |
| N-12 | Renovación después de enviado el aviso | **Cada fecha de entrega esperada vigente tiene su propio aviso.** | RN-37 |

**Resuelto:**

| # | Caso | Decisión | Regla |
|---|---|---|---|
| N-08 | Varios libros que vencen el mismo día en préstamos distintos | **Un solo correo por alumno** con todos sus libros. | RN-69 |

---

## 7. Alcance

**Incluido:** selección de rol y autenticación del Administrador; catálogo con búsqueda y ubicación; alta de libros y ejemplares; préstamo en mostrador con validación de la matrícula, límite de 2 libros en poder y sin libros repetidos; seguimiento de préstamos y vencimientos; **aviso por correo al alumno un día antes de que venza su préstamo**, con seguimiento de envíos y fallos; devoluciones por ejemplar; **renovación de préstamos**; **edición y anulación de préstamos el mismo día**; consulta del inventario de ejemplares; **historial de reubicaciones de ejemplares**.

**Excluido:** cuentas y login de estudiantes, "Mis préstamos", rol Auxiliar, gestión de usuarios, bitácora general de actividad, sanciones, categorías, recuperación de contraseña, comprobantes y exportaciones.
