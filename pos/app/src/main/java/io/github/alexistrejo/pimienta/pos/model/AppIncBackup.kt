package io.github.alexistrejo.pimienta.pos.model

data class AppIncBackup(
    val app: String,
    val version: String,
    val fecha: String,
    val cfg: Configuration,
    val productos: List<Product>
)

data class Configuration(
    val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val moneda: String,
    val pie: String,
    val pin: String,


    // El JSON original representa estos booleanos como 0 o 1.
    val descontarStock: Int,
    val permitirSinStock: Int,
    val mostrarTicket: Int,
    val cajonAuto: Int,
    val nubeActiva: Int,
    val licenciaValida: Int,
    val terminalImprime: Int,

    val stockMinDefault: Double,
    val folio: Int,
    val ultimoRespaldo: Long,

    val nubeProjectId: String,
    val nubeTiendaId: String,


    val apartadoAnticipoPct: Double,
    val apartadoDias: Int,
    val apartadoRecargos: String,
    val apartadoFolio: Int,

    val licenciaCodigo: String,
    val licenciaNegocio: String,
    val licenciaVence: String,
    val licenciaUltimaRevision: Long,


    val terminalId: String,
    val dispositivoId: String,
    val dispositivoNombre: String,
    val ultimaImpresoraBT: String
)


data class Product(
    val id: String,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val costo: Double,
    val precio: Double,
    val stock: Double,
    val stockMin: Double,
    val porPeso: Boolean,
    val precioKg: Double,
    val actualizado: Long,
    val imagen: String? = null
)