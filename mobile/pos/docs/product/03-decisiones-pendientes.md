# Decisiones pendientes de producto

Estas decisiones afectan reglas de negocio y alcance. Deben acordarse antes de convertir esta propuesta en especificaciones detalladas.

## Prioridad 1: necesarias para definir el MVP

1. **Inventario entre tablets:** decidido. Se acepta la sobreventa eventual; las tablets sincronizan movimientos de venta, no saldos calculados localmente. El servidor aplica los movimientos una sola vez, puede quedar en negativo y crea una incidencia para conciliación.
2. **Pagos:** decidido para el MVP. Se acepta efectivo, tarjeta con terminal externa de Mercado Pago y pagos mixtos. La terminal no se integra por API/SDK; el cajero confirma manualmente el pago aprobado. Transferencias y vales quedan fuera de alcance.
3. **Caja y turnos:** decidido para el MVP. Hay apertura con fondo inicial, cierre, conteo físico y diferencia de caja. Falta definir si después se requerirán retiros, ingresos adicionales y devoluciones de efectivo.
4. **Roles y permisos:** decidido. La autenticación es perfil más PIN de cuatro dígitos. Existen los roles Cajero, Manager y Superadmin. Descuentos, cortesías y cancelaciones solicitados por un cajero requieren autorización in situ de Manager o Superadmin.
5. **Postventa:** decidido para el MVP. Antes del cobro el cajero puede cancelar libremente. Después del cobro, solo un Manager o Superadmin puede cancelar totalmente una venta exclusivamente en efectivo y únicamente dentro del turno en curso. No hay devoluciones parciales ni cancelaciones postventa de tarjeta externa o pagos mixtos.
6. **Productos por peso:** decidido. Se venden por kilogramo y llegan con una etiqueta cuyo código incluye directamente producto y peso; no habrá captura manual ni báscula conectada en el MVP. Falta validar el formato concreto que emite el proveedor o impresora de etiquetas.
7. **Descuentos:** decidido. Se permite un descuento único, parcial o total, sobre el total de la venta. Requiere autorización por PIN de Manager o Superadmin. No hay descuentos por producto, promociones, cupones ni listas de precios especiales.
8. **Administración:** decidido. El panel local offline cubre supervisión de la propia caja, ventas, tickets, cancelaciones, cortes y mermas. Productos, precios, categorías e inventario maestro se gestionan en la aplicación web central y se descargan a las tablets.
9. **Mermas:** decidido. Manager las registra en tiempo real durante el turno, con producto, cantidad y motivo obligatorios. Se descuenta de inmediato el stock local cuando aplique, se sincroniza como movimiento y se resume en el cierre para auditoría.
10. **Tickets:** decidido. La impresión es automática y obligatoria al confirmar cada venta; se permite reimpresión desde venta e historial. Si falla la impresora, la venta se conserva y la impresión queda pendiente para reintento. El ticket lleva folio, fecha/hora, detalle, total, pago y la leyenda del negocio.
11. **Reportes:** decidido. El Corte Z local presenta pagos, arqueo/diferencia, mermas, cancelaciones y descuentos de la caja. La web central consolida ventas por producto y periodo, mermas/cancelaciones por motivo e historial de cortes y diferencias.
12. **Turnos:** decidido. Hay un único turno activo por tablet. Un turno puede cerrarse localmente aunque haya eventos pendientes de sincronización; la cola los conserva y posteriormente respeta el orden de las operaciones y su cierre.
13. **Cierre de turno:** decidido. El cajero hace un conteo ciego y prepara el corte; Manager o Superadmin revisa y aprueba con PIN para sellarlo. La aprobación funciona localmente aun sin red.
14. **Corrección de conteo:** decidido. Antes de la firma, Manager puede devolver el corte al cajero para recaptura ciega. Cada envío, rechazo y reintento queda en la bitácora; solo la aprobación final congela el cierre.
15. **Usuarios offline:** decidido. Usuarios, roles y PINs se gestionan solo en la web central. Las tablets descargan verificadores de PIN para autenticación y autorizaciones locales; revocaciones y cambios se aplican en la siguiente sincronización.
16. **Tablets autorizadas:** decidido. Cada tablet se enrola previamente por Superadmin mediante una identidad local y código/token de vinculación. Solo dispositivos autorizados pueden descargar datos o sincronizar; una revocación se aplica en el siguiente intento de conexión.
17. **Vigencia de catálogo offline:** decidido. La caja nunca se bloquea por falta de sincronización. De 24 a 72 horas se advierte al cajero; después de 72 horas, un Manager debe aprobar con PIN la apertura de cada nuevo turno usando catálogo potencialmente desactualizado.
18. **Precios en carrito:** decidido. Cada línea conserva el precio al momento de agregarse, incluso si cambia su cantidad. El precio vigente solo aplica al crear una línea nueva tras eliminarla o al iniciar otra venta.
19. **Folios de ticket:** decidido. El folio visible combina código de tablet, turno y consecutivo local; cada venta además posee un UUID interno para sincronización idempotente.
20. **Monto abierto:** decidido. Cada línea exige monto, categoría predefinida y descripción de al menos cuatro caracteres, más autorización por PIN de Manager/Superadmin. Aparece en ticket y reportes, pero no afecta inventario.
21. **Impuestos:** decidido. Los precios son finales e incluyen IVA; el POS y ticket MVP no calculan ni desglosan IVA por separado.
22. **Facturación:** decidido. La facturación electrónica está fuera del MVP; el ticket es un comprobante operativo de compra, no una factura fiscal.
23. **Reposiciones e inventario físico:** decidido. Manager puede registrar entradas operativas en su tablet, que se aplican localmente y se sincronizan como movimientos. Conteos físicos, ajustes masivos y compras globales se realizan en la web central por Superadmin.
24. **Venta con stock en cero/negativo:** decidido. La caja advierte pero permite vender y marca la venta para auditoría. Un límite negativo configurable centralmente requiere autorización de Manager/Superadmin solo al excederse.
25. **Producto no disponible:** decidido. Permanece visible pero deshabilitado al sincronizarse la marca central y no se agrega al carrito. Manager/Superadmin puede forzar una sola adición para la venta actual mediante PIN; el estado maestro no cambia desde la tablet.
26. **Producto deshabilitado durante venta:** decidido. Una línea ya agregada puede cobrarse y muestra aviso informativo. Aumentar su cantidad o agregarlo de nuevo queda bloqueado sin override de Manager/Superadmin.
27. **Venta cobrada con incidencia de sincronización:** decidido. El backend no descarta ni corrige automáticamente una venta cobrada. Si hay inconsistencia de negocio, la recibe como `requiere revisión`; la tablet deja de reintentar y conserva toda la evidencia. Solo fallos técnicos temporales se reintentan.
28. **Resolución de incidencias:** decidido. Solo Superadmin puede resolver en la web central una venta marcada como `requiere revisión`; la venta original sigue inmutable y la resolución queda auditada.
29. **Acciones de resolución:** decidido. Superadmin puede clasificar y aceptar el ingreso tal cual, o vincularlo después a un producto/movimiento maestro para afectar inventario y reportes. Ambas rutas requieren nota y bitácora.
30. **Sedes:** decidido. El MVP opera una sola sede, pero toda información operativa debe quedar separada y asociada a una sede para permitir expansión futura. Falta alinear el término y entidad técnica con el backend existente.
31. **Catálogo multisede:** decidido. El producto base es global; inventario, disponibilidad y precio efectivo son propios de cada sede. El precio local hereda el base si no existe override. El MVP recibe solo el catálogo de su única sede.
5. **Catálogo de venta:** ¿hay productos por peso, combos, modificadores, tamaños, extras o recetas; o inicialmente son artículos por unidad con precio fijo?

## Prioridad 2: necesarias para una operación controlada

1. ¿La venta requiere ticket impreso siempre, a solicitud o nunca?
2. ¿Qué debe ocurrir con una venta si falla la impresora después de cobrar?
3. ¿Hay clientes, facturación, cuentas por cobrar o solamente venta de mostrador?
4. ¿Cuánto tiempo puede operar una tablet sin sincronizar antes de requerir atención?
5. ¿Quién revisa y resuelve las ventas de monto abierto, y cuál es su proceso?

## Decisión arquitectónica propuesta, aún no definitiva

Para el cliente Android se propone una aplicación nativa. Es la alternativa que mejor se ajusta al manejo de USB, Bluetooth serial y rendimiento de una pantalla de caja. Esta propuesta no implica iniciar implementación todavía.
