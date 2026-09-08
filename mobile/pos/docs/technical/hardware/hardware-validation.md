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

## Requisitos que quedan fijados

- Un solo hub conectado a la tablet.
- Cargador conectado al puerto USB-C PD del hub.
- Lector, teclado e impresora conectados por USB-A.
- La tablet debe soportar USB host/OTG y carga simultánea.
- No se agregará Bluetooth como ruta principal mientras USB cubra el caso.
- El POS debe mostrar estado de periféricos y conservar trabajos de impresión
  si la impresora no está disponible.
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

## Prueba de aceptación de Phase 0

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
