# Procedimientos y funciones requeridos en la base de datos

Hoy los DAOs de Java ejecutan SQL directo (`SELECT`, `INSERT`, `UPDATE`, `DELETE`). Este documento lista lo que debe crear el encargado de la base para que los DAOs solo llamen a `CALL` / `SELECT fn_...`.

## Convenciones

- **Procedimiento (`CALL`)**: escribe datos. Lo que devuelve va como parámetro `OUT`.
- **Función (`SELECT * FROM fn_...`)**: solo lee. Devuelve una tabla con las columnas indicadas, con esos nombres exactos (los DAOs los leen por nombre).
- Los estados se reciben por **nombre** (`'DISPONIBLE'`, `'PRESTADO'`, `'EN REPARACION'`, `'PERDIDO'`), no por id.
- Los errores de negocio se lanzan con `RAISE EXCEPTION` y un mensaje claro en español; la aplicación lo muestra al usuario.
- Donde se indica un `SQLSTATE` o nombre de restricción, la aplicación depende de él y no debe cambiar.

## Ya existen y se mantienen

| Nombre | Tipo | Uso |
|---|---|---|
| `p_registrar_prestamo(matricula, codigo_barras, id_admin, dias_prestamo, observaciones)` | Procedimiento | Registrar préstamo |
| `p_devolucion_ejemplar(id_prestamo, id_ejemplar, nombre_estado, observaciones)` | Procedimiento | Registrar devolución |
| `p_renovar_prestamo(id_prestamo, id_ejemplar)` | Procedimiento | Renovar |
| `p_actualizar_retraso()` | Procedimiento | Marcar préstamos atrasados |
| `fn_sumar_dias_habiles(fecha, dias)` | Función | Fecha de entrega prevista |
| Vistas `v_catalogo_libros`, `v_prestamos_activos`, `v_prestamos_vencidos`, `v_historial_prestamos`, `v_ubicaciones`, `v_alumnos_bloqueados` | Vistas | Siguen igual |

---

## Vistas nuevas

Cada DAO tiene hoy una constante `SELECT` con los `JOIN` ya armados, a la que el código le pega un `WHERE`. Esa parte base se pasa a una vista; las funciones de las secciones siguientes solo filtran sobre ella. Los nombres de columna deben ser exactamente estos.

| Vista | Reemplaza | Columnas | La usan las funciones |
|---|---|---|---|
| `v_usuarios_rol` | `UsuarioDAO.SELECT_USUARIO` | `id_usuario, nombre, correo, password_hash, matricula, bloqueado, id_rol, nombre_rol` (`usuarios` con `INNER JOIN rol`) | #1, #2 |
| `v_ejemplares_detalle` | `EjemplarDAO.SELECT_EJEMPLAR` | `id_ejemplar, codigo_barras, fecha_adquisicion, id_libro, titulo, isbn, id_ubicacion, pasillo, estante, fila, id_estado_ejemplar, nombre_estado_ejemplar` (`ejemplares` con `libros`, `ubicaciones` y `estado_ejemplares`) | #13, #14 |
| `v_libros_detalle` | `LibroDAO.SELECT_LIBRO` | `id_libro, isbn, titulo, editorial, anio_publicacion, descripcion, portada` (`libros` con `LEFT JOIN portada`, para que aparezcan también los libros sin portada; `portada` es `url_portada`) | #25, #26 |
| `v_detalle_prestamos` | `PrestamoDAO.SELECT_DETALLE` | `id_prestamo, fecha_prestamo, fecha_entrega_esperada, renovado, fecha_renovacion, observaciones, id_estado_prestamo, nombre_estado_prestamo, id_usuario, nombre, matricula, correo, id_ejemplar, codigo_barras, id_libro, titulo, fecha_devolucion_real` (`prestamos` con `detalle_prestamo`, `usuarios`, `estado_prestamo`, `ejemplares` y `libros`; una fila por ejemplar prestado, RN-63) | #8, #12 |

