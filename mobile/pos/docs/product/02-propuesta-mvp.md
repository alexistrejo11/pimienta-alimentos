# Propuesta de MVP

## Resultado esperado

Entregar una caja Android capaz de consultar un catálogo local, construir y cobrar ventas localmente, registrar excepciones y sincronizar las ventas de manera segura cuando haya conectividad.

## Experiencia de caja

La caja usa una única pantalla dividida:

- **Zona de catálogo:** búsqueda rápida y cuadrícula de productos, incluidos productos que no tienen código de barras.
- **Zona de venta:** carrito activo y controles de pago visibles sin abrir un modal.

El lector debe estar listo para recibir lecturas de forma continua. Para pagos habituales, el cajero agrega los productos y confirma el pago desde la misma pantalla. Se contemplan controles de efectivo exacto, billetes rápidos y tarjeta; el detalle de los medios de pago se definirá antes de implementar.

## Operación sin conexión

La tablet confirma una venta en almacenamiento local mediante una operación única y completa. La ausencia de red no impide cobrar.

Después de confirmar el cobro, la venta queda pendiente de envío. Cuando exista conectividad, el sistema la enviará al backend sin volver a cobrar ni modificarla. Cada venta tendrá un identificador único global para que repetir un envío no cree ventas duplicadas.

Una venta ya cobrada es un hecho contable inmutable: el backend no puede descartarla, corregirla automáticamente ni solicitar que la tablet cambie sus importes. Si una regla administrativa o inconsistencia de negocio impide procesarla de forma normal, el backend la conserva con estado **requiere revisión** y acusa recepción final a la tablet. La tablet marca la venta como enviada con incidencia y deja de reintentarlo; el dinero, la venta y el Corte Z local se mantienen intactos para revisión administrativa.

Solo un superadmin puede resolver una venta marcada como requiere revisión desde la web central. Un manager no puede modificarla y la tablet no puede editar, reenviar con cambios ni eliminar la venta ya cobrada. La resolución debe conservar la venta original y dejar una decisión administrativa auditable.

El superadmin dispone de dos formas de resolución:

- **Clasificar y aceptar:** reconoce el ingreso tal como lo reportó la tablet y agrega una categoría o etiqueta de auditoría, por ejemplo una discrepancia de precio o ajuste por monto abierto. El importe se incorpora a los ingresos globales sin cambiar la transacción original.
- **Vincular a producto o movimiento correcto:** asocia posteriormente la venta conflictiva a un producto activo del catálogo maestro. Esto crea el movimiento de inventario y corrige los reportes de rotación correspondientes, sin modificar el ticket físico, el importe cobrado ni los datos originales enviados por caja.

Cada resolución exige una nota explicativa y registra superadmin, fecha/hora y acción aplicada. La venta original se conserva como versión enviada por la caja.

Los fallos técnicos temporales —por ejemplo, ausencia de red, tiempo de espera o servidor no disponible— son diferentes: la venta permanece en cola y se reintenta de forma segura con su mismo UUID. Solo una respuesta final de recepción normal, duplicado ya procesado o requiere revisión detiene los reintentos.

## Alcance de sede

El MVP opera una única sede. No obstante, cada tablet, turno, venta, movimiento de inventario, corte, usuario autorizado y reporte consolidado debe pertenecer a una sede concreta. No se mezclan catálogo operativo, inventario ni reportes de sedes distintas.

La interfaz inicial no necesita permitir elegir sede ni transferir mercancía entre sedes. Esta separación prepara una expansión futura sin cambiar el significado de las transacciones ya registradas. El nombre técnico definitivo de la entidad se alineará con el modelo existente del backend —por ejemplo, `Headquarter`, `Project` o `Sucursal`— antes de diseñar contratos e implementación.

### Catálogo e inventario multisede

El catálogo maestro es global: la identidad y datos base del producto —nombre, descripción, código, categoría e imagen futura— se definen una sola vez en la web central.

Cada sede mantiene de forma independiente su inventario, disponibilidad para venta y precio efectivo. Un producto puede estar disponible en una sede y no en otra. Tiene un precio base maestro, y cada sede puede definir un precio local; si no existe ese ajuste, hereda el precio base. Las ventas conservan siempre el precio capturado, no el precio que resulte después de cambios globales o locales.

