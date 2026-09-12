# Tracking de implementación del POS

Esta carpeta es el tablero oficial del avance. Aquí se registra el alcance de
la versión, la fase actual, el trabajo terminado, lo pendiente y los criterios
para cerrar cada fase.

## Punto de entrada

- [Estado actual](00-estado-actual.md): qué está implementado, qué está en
  curso y cuál es el siguiente corte (revisión 2026-09-11).
- [Alcance y entregables](01-alcance-y-entregables.md)
- [Fases y checklists](02-fases-y-checklists.md)
- [Validación y pruebas](03-validacion-y-pruebas.md)

La integración Device API tiene tracking propio en
[../integration/](../integration/), alineado con este tablero.

## Documentos

- Los detalles técnicos viven en [../technical/README.md](../technical/README.md).

## Regla de entrega

Cada fase debe terminar con:

1. un flujo demostrable con datos realistas;
2. pruebas automatizadas proporcionales a su riesgo;
3. una lista corta de pruebas manuales en emulador y, cuando aplique, tablet/periférico real;
4. documentación actualizada si una decisión cambia.

Los documentos de fases son plan de trabajo. Para afirmar que algo ya existe
debe aparecer también en `00-estado-actual.md` y tener una implementación o
prueba correspondiente en `app/`.