**Seguridad:** `v_usuarios_rol` expone `password_hash`. Debe usarla solo `fn_autenticar_administrador` (#1): no se le da permiso de lectura a nada más, o bien el hash se deja fuera de la vista y la función lo lee de la tabla `usuarios`.

---

## 1. Usuarios y alumnos (`UsuarioDAO`)

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 1 | `fn_autenticar_administrador` | Función | `p_correo TEXT` | Una fila con `id_usuario, nombre, correo, password_hash, matricula, bloqueado, id_rol, nombre_rol`. Usa `v_usuarios_rol`. Compara el correo sin importar mayúsculas y solo para el rol `Administrador`. Sin resultado si no existe. La contraseña se verifica en Java con el hash. |
| 2 | `fn_buscar_alumno_por_matricula` | Función | `p_matricula TEXT` | Usa `v_usuarios_rol`. Mismas columnas que la 1, solo para el rol `Alumno`. |
| 3 | `fn_buscar_alumnos` | Función | `p_texto TEXT` (vacío = todos) | `id_usuario, matricula, nombre, correo, bloqueado, prestamos_activos, prestamos_total`. Busca por matrícula o nombre con `ILIKE`. **Orden: `id_usuario DESC`** (el último registrado primero). `prestamos_activos` = ejemplares sin devolver en préstamos `ACTIVO` o `ATRASADO`; `prestamos_total` = préstamos del alumno. |
| 4 | `sp_registrar_alumno` | Procedimiento | `p_nombre, p_correo, p_matricula` | `OUT p_id_usuario INT`. Inserta con rol `Alumno`. Debe conservar las restricciones `usuarios_matricula_key`, `usuarios_correo_key` y `check_correo`, porque la aplicación detecta esos nombres para avisar al usuario. |
| 5 | `sp_actualizar_alumno` | Procedimiento | `p_id_usuario, p_nombre, p_correo` | Actualiza nombre y correo. La matrícula no cambia. |
| 6 | `sp_eliminar_alumno` | Procedimiento | `p_id_usuario` | Borra solo si el usuario tiene rol `Alumno`. Si tiene préstamos (aunque estén devueltos) debe fallar con `SQLSTATE 23503`. |
| 7 | `sp_registrar_reposicion` | Procedimiento | `p_id_usuario, p_id_libro, p_id_ubicacion, p_codigo_barras, p_fecha_adquisicion` | **Pendiente D-05 / RN-48.** Crea un ejemplar nuevo en estado `DISPONIBLE` y pone `bloqueado = FALSE` al alumno. Falla si el alumno no está bloqueado. Ver `contexto_sistema.md`. |

## 2. Préstamos (`PrestamoDAO`)

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 8 | `fn_listar_en_poder` | Función | `p_id_alumno INT` | Usa `v_detalle_prestamos`. Libros que el alumno tiene sin devolver (préstamo `ACTIVO` o `ATRASADO`, `fecha_devolucion_real IS NULL`), ordenados por `fecha_entrega_esperada`. Columnas: `id_prestamo, fecha_prestamo, fecha_entrega_esperada, renovado, fecha_renovacion, observaciones, id_estado_prestamo, nombre_estado_prestamo, id_usuario, nombre, matricula, correo, id_ejemplar, codigo_barras, id_libro, titulo, fecha_devolucion_real`. |
| 9 | `fn_proximas_entregas` | Función | `p_texto TEXT` (vacío = todos) | Hoy es `SELECT` sobre `v_prestamos_activos` con `estado_prestamo = 'ACTIVO'`. Columnas: `id_prestamo, lector, libro, codigo_barras, fecha_prestamo, fecha_entrega_esperada, estado_prestamo, matricula, id_ejemplar`. Filtra por matrícula o lector con `ILIKE`. Orden: `fecha_entrega_esperada ASC`. |
| 10 | `fn_prestamos_vencidos` | Función | `p_texto TEXT` | Sobre `v_prestamos_vencidos`. Columnas: `id_prestamo, matricula, lector, correo, libro, codigo_barras, fecha_prestamo, fecha_entrega_esperada, estado_prestamo, id_ejemplar`. Mismo filtro y orden que la 9. |
| 11 | `fn_historial_prestamos` | Función | `p_texto TEXT` | Sobre `v_historial_prestamos`. Columnas: `id_prestamo, matricula, lector, libro, codigo_barras, fecha_prestamo, fecha_entrega_esperada, fecha_devolucion_real, estado`. Mismo filtro. Orden: `fecha_prestamo DESC`. |
| 12 | `fn_listar_prestamos_recientes` | Función | `p_texto TEXT` | Usa `v_detalle_prestamos`. Mismas columnas que la 8, sin filtro de estado, orden `fecha_prestamo DESC`. **Verificar antes de crearla:** `listarMasRecientes` ya no se usa desde ningún controlador. |

Las funciones 9 a 11 solo envuelven vistas existentes; son de prioridad baja si el equipo acepta que las vistas se consulten directo.

## 3. Ejemplares (`EjemplarDAO`)

Columnas comunes de las funciones 13 y 14: `id_ejemplar, codigo_barras, fecha_adquisicion, id_libro, titulo, isbn, id_ubicacion, pasillo, estante, fila, id_estado_ejemplar, nombre_estado_ejemplar`.

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 13 | `fn_listar_ejemplares_por_libro` | Función | `p_id_libro INT` | Usa `v_ejemplares_detalle`. Ejemplares del libro, orden `codigo_barras`. |
| 14 | `fn_buscar_ejemplar_por_codigo` | Función | `p_codigo_barras TEXT` | Usa `v_ejemplares_detalle`. Cero o una fila. |
| 15 | `fn_contar_disponibles` | Función | `p_id_libro INT` | Entero: `copias_disponibles` de `v_catalogo_libros`, o 0 si no existe. |
| 16 | `fn_siguiente_codigo_barras` | Función | — | Texto con formato `BC-001`, `BC-002`… (consecutivo global). Se recomienda una secuencia para evitar duplicados cuando dos equipos registran a la vez. |
| 17 | `sp_registrar_ejemplar` | Procedimiento | `p_id_libro, p_id_ubicacion, p_codigo_barras, p_fecha_adquisicion` | `OUT p_id_ejemplar INT`. El estado inicial siempre es `DISPONIBLE`. |
| 18 | `sp_cambiar_estado_ejemplar` | Procedimiento | `p_id_ejemplar, p_nombre_estado` | Cambia el estado. Falla si el nombre no existe. |
| 19 | `sp_actualizar_ejemplar` | Procedimiento | `p_id_ejemplar, p_nombre_estado, p_fecha_adquisicion` | Actualiza estado y fecha de adquisición. |
| 20 | `fn_ejemplar_tiene_historial` | Función | `p_id_ejemplar` | `BOOLEAN`: existe en `detalle_prestamo` o en `historial_ubicaciones`. |
| 21 | `sp_eliminar_ejemplar` | Procedimiento | `p_id_ejemplar` | Borra el ejemplar. |
| 22 | `sp_reubicar_libro` | Procedimiento | `p_id_libro, p_id_ubicacion` | Mueve **todos** los ejemplares del libro a la nueva ubicación (RN-62). El trigger `fn_registro_ubicacion` ya guarda el historial, pero tiene el administrador fijo en `1`; conviene recibir `p_id_admin` y `p_motivo` y pasarlos al historial. |

## 4. Libros (`LibroDAO`)

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 23 | `fn_buscar_catalogo` | Función | `p_texto TEXT` | Sobre `v_catalogo_libros`. Columnas: `id_libro, isbn, titulo, editorial, portada, autores, copias_disponibles`. Busca por título, ISBN o autores con `ILIKE`, orden `titulo`. |
| 24 | `fn_buscar_inventario` | Función | `p_texto TEXT` | Un renglón por libro: `id_libro, titulo, isbn, total, disponibles, prestados`. Cuenta ejemplares por estado (`DISPONIBLE`, `PRESTADO`). Busca por título, ISBN o código de barras de algún ejemplar. Orden `titulo`. |
| 25 | `fn_buscar_libro_por_id` | Función | `p_id_libro` | Usa `v_libros_detalle`. Cero o una fila: `id_libro, isbn, titulo, editorial, anio_publicacion, descripcion, portada`. |
| 26 | `fn_buscar_libro_por_isbn` | Función | `p_isbn TEXT` | Usa `v_libros_detalle`. Mismas columnas que la 25. |
| 27 | `fn_listar_autores_libro` | Función | `p_id_libro` | `id_autor, nombre`, orden por nombre. |
| 28 | `sp_registrar_libro` | Procedimiento | `p_isbn, p_titulo, p_editorial, p_anio_publicacion, p_descripcion, p_portada, p_autores TEXT[]` | `OUT p_id_libro INT`. **En una sola transacción**: inserta el libro, crea los autores que no existan (nombre sin importar mayúsculas), los relaciona en `libro_autor` y guarda la portada si viene. Hoy los autores se crean fuera de la transacción del libro; este procedimiento lo corrige. |
| 29 | `sp_actualizar_libro` | Procedimiento | `p_id_libro, p_isbn, p_titulo, p_editorial, p_anio_publicacion, p_descripcion, p_portada` | Actualiza los datos. Si `p_portada` no es nulo, inserta o reemplaza la portada (`ON CONFLICT`). |
| 30 | `sp_eliminar_libro` | Procedimiento | `p_id_libro` | Borra el libro; sus autores y su portada se borran solos. Debe fallar si el libro tiene ejemplares. |

## 5. Autores (`AutorDAO`)

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 31 | `fn_buscar_autores` | Función | `p_texto TEXT` | `id_autor, nombre`, filtro `ILIKE`, orden `nombre`. |
| 32 | `sp_registrar_autor` | Procedimiento | `p_nombre TEXT` | `OUT p_id_autor INT`. Si ya existe un autor con ese nombre (sin importar mayúsculas) devuelve su id; si no, lo crea. Reemplaza `buscarPorNombre` + `insertar`. |

## 6. Ubicaciones (`UbicacionDAO`)

| # | Nombre | Tipo | Parámetros de entrada | Debe devolver / hacer |
|---|---|---|---|---|
| 33 | `sp_buscar_o_crear_ubicacion` | Procedimiento | `p_pasillo, p_estante, p_fila` | `OUT p_id_ubicacion INT`. Devuelve la ubicación si existe o la crea. |
| 34 | `fn_ubicacion_de_libro` | Función | `p_id_libro` | Cero o una fila: `id_ubicacion, pasillo, estante, fila`. Todos los ejemplares de un libro comparten ubicación (RN-50). |

## 7. Sin cambios

`HistorialUbicacionDAO.listar` ya consulta solo la vista `v_ubicaciones`. `UsuarioDAO.listarBloqueados` usa solo `v_alumnos_bloqueados` y hoy no lo invoca ningún controlador.

---

## Resumen para el encargado de la base

- **Vistas nuevas (4):** `v_usuarios_rol`, `v_ejemplares_detalle`, `v_libros_detalle` y `v_detalle_prestamos`. Se crean primero, porque las funciones dependen de ellas.
- **Procedimientos nuevos (14):** 4, 5, 6, 7, 17, 18, 19, 21, 22, 28, 29, 30, 32 y 33.
- **Funciones nuevas (20):** 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 23, 24, 25, 26, 27, 31 y 34.
- **Prioridad alta:** 7 (hoy un alumno bloqueado no se puede desbloquear), 22 (administrador fijo en el historial) y 28 (transacción completa del libro).
- **Prioridad baja:** 9, 10, 11 y 12, porque solo envuelven vistas.