En el MVP, la única sede usa por defecto los precios base. La tablet recibe únicamente el catálogo operativo de su sede, incluido su precio efectivo y disponibilidad, para que añadir una segunda sede no requiera cambiar el comportamiento local de caja.

El catálogo disponible en caja será una copia local actualizable. Los cambios remotos de catálogo se aplican posteriormente y no alteran ventas ya cerradas.

## Inventario y disponibilidad

El POS separa dos decisiones:

1. **Política global de venta:** una configuración de operación define si la caja puede vender cuando no hay existencia disponible (`venta sin stock`) o si debe respetar la disponibilidad registrada (`venta con stock`). Esta política puede variar según la necesidad administrativa del negocio.
2. **Regla por producto:** cada producto indica si su existencia se controla. Los artículos empaquetados o contables por unidad, como papas y refrescos, pueden usar control de inventario. Los alimentos preparados en el momento pueden marcarse como no controlados por inventario.

Un producto preparado no se bloquea por falta de stock, incluso si la operación general está configurada para venta con stock. Su disponibilidad depende de la operación física de la cafetería y no de una cantidad exacta registrada en el POS.

Por lo tanto, el sistema debe distinguir al menos estos conceptos de negocio:

- **Producto con inventario controlado:** artículo cuantificable, normalmente por pieza, cuya existencia puede consultarse y actualizarse.
- **Producto sin inventario controlado:** artículo preparado o de cuantificación no confiable; se vende sin validar una existencia exacta.
- **Política de venta sin stock:** permite finalizar una venta de un producto controlado aunque su existencia sea cero o insuficiente, dejando el ajuste para control posterior.
- **Política de venta con stock:** muestra la disponibilidad de los productos controlados, pero no bloquea por defecto una venta al llegar a cero.

En venta con stock, si la existencia local de un producto controlado es cero o negativa, el POS muestra una alerta visual clara en el catálogo y/o carrito. El cajero puede continuar cobrando para no detener la fila. Cada venta realizada en esa condición queda marcada para auditoría.

La web central puede configurar un límite máximo de inventario negativo por operación. Si agregar un artículo supera dicho límite, la tablet bloquea únicamente esa adición y solicita el PIN de un manager o superadmin para autorizar el sobregiro. La autorización queda auditada con producto, cantidad, saldo previo y resultante, responsable y fecha. Los productos sin inventario controlado no participan en este límite.

Un producto también puede estar marcado explícitamente como **no disponible** desde la web central. Esta decisión administrativa es distinta de tener saldo cero o negativo. Tras sincronizarla, el producto permanece visible en el grid con apariencia deshabilitada y una indicación de agotado/no disponible, pero no puede agregarse al carrito; el POS muestra un mensaje explicando que administración lo marcó como no disponible.

Un manager o superadmin puede forzar la adición de un producto no disponible mediante PIN cuando existe disponibilidad física. El override se limita a la venta actual y queda auditado. No habilita permanentemente el producto en la tablet ni altera el estado maestro de disponibilidad; cualquier cambio persistente se realiza desde la web central.

Si un producto ya estaba en un carrito activo cuando la tablet recibe su marca de no disponible, la línea permanece y puede cobrarse normalmente. El POS muestra un aviso informativo en la línea para indicar que el catálogo se actualizó. No se requiere PIN ni se bloquea la venta en curso.

Después de esa actualización, el cajero no puede aumentar la cantidad de esa línea ni agregar el producto a otro carrito sin aplicar el override de manager/superadmin. Así se respeta el compromiso con el cliente ya atendido sin permitir nuevas ventas inadvertidas de un producto deshabilitado.

### Dos cajas sin conexión y reconciliación

Se permite vender en negativo. Por tanto, una existencia local insuficiente o desactualizada no invalida una venta que ya fue cobrada cuando la política de operación permite continuar.

Cada tablet conserva una copia local del saldo conocido para informar a la cajera, pero **no sincroniza ese saldo como una orden para reemplazar el inventario del servidor**. Al recuperar red, sincroniza los hechos ocurridos: cada venta y la cantidad de cada producto controlado que se vendió.

El servidor registra y aplica todos los movimientos de venta recibidos una sola vez. Con dos tablets que partieron de 10 refrescos y vendieron 6 cada una sin conexión, el resultado central es:

```text
Saldo inicial:                  10
Venta recibida desde Tablet A:  -6
Venta recibida desde Tablet B:  -6
Saldo central resultante:       -2
```

