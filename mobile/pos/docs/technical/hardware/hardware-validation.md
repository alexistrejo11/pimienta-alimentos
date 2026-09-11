# Hardware y conexión del POS

Estado: decisión provisional confirmada para Phase 0, 2026-09-05.

## Arquitectura elegida

La tablet tendrá un único punto de conexión mediante un hub USB-C con Power
Delivery (PD):

```text
Cargador 30–65 W
        │
        ▼
Puerto USB-C PD del hub ── Tablet Android
        │
        ├── USB-A: lector de barras
        ├── USB-A: teclado
        └── USB-A: impresora de tickets
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

- Un solo hub conectado a la tablet.
- Cargador conectado al puerto USB-C PD del hub.
- Lector, teclado e impresora conectados por USB-A.
- La tablet debe soportar USB host/OTG y carga simultánea.
- No se agregará Bluetooth como ruta principal mientras USB cubra el caso.
- El POS debe mostrar estado de periféricos y conservar trabajos de impresión
  si la impresora no está disponible.
- Los fakes de scanner, impresora y cajón permanecen como implementaciones de
  prueba; no se consideran drivers de producción.
- La primera compatibilidad de producción se limita a lectores USB serial/CDC o
  HID controlado y a impresoras ESC/POS de 58 mm por USB o TCP/IP RAW.
- La impresora objetivo usa rollo térmico de 58 mm.
- La prueba de periféricos debe validar impresión ESC/POS, caracteres españoles
  (acentos y `ñ`) y, si existe un puerto de apertura compatible, el pulso del
  cajón conectado a la impresora.

## Pendientes de validación física

- Marca/modelo exacto de tablet y versión de Android.
- Marca/modelo del hub y potencia PD real.
- Cargador y cable USB-C compatibles con esa potencia.
- Modelo del lector y si funciona como teclado HID o requiere protocolo propio.
- Modelo de impresora, comandos soportados (por ejemplo ESC/POS), code page para
  español y conexión USB/TCP/IP.
- Tipo de conector del cajón (por ejemplo RJ11/RJ12), voltaje/pulso requerido y
  si la impresora puede accionarlo; no asumir que el cable es Ethernet.
- Si la impresora requiere alimentación propia o puede alimentarse desde el hub.
- Prueba de los tres periféricos conectados durante carga sostenida.
- Comportamiento después de desconectar/reconectar el hub y reiniciar la tablet.

## Prueba de aceptación de Fase 3B

1. Conectar el hub sin periféricos y verificar que la tablet carga.
2. Conectar lector y comprobar que Android recibe una lectura como entrada.
3. Conectar teclado y comprobar escritura en una aplicación de prueba.
4. Conectar impresora y confirmar enumeración USB sin perder carga.
5. Mantener los tres dispositivos conectados durante 30 minutos.
6. Desconectar y reconectar el hub, y repetir la prueba sin reiniciar el POS.
7. Registrar modelo, VID/PID USB, mensajes de Android y resultado por periférico.
8. Imprimir un ticket de prueba de 58 mm con `áéíóúüñÁÉÍÓÚÜÑ` y comprobar que
   los caracteres sean legibles.
9. Ejecutar la prueba combinada de impresión y apertura del cajón si el modelo
   declara esa capacidad.

Esta prueba debe ejecutarse antes de implementar drivers específicos. Si el
lector es HID, la primera integración puede evitar un SDK de fabricante.

La prueba no bloquea el desarrollo de Fase 3A, pero sí bloquea la selección de
un adapter específico, el perfil de producción y la publicación del soporte de
ese conjunto de periféricos.
