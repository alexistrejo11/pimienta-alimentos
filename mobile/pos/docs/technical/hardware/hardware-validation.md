# Hardware y conexión del POS

Estado: inventario de producción en captura (2026-09-11). Arquitectura de
hub USB-C PD confirmada desde Phase 0; modelos concretos se registran abajo
conforme se confirman.

## Inventario confirmado

### Lector de códigos de barras — confirmado

| Campo | Valor |
|---|---|
| Pieza | Lector de códigos de barras |
| Marca | Shawty |
| Modelo | S0024 |
| Formato | 1D y 2D (incluye QR, DataMatrix, PDF417, etc.) |
| Conexión física prevista | USB (vía hub USB-A) |
| Interfaces declaradas | USB-HID (fábrica), USB-COM / Virtual COM, Bluetooth inalámbrico |
| Drivers (modo HID) | No requiere driver; emula teclado |
| Color | Negro |
| Referencias | [Sanborns](https://www.sanborns.com.mx/producto/660933/lector-de-codigos-de-barras-inalambrico-shawty-1d-2d-lector-color-negro), [Mercado Libre](https://www.mercadolibre.com.mx/lector-de-codigos-de-barras-shawty-s0024-inalambrico-1d-y-2d-usb-con-bluetooth-y-bateria/p/MLM62376358) |

#### Comportamiento de interfaz

- **USB-HID (default de fábrica):** al escanear escribe el código donde esté el
  foco, como un teclado. Sin SDK de fabricante.
- **USB-COM / Serial (avanzado):** se activa escaneando el código de
  configuración del manual; el SO expone un puerto serie virtual. Útil si el
  POS necesita control exclusivo de lecturas.
- **Bluetooth:** soportado por el hardware; contingencia si falla USB, no ruta principal.

#### Decisión de implementación (provisional)

Primera integración en Fase 3B: **USB-HID** sobre el hub, alineada con
`ScannerSource.USB_HID` y sin adapter propietario. USB-COM queda como
alternativa si HID compite mal con el teclado soft o con campos Compose.
Bluetooth queda como contingencia de cable, no del camino feliz inicial.

#### Pendiente de validación física (este lector)

- VID/PID USB al enumerar en la tablet objetivo.
- Sufijo de terminación de lectura (Enter/CR/LF) y si llega completo de un golpe.
- Comportamiento con foco en búsqueda/carrito de la app POS.
- Estabilidad al desconectar/reconectar el hub.
- Confirmar que se deja en HID (o documentar el código de config si se cambia a COM).

### Impresora de tickets — confirmada

| Campo | Valor |
|---|---|
| Pieza | Impresora térmica de tickets |
| Marca / familia | Generic / OEM (Zijiang, EasyTime, Zetpos, etc.) |
| Modelo | POS-5890A (también referido como ZJ-5890A) |
| Protocolo | ESC/POS compatible |
| Conexión de producción | USB (vía hub USB-A) |
| Red LAN/Ethernet | No |
| Bluetooth | Opcional según variante de fábrica; **no** es ruta principal del POS |
| Rollo físico | 58 mm |
| Ancho de impresión útil | 48 mm |
| Code pages declaradas | PC850 (multilingual), PC437/PC347 Europa, PC860, West Europe y extensiones |
| Cajón | Sí — puerto RJ11/RJ12 para pulso de apertura |
| Transporte previsto en app | `PrintTransport` USB + `EscPosEncoder` existente; TCP RAW no aplica a este modelo |

#### Decisión de implementación (provisional)

- Perfil de producción sobre el genérico 58 mm ya modelado (`PrinterProfiles.genericEscPos58`).
- Code page inicial: **CP850 / PC850**, alineada con el encoder actual y el español (`ñ`, acentos).
- Corte y cajón: habilitar en el perfil tras prueba física (`supportsCut`, `supportsCashDrawer` + `DrawerPulse`).
- No se implementa transporte de red para este kit. Bluetooth queda fuera del camino feliz.
- Adapter propietario solo si la unidad real falla con ESC/POS genérico (poco probable en esta familia OEM).

#### Pendiente de validación física (esta impresora)

- Variante exacta en mano (etiqueta Zijiang / EasyTime / Zetpos u otra) y VID/PID USB.
- Confirmación de code page efectiva con ticket de prueba `áéíóúüñÁÉÍÓÚÜÑ`.
- Comando de corte real y si el papel se corta limpio en 58 mm.
- Pulso de cajón (RJ11/RJ12): polaridad, tiempos on/off y que la gaveta abra.
- Alimentación propia vs hub; que USB de datos no pierda carga de la tablet.
- Enumeración USB estable tras desconectar/reconectar el hub.

### Tablet — confirmada

| Campo | Valor |
|---|---|
| Pieza | Tablet Android POS |
| Marca / modelo | Rikkai S25 (también listada como genérica / Umiio S25 Ultra según distribuidor) |
| Pantalla | ~10.1" (según listados comerciales) |
| Android declarado | Android 15 de fábrica (marcas genéricas pueden usar skins sobre bases anteriores; verificar build real) |
| Puerto | Un único USB-C convencional |
| OTG / USB host | Sí — **comprobado** con hub USB-C genérico (periféricos reconocidos en un POS anterior) |
| OTG + carga nativa simultánea | **No** — el puerto conmuta host vs carga; aún **sin** hub PD de passthrough de carga en mano |
| Implicación para el kit | Los periféricos por hub USB ya funcionan en host. La carga durante el turno es el hueco abierto: hace falta hub USB-C con Power Delivery (datos + energía). Incluso con hub PD, la simultaneidad depende del chipset y hay que verificarla en mesa |
| Referencias | [Mercado Libre (Rikkai)](https://www.mercadolibre.com.mx/tabletas-rikkai-android-15-101-16gb256gb-rom-5gwifi-b50/up/MLMU3360687665), [Amazon AE (S25 Ultra listing)](https://www.amazon.ae/S25-Ultra-3000x1440-Speakers-Keyboard/dp/B0FMST2RWL) |

#### Decisión de implementación (provisional)

- Sigue el diseño de un solo hub USB-C PD como punto de conexión **cuando se quiera cargar y usar periféricos a la vez**.
- Host USB (lector/impresora vía hub genérico) ya tiene evidencia empírica positiva; no bloquea empezar Fase 3B de software.
- **Operación a batería aceptada** como modo válido de mostrador si no hay hub PD o si PD falla en esta unidad. No es lo ideal para turnos largos, pero **no bloquea desarrollo ni el camino feliz USB**.
- Si un hub PD no logra carga+datos en esta unidad, plan B operativo: recargas entre turnos / a batería; plan C de hardware: otra tablet — decidir tras la mesa, no en código.
- **Bluetooth** no es ruta principal. Queda como contingencia si se pierde/rompe un cable USB o el hub: el Shawty S0024 y algunas variantes de la POS-5890A lo soportan. Se implementaría solo si hace falta en piloto; no retrasa la integración USB.

#### Pendiente de validación física (esta tablet)

- Etiqueta exacta en mano (Rikkai vs Umiio vs otra) y build Android (`Settings → About`).
- Con hub **PD**: ¿carga mientras hay datos y periféricos?
- Estabilidad con Shawty S0024 + POS-5890A conectados en el kit definitivo.
- Orientación landscape/portrait y tamaño táctil en UI POS.
- Comportamiento al desconectar/reconectar el hub sin reiniciar la app.

### Piezas aún no registradas

- Hub USB-C con PD (deseable para cargar en turno; no bloquea USB a batería).
- Cargador y cable USB-C compatibles con ese hub.
- Cajón de dinero (modelo; la impresora ya declara el puerto de control).
- Teclado USB (si se usa aparte del lector).
- Bluetooth de contingencia (solo si USB falla en piloto; no es entregable inicial).

## Arquitectura elegida

La tablet tendrá un único punto de conexión mediante un hub USB-C con Power
Delivery (PD):

```text
Cargador 30–65 W
        │
        ▼
Puerto USB-C PD del hub ── Tablet Rikkai S25 (USB-C único; OTG+carga solo vía hub PD)
        │
        ├── USB-A: lector de barras (Shawty S0024, HID por defecto)
        ├── USB-A: teclado
        └── USB-A: impresora ESC/POS 58 mm (POS-5890A / ZJ-5890A)
```

El hub debe transportar datos USB mientras carga la tablet. El puerto PD es
entrada de energía; no se debe asumir que todos los puertos USB-A entregan la
misma potencia ni que cualquier hub soporta tres periféricos simultáneos.

## Alcance de la implementación

La integración se divide en transporte, protocolo y perfil:

```text
TicketPrinter
      |
ESC/POS encoder/protocol
      |
USB transport ----- TCP RAW transport
      |
Printer profile: 58 mm, code page, corte, cajón y pulso opcionales
```

El transporte solo envía y recibe bytes. El protocolo genera comandos de
impresión. El perfil declara capacidades o diferencias del equipo. Por esto no
se implementará inicialmente una clase por marca o modelo: un equipo ESC/POS
compatible usará un transporte y un perfil genéricos. Un adapter específico
solo se justificará si el hardware real requiere comandos propietarios, un SDK o
un comportamiento que el perfil común no pueda expresar.

La Fase 3A puede implementarse sin periféricos físicos. Incluye los contratos
internos, fakes permanentes, encoder ESC/POS, perfiles, estados de diagnóstico y
la cola durable de `PrintJob`. La Fase 3B agrega los transportes Android y los
adaptadores de scanner después de validar los modelos concretos.

La autodetección será asistida, no universal: Android puede identificar un USB
por VID/PID, clase e interfaces, pero no puede deducir de forma confiable el
protocolo de cualquier impresora. La app sugerirá una configuración, solicitará
permisos y exigirá una impresión de prueba antes de guardarla.

## Requisitos que quedan fijados

- Tablet de producción: Rikkai S25 (alias Umiio S25 Ultra / genérica). OTG sí;
  OTG+carga nativa **no**. Hub PD deseable; operación a batería aceptada
  mientras tanto.
- Un solo hub conectado a la tablet (datos; PD cuando exista).
- Cargador al hub PD cuando se use ese modo; si no, carga aparte entre turnos.
- Lector, teclado e impresora conectados por USB-A.
- Lector de producción: Shawty S0024 en USB-HID como primera ruta.
- Impresora de producción: POS-5890A / ZJ-5890A, ESC/POS, USB, rollo 58 mm
  (impresión útil 48 mm), code page inicial PC850, cajón por RJ11/RJ12.
- Bluetooth: contingencia de cable/hub, no camino feliz ni bloqueante.
- El POS debe mostrar estado de periféricos y conservar trabajos de impresión
  si la impresora no está disponible.
- Los fakes de scanner, impresora y cajón permanecen como implementaciones de
  prueba; no se consideran drivers de producción.
- La primera compatibilidad de producción de este kit: lector USB-HID y
  impresora ESC/POS 58 mm por USB (sin LAN en el modelo elegido).
- La prueba de periféricos debe validar impresión ESC/POS, caracteres españoles
  (acentos y `ñ`) y el pulso del cajón conectado a la impresora.

## Pendientes de validación física

- Hub USB-C **con PD** + Rikkai S25: ¿carga y datos a la vez con lector e
  impresora? (Host sin carga ya funciona con hub genérico; esto es autonomía
  de turno, no reconocimiento de periféricos.)
- Marca/modelo exacto del hub PD y potencia real.
- Cargador y cable USB-C compatibles con esa potencia.
- Lector Shawty S0024: VID/PID, terminador HID y prueba de foco en la app
  (modelo ya elegido; falta mesa).
- Impresora POS-5890A / ZJ-5890A: VID/PID, PC850 real, corte, cajón RJ11/RJ12
  y alimentación (modelo ya elegido; falta mesa).
- Modelo exacto del cajón conectado al puerto de la impresora.
- Prueba de los tres periféricos conectados durante carga sostenida.
- Comportamiento después de desconectar/reconectar el hub y reiniciar la tablet.

## Prueba de aceptación de Fase 3B

1. Conectar el hub PD a la Rikkai S25 sin periféricos y verificar que la tablet carga.
2. Conectar el Shawty S0024 y comprobar que Android recibe una lectura HID.
3. Conectar teclado y comprobar escritura en una aplicación de prueba.
4. Conectar la POS-5890A / ZJ-5890A y confirmar enumeración USB sin perder carga.
5. Mantener los tres dispositivos conectados durante 30 minutos.
6. Desconectar y reconectar el hub, y repetir la prueba sin reiniciar el POS.
7. Registrar modelo, VID/PID USB, mensajes de Android y resultado por periférico.
8. Imprimir un ticket de prueba de 58 mm con `áéíóúüñÁÉÍÓÚÜÑ` y comprobar que
   los caracteres sean legibles.
9. Ejecutar la prueba combinada de impresión y apertura del cajón si el modelo
   declara esa capacidad.

Esta prueba debe ejecutarse antes de publicar el perfil de producción del kit.
Con el lector en HID, la primera integración puede evitar un SDK de fabricante.

La prueba no bloquea el desarrollo sobre fakes (Fase 3A), pero sí bloquea declarar
soporte oficial del conjunto de periféricos y el adapter/perfil de producción.