El saldo negativo es una incidencia de conciliación, no una causa para borrar, rechazar ni duplicar ventas reales. El proceso posterior revisará la diferencia física, posibles errores de captura, mermas o reposiciones y registrará el ajuste que corresponda. Una vez sincronizado, las tablets descargan el saldo central actualizado.

Para evitar duplicados durante reintentos, cada venta debe tener un identificador único global y el servidor debe reconocer cuando ya procesó ese mismo movimiento.

## Catálogo y selección de productos

### Información base del producto

Para el MVP, un producto cuenta con identificador, nombre, código opcional, categoría simple, precio de venta y costo de adquisición. La categoría es texto, por ejemplo `Desayunos` o `Bebidas`; no requiere todavía una estructura relacional independiente.

El precio de venta es el importe final al público: incluye IVA. El POS no calcula, desglosa ni muestra impuestos por separado en la pantalla de caja ni en el ticket del MVP.

La facturación electrónica queda fuera de alcance. El ticket impreso por el POS es un comprobante de compra operativo y no una factura fiscal.

Un producto también declara si controla inventario. Si lo controla, tiene una cantidad disponible y un umbral mínimo de alerta. Si no lo controla —por ejemplo, un alimento preparado al momento— su cantidad de stock no aplica y no debe representarse artificialmente como cero.

La unidad de venta puede ser pieza o peso/granel. Para productos por peso, el precio se expresa por kilogramo y la cantidad vendida se registra con la precisión necesaria. No habrá báscula conectada al POS en el MVP: los artículos llegarán etiquetados y el lector obtendrá de esa etiqueta el producto y su peso para calcular el importe.

La etiqueta contiene directamente la identificación del producto y el peso. Al escanearla, el POS interpreta ambos valores localmente y calcula el importe sin consultar un servicio remoto. El formato exacto de codificación se definirá a partir del proveedor o impresora de etiquetas disponible; no se debe inventar un formato incompatible.

El código representa un código de barras o una clave interna y es único entre productos. Un producto sin código físico puede no tener código o recibir una clave interna autogenerada, siempre sin duplicar una existente. Un intento administrativo de registrar un código ya usado debe rechazarse; en el backend se representa como conflicto (`HTTP 409`).

### Selección rápida en caja

La pantalla única combina tres mecanismos simultáneos:

- **Lectura de código:** al recibir un código de barras, el POS localiza el producto por código y lo agrega directamente al carrito, sin abrir pop-ups.
- **Lectura de etiqueta por peso:** al recibir una etiqueta de producto a granel, el POS identifica el producto y el peso contenidos en la etiqueta, calcula el importe con el precio por kilogramo y agrega esa línea al carrito.
- **Cuadrícula por categoría:** categorías visibles como pestañas y botones grandes para productos preparados o de venta directa que normalmente no se escanean.
- **Búsqueda por texto:** filtrado inmediato por nombre para artículos poco frecuentes.

### Fuera de alcance inicial

No se incluyen variantes, tamaños, extras, combos, imágenes pesadas embebidas ni una estructura relacional compleja de categorías. Los descuentos por producto, promociones, cupones y listas de precios especiales tampoco forman parte del MVP.

## Administración local y administración central

### Panel administrativo en tablet

Cada tablet dispone de un panel administrativo local, disponible sin conexión, para supervisar la operación de esa caja. Consulta su propia información local y permite al manager revisar ventas del día, historial de tickets originados en esa tablet, cancelaciones, aperturas y cortes de caja, así como registrar mermas.

### Mermas

El manager puede registrar mermas durante el turno, en el momento en que ocurren. Cada merma exige producto, cantidad y motivo; los motivos pueden incluir producto caducado, accidente o caída, error de preparación y prueba de calidad.

Al registrarse, una merma de un producto con inventario controlado descuenta inmediatamente la existencia local de la tablet y crea un evento pendiente de sincronización. Es un movimiento operativo auditable, no una edición directa ni silenciosa del saldo maestro. Debe conservar producto, cantidad, responsable, fecha y motivo.

Si el producto no controla inventario, la merma puede conservarse como registro operativo, pero no modifica una cantidad de stock que no aplica. Mientras dos tablets estén desconectadas, la otra tablet puede mostrar una existencia desactualizada hasta recibir el movimiento desde el servidor.

En el cierre de turno, el POS muestra el resumen de mermas registradas durante el día para auditoría y validación del manager. El cierre no crea nuevamente los movimientos de merma ni depende de que exista red.

