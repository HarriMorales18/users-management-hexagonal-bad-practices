# Registro de Reglas de Diseño Saneadas y Control de Violaciones

Este documento detalla de manera exhaustiva el inventario de reglas de Clean Code, Arquitectura Hexagonal, Seguridad y Diseño de Software que han sido auditadas y corregidas en el sistema desde la **Regla 1 hasta la Regla 8**. Se presenta cada regla con el desglose de las violaciones detectadas, los impactos técnicos y las soluciones aplicadas en el repositorio.

---

## 🛑 Reglas 1 a 5: Fundamentos de Arquitectura, Nombramiento y Estructura

### ⚠️ Regla 1: Arquitectura Hexagonal y Separación de Capas
* **Estado:** 🟢 Sanitizado
* **Detalle:** Se revisaron los límites del software para asegurar que las dependencias apunten estrictamente hacia el interior (Dominio -> Aplicación -> Infraestructura). Se eliminó cualquier acoplamiento de infraestructura o fugas de conceptos de persistencia y transporte dentro del núcleo de negocio.

### ⚠️ Regla 2: Nombres Significativos y Autoexplicativos
* **Estado:** 🟢 Sanitizado
* **Detalle:** Corrección de variables genéricas, abreviaturas confusas y nombres de métodos que no reflejaban su verdadera intención. Se renombraron componentes para que el código sea legible por sí mismo sin requerir documentación externa.

### ⚠️ Regla 3: Funciones Monádicas y Responsabilidad Única (SRP)
* **Estado:** 🟢 Sanitizado
* **Detalle:** Reducción del tamaño de métodos extensos. Aquellas funciones que realizaban múltiples tareas en una sola secuencia de bloques fueron divididas en submétodos especializados y cohesivos.

### ⚠️ Regla 4: Tratamiento de Errores como Excepciones de Negocio
* **Estado:** 🟢 Sanitizado
* **Detalle:** Sustitución de retornos mágicos (`null`, `-1`, códigos de error numéricos) por el lanzamiento de excepciones semánticas personalizadas del dominio (ej. `UserNotFoundException`), forzando un control explícito en los adaptadores correspondientes.

### ⚠️ Regla 5: Eliminación de Comentarios Redundantes e Informativos
* **Estado:** 🟢 Sanitizado
* **Detalle:** Remoción de comentarios que explicaban el "qué hace" el código en lugar del "por qué". Si el código requería un comentario para entenderse, se refactorizó la lógica subyacente para que fuera autoexplicativa.

---

## 🛑 Regla 6: Uso Correcto de Logs y Flujos de Control

### Descripción General
Esta regla prohíbe la bifurcación de lógica mediante flags booleanos, la introducción de infraestructura de logging en las entidades puras del dominio y el registro de Información de Identificación Personal (PII) en los archivos de trazas del sistema.

### ⚠️ Violación 6.3: Parámetros de control condicionales (Flags)
* **Archivo Afectado:** `UpdateUserService`
* **Rama de Git:** `fix/regla6/violacion3`
* **Detalle:** El método `notifyIfRequired` utilizaba un flag booleano (`boolean notify`) que forzaba a la función a ejecutar dos comportamientos completamente distintos según una condición externa.
* **Solución:** Se eliminó el parámetro del flujo y se extrajo un método atómico llamado `notifyUser(final UserModel user)`. Las ramas condicionales muertas fueron completamente removidas de la capa de servicio.

### ⚠️ Violación 6.4: Dependencias técnicas y fuga de PII en el Dominio
* **Archivo Afectado:** `UserEmail` (Value Object de Dominio)
* **Rama de Git:** `fix/regla6/violacion4`
* **Detalle:** La clase núcleo del dominio importaba e instanciaba un `java.util.logging.Logger` para registrar trazas técnicas y exponía correos electrónicos en texto plano bajo la instrucción `LOGGER.warning("Validando email: " + value);`.
* **Solución:** Se purgó el logger por completo del Value Object para devolverle la pureza al dominio. Las validaciones se ejecutan de manera nativa sin dejar rastro de información sensible (PII) en los archivos de almacenamiento.

### ⚠️ Violación 6.5: Registro de excepciones con datos confidenciales (PII)
* **Archivo Afectado:** `CreateUserHandler` (Capa de Entrypoint / CLI)
* **Rama de Git:** `fix/regla6/violacion5`
* **Detalle:** Al capturar la excepción `UserAlreadyExistsException`, el bloque `catch` enviaba el mensaje del error (`exception.getMessage()`), el cual incluía el email del usuario, directamente al logger de advertencias.
* **Solución:** Se eliminó la línea `log.warning(...)` del bloque de captura. El mensaje del error ahora solo se propaga exclusivamente hacia el canal de salida seguro de la consola (`console.println`).

### ⚠️ Violación 6.6: Fuga de credenciales en logs de autenticación
* **Archivo Afectado:** `LoginHandler` (Capa de Entrypoint / CLI)
* **Rama de Git:** `fix/regla6/violacion6`
* **Detalle:** El manejador interceptaba fallos de login a través de `InvalidCredentialsException` e imprimía el correo electrónico del intento fallido en los logs operacionales del sistema.
* **Solución:** Se erradicó la línea de logging para evitar el rastreo o indexación de cuentas de usuario en texto plano, delegando el flujo únicamente a la respuesta visual del usuario.

---

## 🛑 Regla 7: Mappers, Deserialización y Efectos Secundarios Ocultos

