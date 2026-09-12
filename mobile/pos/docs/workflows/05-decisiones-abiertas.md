# Decisiones abiertas antes de implementar

Estas decisiones no bloquean la documentación de los workflows ya definidos, pero sí deben cerrarse antes de construir el flujo o integración correspondiente. Se separan para no convertir supuestos en código.

## Experiencia de caja

### Pago mixto: confirmar alcance primero

El tipo `MIXTO` permanece reservado en el modelo de pagos, pero no está habilitado en el MVP. Antes de diseñar componentes, cambios o una interfaz específica, el cliente debe confirmar que realmente necesita combinar efectivo y tarjeta en una venta. Si se confirma, se definirá como un workflow independiente.

### Cambio de usuario con carrito activo

Propuesta: no permitir cambiar de usuario, bloquear sesión ni entrar a un turno distinto mientras existe un carrito con líneas. El cajero debe cobrarlo o limpiarlo primero. Evita que una venta quede atribuida a otro usuario.

### Bloqueo por inactividad

Falta definir después de cuántos minutos se bloquea la tablet y qué ocurre con un carrito existente. La recomendación es bloquear solo cuando el carrito está vacío; con carrito activo, mostrar una advertencia progresiva antes de bloquear y conservar el borrador local si se decide persistirlo.

### Búsqueda textual

La venta prioriza escaneo y categorías. Falta decidir si la búsqueda por texto abre:

- un modo de búsqueda táctil con teclado alfabético controlado; o
- solo un campo disponible cuando la tablet tenga un teclado físico validado.

No debe dejar un campo de texto permanentemente enfocado durante venta.

## Periféricos y hardware

### Etiquetas por peso

Está decidido que el código contiene producto y peso; falta validar el formato real de la impresora/proveedor antes de implementar el parser.

### Lector e impresora reales

Falta identificar modelos, conexión exacta y protocolo:

- lector USB/serial/SPP o SDK del fabricante;
- impresora térmica de 58 mm, USB o TCP/IP, y conjunto exacto de comandos ESC/POS;
- code page soportada para acentos y `ñ` en español;
- comportamiento ante reconexión;
- compatibilidad con el hub USB-C y Power Delivery.

### Cajón de efectivo

La arquitectura contempla probar el cajón desde el panel de periféricos, pero la acción queda pendiente de validación física: tipo de conector (por ejemplo RJ11/RJ12), pulso requerido y si la impresora puede emitirlo. El botón combinado `Imprimir ticket de prueba + abrir cajón` solo se habilita cuando esa compatibilidad esté confirmada; no se asume que el cable sea Ethernet ni que el cajón pueda abrirse directamente desde la tablet.

## Seguridad y operación

### Recuperación de dispositivo

Falta definir el proceso para reemplazar una tablet dañada con eventos no sincronizados. Debe evitarse borrar o reenrolar un dispositivo si conserva evidencia pendiente.

### Copia de seguridad y retención local

Definir cuánto historial local conserva una tablet, cuándo puede depurarse después de sincronizar y qué respaldo central cubre auditoría. Esto debe respetar la regla de no limpiar evidencia pendiente.

### Actualización obligatoria de aplicación

Definir cómo el backend informa una versión mínima compatible, cuánto tiempo se soportan eventos de versiones anteriores y qué ocurre si una tablet está offline durante una actualización requerida.

## No funcionales para la fase previa a implementación

Antes de codificar, conviene fijar criterios medibles:

- tiempo máximo para agregar un producto localmente;
- tiempo máximo para confirmar una venta local;
- tiempo objetivo de atención de un `PrintJob` cuando el periférico está listo, sin prometer una latencia fija de generación o salida;
- capacidad mínima de eventos/ventas offline;
- versión mínima de Android y tamaños/orientación de tablets soportados;
- estrategia de pruebas de apagado/reinicio durante venta, impresión y sync.

## Regla de priorización

No se necesita cerrar todas estas decisiones para empezar todos los flujos. Se cierran justo antes del corte vertical que las implementa. Por ejemplo, el formato de etiqueta por peso se define antes de integrar ese parser, pero no bloquea el flujo de efectivo por productos de pieza.