### Reposiciones operativas

Un manager autenticado puede registrar en la tablet entradas de mercancía o reabastecimientos de emergencia durante el turno. La entrada aumenta de inmediato la existencia local del producto con inventario controlado y crea un movimiento auditable pendiente de sincronización; no reemplaza silenciosamente un saldo.

Cada entrada debe identificar producto, cantidad, responsable, fecha y motivo o referencia de reabastecimiento. Mientras las tablets estén desconectadas, el aumento solo será visible de inmediato en la tablet que lo registró; las demás lo conocerán después de la sincronización central.

Los conteos físicos periódicos, ajustes masivos y compras a proveedores se gestionan exclusivamente en la web central por Superadmin. Sus resultados se distribuyen después a las tablets como actualización del inventario maestro.

El historial local no pretende ser un reporte consolidado de todas las tablets mientras no exista red. La información consolidada pertenece al backend central.

### Catálogo e inventario maestro

La creación y edición de productos, precios y categorías se realiza en la aplicación web administrativa conectada al backend central. El inventario maestro también se administra centralmente.

La tablet no modifica directamente estos datos maestros. Puede ofrecer un acceso a la aplicación web —por ejemplo, como módulo o WebView— únicamente cuando haya red. Después de un cambio central, las tablets descargan la actualización y refrescan su catálogo local sin alterar las ventas ya confirmadas.

La administración web es la fuente de verdad para catálogo e inventario. La tablet es la fuente de verdad temporal para los hechos generados en caja que todavía están pendientes de sincronización.

### Precio en una venta activa

Al agregar un producto al carrito, el POS captura explícitamente su precio de venta en esa línea. El precio permanece estable aunque el catálogo local o central se actualice mientras el cliente está siendo atendido. Al confirmar la venta, ese precio capturado se conserva como parte del historial inmutable de la venta.

La línea completa conserva su precio capturado aunque su cantidad aumente o disminuya durante esa venta. Si se elimina una línea o se vacía/cancela el carrito y posteriormente se agrega de nuevo el producto, la nueva línea toma el precio vigente del catálogo local en ese momento. Una venta nueva también usa los precios vigentes.

### Vigencia del catálogo sin conexión

La tablet puede cobrar de forma indefinida con su último catálogo descargado; una falta de red nunca detiene por sí sola una venta en curso. El POS muestra el tiempo desde la última sincronización exitosa y escala sus avisos:

- **De 0 a 24 horas:** operación normal, con indicador discreto de modo offline cuando corresponda.
- **De 24 a 72 horas:** al abrir turno o iniciar la aplicación, se muestra una advertencia al cajero indicando cuántas horas lleva el catálogo sin actualizar. La venta continúa sin autorización adicional.
- **Más de 72 horas:** al abrir un nuevo turno se muestra una alerta destacada de catálogo posiblemente desactualizado. Un manager debe ingresar su PIN para aceptar operar ese turno con el catálogo local disponible.

La validación por catálogo antiguo se registra con el turno y no interrumpe una venta ni un turno que ya estuviera activo. La marca de última sincronización debe referirse a una sincronización exitosa, no solo a una conexión de red detectada.

## Excepción: monto abierto o producto genérico

Cuando el artículo no exista en el catálogo local, la caja podrá registrar una línea de monto abierto o producto genérico y continuar con el cobro. Esta línea debe quedar identificada como excepción para conciliación posterior.

Para agregarla, el cajero captura un monto mayor a cero, selecciona una categoría de un catálogo predefinido y escribe una descripción o motivo de al menos cuatro caracteres. La categoría permite incluir la venta en reportes; la descripción aparece en el ticket y en el historial. Cada línea de monto abierto requiere la autorización inmediata por PIN de un manager o superadmin.

Como mínimo, la excepción conserva:

- descripción capturada por el cajero;
- categoría seleccionada;
- monto cobrado;
- usuario responsable;
- usuario que autorizó;
- fecha y terminal donde se originó;
- motivo, cuando la política de operación lo exija;
- estado de conciliación.

La excepción no está ligada a un producto maestro, no descuenta inventario automáticamente y no altera el stock de un artículo existente. El proceso de conciliación puede clasificarla posteriormente si corresponde.

## Pagos y caja

### Medios de pago del MVP

