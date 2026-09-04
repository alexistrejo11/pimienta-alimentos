package io.github.alexistrejo11.pimienta.shared.spreadsheet;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    name = "SpreadsheetBulkImportResult",
    description =
        """
        Resultado de importación masiva. Si hay errores de fila, no se escribe nada \
        (created/updated = 0). Con dryRun=true los contadores son el plan, sin persistir.""")
public record SpreadsheetBulkImportResult(
    @Schema(description = "Filas actualizadas (o previstas en vista previa).", example = "5")
    int updated,
    @Schema(description = "Filas insertadas (o previstas en vista previa).", example = "12")
    int created,
    @Schema(description = "Filas ignoradas (sin efecto, p. ej. vacías).", example = "1")
    int skipped,
    @Schema(description = "Errores por fila; si no está vacío no hubo escrituras.")
    List<SpreadsheetBulkImportRowError> errors,
    @Schema(description = "true si solo se validó el archivo, sin persistir.")
    boolean dryRun) {}