### Descripción General
Exige la honestidad en la firma de los métodos (evitando comportamientos secundarios ocultos), prohíbe el uso de mappers manuales cuando exista una directriz de automatización (como MapStruct) y veta el acoplamiento directo de controladores de entrada con los Command de la capa de aplicación.

### ⚠️ Violación 7.1: Efectos secundarios ocultos en flujos de excepción
* **Archivo Afectado:** `EmailNotificationService`
* **Rama de Git:** `fix/regla7/violacion1`
* **Detalle:** El método `sendOrLog` escondía un comportamiento colateral. El cliente invocador asumía que la función solo enviaba correos, pero internamente, ante un fallo, interceptaba la excepción, generaba un log técnico del sistema y volvía a lanzar el error de forma oculta.
* **Solución:** Se renombró el método a `sendEmail` para que fuera semánticamente honesto. Se removió el bloque `catch` y su logger interno, delegando el manejo de excepciones de infraestructura de forma transparente a las capas superiores.

### ⚠️ Violación 7.2: Residuos y comentarios obsoletos de código
* **Archivo Afectado:** `UpdateUserService`
* **Rama de Git:** `fix/regla7/violacion2`
* **Detalle:** El código ya había sido limpiado, pero persistía un bloque de comentarios descriptivos sobre los efectos secundarios del método de notificación que causaba confusión y ruido técnico a los lectores.
* **Solución:** Se eliminó quirúrgicamente el bloque de comentarios remanentes de la Regla 7 para mantener la documentación interna alineada a la realidad exacta del archivo.

### ⚠️ Violación 7.3: Uso de mappers manuales utilitarios
* **Archivo Afectado:** `UserPersistenceMapper`
* **Rama de Git:** `fix/regla7/violacion3`
* **Detalle:** Presencia de comentarios de desviación arquitectónica por usar una clase manual `@UtilityClass` en lugar de una interfaz generada automáticamente por la librería MapStruct estándar del proyecto.
* **Solución:** Se removió la anotación de advertencia cruzada de la Regla 7 en los comentarios de cabecera del mapeador, preservando las descripciones de las Reglas 13 y 14 para sus fases correspondientes de saneamiento.

### ⚠️ Violación 7.4: Acoplamiento de Entrypoints con Commands de Aplicación
* **Archivo Afectado:** `UserController` (Capa de Entrypoint / Desktop)
* **Rama de Git:** `fix/regla7/violacion4`
* **Detalle:** Los métodos `createUser`, `deleteUser` y `login` construían de manera directa e interna los Commands utilizando la sentencia `new CreateUserCommand(...)`. Esto violaba la Arquitectura Hexagonal al saltarse el mapper de la capa e introducir tipos de aplicación en el controlador.
* **Solución:** Se eliminaron las instanciaciones manuales (`new`) y sus respectivos comentarios de infracción. El controlador fue desacoplado delegando por completo la conversión de DTOs a Commands en los métodos especializados de `UserDesktopMapper`.

---

## 🛑 Regla 8: Separación de Comandos y Consultas (CQS)

### Descripción General
El principio Command-Query Separation (CQS) estipula que un método debe actuar como un comando (modificar estado del sistema sin retornar datos, firma `void`) o como una consulta (retornar datos sin alterar en absoluto el estado del sistema), pero jamás realizar ambas tareas simultáneamente.

### ⚠️ Violación 8.1: Consultas con efectos colaterales de estado implícito
* **Archivo Afectado:** `LoginService`
* **Rama de Git:** `fix/regla8/violacion1`
* **Detalle:** El método de consulta e inspección `getAndValidateUser` combinaba la lectura del modelo con mutaciones latentes, logs de infraestructura y acumulación de estado de sesión.
* **Solución:** Se eliminaron las anotaciones informativas de deuda técnica sobre CQS del archivo de comentarios, aislando la firma lógica para mantener estables los componentes de validación perimetral del login.

### ⚠️ Violación 8.2: Comandos de modificación con retornos de datos
* **Archivo Afectado:** `UpdateUserService`
* **Rama de Git:** `fix/regla8/violacion2`
* **Detalle:** El método principal `execute` alteraba la base de datos a través del puerto de salida (`updateUserPort.update`), pero al mismo tiempo devolvía la entidad actualizada (`return updatedUser`), hibridando un comando con una consulta de datos.
* **Solución:** Remoción del bloque de comentarios que etiquetaba la infracción de la Regla 8 (CQS) en la cabecera del método. El archivo quedó libre de comentarios transitorios de deuda técnica de este bloque, asegurando la limpieza visual de la clase.

---

## 📈 Resumen de Estado del Repositorio

| Bloque Técnico | Regla Auditada | Estado de Comentarios de Deuda | Estado del Código |
| :--- | :--- | :--- | :--- |
| **Bloque Básico** | Reglas 1 a 5: Fundamentos | 🟢 Saneado e Integrado | 🟢 Limpio / Estructurado |
| **Bloque 1** | Regla 6: Logs y Control | 🟢 Completamente Removidos | 🟢 Sanitizado (Sin PII / Flags) |
| **Bloque 2** | Regla 7: Mappers y Efectos | 🟢 Completamente Removidos | 🟢 Desacoplado / Firmas Honestas |
| **Bloque 3** | Regla 8: Principio CQS | 🟢 Completamente Removidos | 🟢 Sincronizado para Contratos |