El MVP admite efectivo, tarjeta mediante una terminal física externa de Mercado Pago y pagos mixtos de efectivo más tarjeta. Transferencias y vales quedan fuera de alcance inicial.

La terminal de Mercado Pago opera de forma independiente del POS. La cajera captura el monto en la terminal y, solo después de ver un cobro aprobado, confirma el pago con tarjeta en el POS. En esta fase no se integra una API ni SDK de Mercado Pago; por tanto, el POS registra el medio como `TARJETA_EXTERNA_MP`, pero no puede verificar automáticamente la aprobación con Mercado Pago.

Para hacer auditable la confirmación manual, el POS debe registrar al menos el importe, cajero, fecha y terminal. Si la terminal muestra una referencia u últimos dígitos, podrán capturarse como dato opcional de respaldo sin detener la fila.

### Confirmación de venta

Una venta puede estar en preparación o en intento de pago, pero solo queda **confirmada** cuando la cajera finaliza un cobro exitoso en el POS. En ese momento se registran atómicamente la venta, sus pagos, los movimientos de inventario que apliquen y el evento pendiente de sincronización.

Si la tarjeta es declinada, la terminal falla o el cliente cambia de decisión, la cajera puede cancelar el intento de pago o sustituirlo por efectivo sin perder los artículos del carrito. La cancelación del intento no crea una venta ni descuenta inventario.

Durante un intento de pago, el carrito queda protegido contra una segunda confirmación accidental. Al cancelar el intento vuelve a estar editable.

### Pagos mixtos

Un pago mixto se registra como dos o más componentes de pago asociados a la misma venta. La suma aplicada debe cubrir exactamente el total de la venta para poder confirmarla.

El cambio solo corresponde a efectivo entregado por encima de la porción en efectivo que se aplicará a la venta. Una tarjeta no genera cambio en efectivo.

### Apertura y cierre de turno

Cada cajero abre un turno declarando el fondo inicial de efectivo. Al cerrar, el POS calcula:

```text
Efectivo esperado = fondo inicial + efectivo cobrado - cambio entregado - devoluciones de efectivo autorizadas
```

El cajero captura el conteo físico y el POS calcula la diferencia de caja para auditoría. El total cobrado por tarjeta se informa por separado y no forma parte del efectivo esperado.

Los retiros de efectivo, ingresos adicionales y devoluciones de efectivo no son parte del MVP hasta definir sus reglas; mientras no existan, deben estar prohibidos o claramente fuera del flujo de caja.

Cada tablet admite un único turno activo. No se puede abrir otro hasta cerrar el actual. Los cambios de cajero se realizan en lapsos sin atención: se completa el corte, se libera la tablet y el siguiente cajero abre un nuevo turno con su fondo inicial declarado.

El cierre usa doble control. Primero, el cajero realiza un conteo ciego: captura el desglose físico del efectivo sin que el POS muestre por adelantado el efectivo esperado. Una vez enviado el conteo, el sistema calcula el esperado y la diferencia. Después, un manager revisa ventas, mermas, cancelaciones y diferencia de caja, e ingresa su PIN para aprobar y sellar oficialmente el cierre.

La aprobación del manager registra de manera separada el cajero que preparó el corte, el manager que lo validó, los importes declarados, la diferencia y las marcas de tiempo. El PIN de aprobación se verifica localmente contra una credencial almacenada de forma segura, por lo que el cierre funciona incluso sin conexión.

Antes de la aprobación final, el manager puede seleccionar **Corregir conteo** si identifica un error. El corte vuelve al formulario de conteo ciego para que el cajero capture nuevamente el desglose físico. Cada conteo enviado, rechazo y reintento queda en la bitácora local del turno, con usuario y marca de tiempo. Solo el corte aprobado mediante PIN queda cerrado y congelado; los borradores previos no son el corte oficial.

El cierre de turno funciona sin conexión. Si hay ventas, mermas, cancelaciones o el propio corte pendientes de sincronización, el sistema permite cerrar localmente y conserva esos hechos con su cajero, tablet y marca de tiempo. La sincronización posterior los envía sin depender de que el mismo turno continúe activo; un turno nuevo puede comenzar mientras la cola de eventos anteriores espera conectividad.

Los eventos de una misma tablet conservan su orden causal al sincronizarse: las operaciones del turno se reciben antes de su cierre, aunque la conexión se recupere más tarde.

## Usuarios, autenticación y autorizaciones

### Autenticación operativa

La tablet muestra una selección de perfil y pide un PIN de cuatro dígitos para iniciar o cambiar de usuario rápidamente. La sesión activa identifica quién realizó cada acción de caja. El PIN se conserva de forma segura; no se almacena ni se transmite como texto legible. Dado que es un PIN corto, la tablet debe limitar intentos fallidos y aplicar bloqueo temporal para reducir ataques por prueba repetida.

Los usuarios, sus roles y sus credenciales se crean, modifican o dan de baja exclusivamente en la aplicación web central. Las tablets descargan una copia local de los usuarios autorizados y verificadores seguros de sus PINs para poder autenticar a cajeros y ejecutar autorizaciones de supervisor sin red. No se permite crear usuarios ni cambiar PINs desde la tablet.

Cuando la administración central cambia un PIN, un rol o el estado de un usuario, la tablet recibe el cambio en su siguiente sincronización. No puede revocarse de forma instantánea una credencial en una tablet que permanece desconectada; este es un límite operativo aceptado de la autenticación offline.

### Registro y autorización de tablets

Antes de operar, cada tablet se registra y autoriza desde la web central por un superadmin. En su primera instalación, la aplicación genera una identidad propia del dispositivo y solicita un código o token de vinculación. La identidad y la credencial de instalación permiten reconocer a la tablet sin depender únicamente de un identificador de hardware cambiante.

Una tablet no autorizada no puede descargar catálogo ni usuarios, ni enviar ventas, mermas o cierres. El backend rechaza cualquier sincronización de una identidad de dispositivo desconocida o revocada.

Si una tablet se extravía, se roba o se retira, un superadmin revoca su acceso en la web central. Al siguiente intento de conexión, la tablet revocada pierde la sesión y no puede continuar sincronizando. Si permanece completamente offline, no puede conocer aún la revocación; esta limitación se deriva de la operación offline y debe mitigarse con resguardo físico del dispositivo y bloqueo local.

### Roles

- **Cajero:** puede abrir turno, escanear o seleccionar productos, usar monto abierto y cobrar. No puede aplicar descuentos, cortesías ni cancelaciones por cuenta propia.
- **Manager (encargado o staff):** puede cerrar y realizar corte de caja, gestionar el catálogo disponible localmente, consultar reportes e historial y autorizar acciones restringidas.
- **Superadmin:** tiene acceso total al POS y backend; administra usuarios y puede cambiar configuraciones críticas, incluido el modo operativo de inventario. También puede solicitar limpieza local bajo las restricciones de seguridad descritas abajo.

### Autorización en el punto de venta

Cuando un cajero solicite un descuento, cortesía o cancelación, la venta se mantiene visible y sin confirmar. Un manager o superadmin ingresa su PIN en ese momento para autorizar la acción. La autorización registra quién la concedió, qué acción aprobó, el monto o venta afectada, la fecha y el motivo cuando aplique.

La autorización no cambia la identidad de la sesión del cajero: el responsable de cobrar y el responsable de autorizar quedan registrados por separado.

### Descuentos del MVP

El MVP permite un descuento único sobre el total de una venta, ya sea parcial o del 100 %. Siempre requiere autorización en sitio de un manager o superadmin mediante PIN. Debe registrar el importe descontado, el responsable que lo autorizó y el motivo cuando la operación lo solicite.

No se incluyen descuentos por producto, reglas de promoción, cupones ni listas de precios especiales.

### Operaciones críticas

La limpieza de datos locales es una operación destructiva exclusiva de superadmin. No debe ejecutarse si existen ventas, movimientos o cierres pendientes de sincronización; primero deben sincronizarse o resolverse mediante un procedimiento administrativo explícito. Esta protección evita la pérdida de ventas reales.

## Postventa y cancelaciones

### Antes de confirmar el cobro

El cajero puede eliminar artículos o vaciar el carrito sin autorización mientras la venta no esté confirmada. Esta acción no crea una venta, un pago ni un movimiento de inventario.

### Después de confirmar el cobro

En el MVP, solo puede cancelarse por completo una venta pagada exclusivamente en efectivo. La cancelación requiere autorización en pantalla mediante el PIN de un manager o superadmin y solo está disponible para tickets del turno en curso.

Al cancelar una venta en efectivo, el sistema conserva el ticket original como evidencia, registra quién autorizó y cuándo, revierte los movimientos de inventario de los artículos controlados y descuenta el efectivo devuelto del esperado en caja. No se permiten cancelaciones parciales en esta fase.

Las ventas pagadas mediante tarjeta externa de Mercado Pago o con pago mixto no se pueden cancelar desde el POS MVP una vez confirmadas. La aplicación no conoce todavía el proceso de reembolso de la terminal externa y no debe simularlo ni compensarlo con efectivo. Cualquier incidencia de este tipo se resuelve por el procedimiento externo vigente hasta que se diseñe una integración o proceso formal de devoluciones.

## Tickets e impresión

Cada venta confirmada genera localmente dos identificadores. El **folio visible** es consecutivo dentro de la combinación de tablet y turno, con formato equivalente a `T1-104-0023`: código autorizado de tablet, identificador de turno y consecutivo local. Este es el folio impreso y usado para buscar o cancelar el ticket. El **UUID interno** identifica la venta para trazabilidad técnica e idempotencia de sincronización.

El consecutivo y el UUID se asignan al confirmar la venta dentro de la misma operación local que la registra, por lo que no se reutilizan ni colisionan aunque la tablet esté offline. El código de tablet se asigna durante su enrolamiento central y debe permanecer estable mientras esté autorizada.

Cada venta confirmada dispara automáticamente la impresión de su ticket. El comprobante es obligatorio para todas las transacciones bajo la política del negocio: si no se entrega el ticket, el consumo es gratis.

El ticket incluye fecha, hora, folio único, desglose de productos, total y medios de pago. Al final muestra de forma visible la leyenda: **“Si no te entregamos tu ticket, tu consumo es GRATIS”**.

Una venta ya cobrada sigue siendo válida si la impresora se queda sin papel, se atasca o se desconecta. En ese caso, el POS conserva la venta, marca la impresión como pendiente o fallida, avisa de forma visible a la cajera y permite reintentarla. La impresión no se pierde al cerrar o reiniciar la aplicación.

La pantalla de venta y el historial de tickets del turno permiten reimprimir un comprobante. Las reimpresiones se registran para auditoría y deben identificarse como duplicado o reimpresión, sin generar otra venta ni otro cobro.

## Reportes del MVP

### Corte de turno local en tablet

Al cierre, la tablet produce un corte de turno local —Corte Z— basado en las operaciones registradas en esa caja. Incluye:

- total de ventas por medio de pago, incluidos efectivo y tarjeta externa;
- efectivo esperado frente a efectivo contado, con faltante o sobrante resultante;
- total de mermas, cancelaciones y descuentos realizados durante el turno.

El corte conserva el resultado y los valores declarados como evidencia del turno. No requiere conexión para calcularse. Si existen eventos pendientes de sincronización, el corte sigue siendo válido localmente y el estado de sincronización debe quedar visible para administración.

### Reportes del panel web central

El panel administrativo central ofrece información consolidada de las tablets ya sincronizadas:

- ventas por producto, con cantidad e importe para identificar productos más vendidos;
- ventas históricas filtrables por día, semana, mes, rango de fechas o turno;
- mermas y cancelaciones agrupadas por motivo;
- historial de cortes de caja, incluidos faltantes y sobrantes por turno.

Los reportes centrales deben indicar o considerar el retraso de sincronización cuando existan eventos pendientes en alguna tablet; un reporte consolidado no puede asumir que incluye ventas que aún permanecen offline.

## Límites explícitos del MVP

- No se garantiza inventario global exacto entre las dos tablets mientras ambas estén desconectadas. La diferencia se acepta temporalmente y se refleja en el saldo central —incluso negativo— cuando las ventas se sincronizan.
- La aplicación no autoriza por sí misma pagos bancarios sin conexión. La integración con una terminal de pago depende de las capacidades del proveedor adquirente.
- No hay devoluciones parciales ni cancelación postventa desde el POS para tarjeta externa o pagos mixtos.
- La impresión es un resultado operativo importante, pero una falla de impresión no invalida una venta ya confirmada; debe poder reintentarse.

## Criterios de éxito iniciales

- Se puede cobrar con el servicio remoto y la LAN desconectados.
- Una venta habitual no requiere navegar entre pantallas ni abrir modales.
- Un producto faltante no detiene la atención.
- Cada venta confirmada puede identificarse, reenviarse sin duplicarse y auditarse.
- La pérdida temporal de un periférico no borra ni corrompe una venta en curso o confirmada.
