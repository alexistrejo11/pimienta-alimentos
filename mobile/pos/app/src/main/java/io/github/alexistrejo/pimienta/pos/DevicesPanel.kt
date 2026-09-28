package io.github.alexistrejo.pimienta.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.hardware.BondedBluetoothDevice
import io.github.alexistrejo.pimienta.pos.hardware.BondedBluetoothKind
import io.github.alexistrejo.pimienta.pos.hardware.EscPosEncoder
import io.github.alexistrejo.pimienta.pos.hardware.OperationalDocument
import io.github.alexistrejo.pimienta.pos.hardware.PeripheralStatus
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrintResult
import io.github.alexistrejo.pimienta.pos.hardware.PrintableLine
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.PrinterLink
import io.github.alexistrejo.pimienta.pos.hardware.PrinterPreferences
import io.github.alexistrejo.pimienta.pos.hardware.UsbTicketPrinter
import io.github.alexistrejo.pimienta.pos.hardware.bluetoothRadioReady
import io.github.alexistrejo.pimienta.pos.hardware.bondedBluetoothDevices
import io.github.alexistrejo.pimienta.pos.hardware.scannerLooksPresent
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// Full-screen tablet config for printers and scanners; no Manager PIN.
@Composable
internal fun DevicesScreen(
    repository: PosRepository,
    onReturn: () -> Unit,
    returnLabel: String,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PosButton(returnLabel, onReturn)
            Column(Modifier.weight(1f)) {
                Text(
                    "Impresión y dispositivos",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Configuración de la tablet · no cambia el cajero del turno",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DevicesPanel(
            repository = repository,
            showHeading = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        )
    }
}

// Presence chips, Bluetooth destination, and hardware tests shared by caja and Manager Estado.
@Composable
internal fun DevicesPanel(
    repository: PosRepository,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
) {
    val printJobs = remember { mutableStateOf(emptyList<PrintJobEntity>()) }
    var peripheralMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingScan by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode = repository.mode()
    var printerAvailability by remember(mode) { mutableStateOf(PrinterFactory.availability(context, mode)) }
    var usbStatus by remember { mutableStateOf(UsbTicketPrinter.status(context)) }
    var savedMac by remember { mutableStateOf(PrinterPreferences(context).mac()) }
    var bonded by remember { mutableStateOf(emptyList<BondedBluetoothDevice>()) }
    var bluetoothGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bluetoothGranted = granted
        if (granted) bonded = bondedBluetoothDevices(context)
        printerAvailability = PrinterFactory.availability(context, mode)
        usbStatus = UsbTicketPrinter.status(context)
    }

    fun refreshPrinter() {
        printerAvailability = PrinterFactory.availability(context, mode)
        usbStatus = UsbTicketPrinter.status(context)
        savedMac = PrinterPreferences(context).mac()
        if (bluetoothGranted) bonded = bondedBluetoothDevices(context)
    }

    LaunchedEffect(mode, bluetoothGranted) {
        refreshPrinter()
        PosPrinterRegistry.statusTick.collect { refreshPrinter() }
    }
    LaunchedEffect(mode, bluetoothGranted) {
        while (true) {
            delay(2_000)
            refreshPrinter()
        }
    }

    fun refreshPrintJobs() {
        scope.launch {
            printJobs.value = withContext(Dispatchers.IO) { repository.pendingPrintJobs() }
        }
    }

    LaunchedEffect(Unit) {
        refreshPrintJobs()
        PosScannerRegistry.hid?.start()
        PosScannerRegistry.primary?.start()
    }

    // Waits on the HID registry timestamp so a missed SharedFlow subscriber still shows the scan.
    LaunchedEffect(awaitingScan) {
        if (!awaitingScan) return@LaunchedEffect
        PosScannerRegistry.hid?.start()
        PosScannerRegistry.primary?.start()
        val armedAt = System.currentTimeMillis()
        peripheralMessage = "Escanea un código con el lector…"
        val value = withTimeoutOrNull(20_000) {
            while (true) {
                val at = PosScannerRegistry.lastReadAtMillis
                val code = PosScannerRegistry.lastReadValue
                if (code != null && at >= armedAt) return@withTimeoutOrNull code
                delay(40)
            }
        }
        peripheralMessage = if (value != null) {
            "Lectura: $value"
        } else {
            "No se recibió lectura. Vuelve a intentar."
        }
        awaitingScan = false
    }

    val printers = remember(bonded) { bonded.filter { it.kind == BondedBluetoothKind.PRINTER } }
    val scanners = remember(bonded) { bonded.filter { it.kind == BondedBluetoothKind.SCANNER } }
    val others = remember(bonded) { bonded.filter { it.kind == BondedBluetoothKind.OTHER } }
    val radioReady = bluetoothRadioReady(context)
    val showUsbChip = usbStatus == PeripheralStatus.READY || usbStatus == PeripheralStatus.PERMISSION_REQUIRED
    val showBtChip = savedMac != null && radioReady &&
        printerAvailability.link == PrinterLink.BLUETOOTH &&
        (printerAvailability.status == PeripheralStatus.READY ||
            printerAvailability.status == PeripheralStatus.DISCOVERED)
    val btConfiguredOffline = savedMac != null && !showBtChip
    val showScannerChip = scannerLooksPresent(scanners)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showHeading) {
            Text("Impresión y dispositivos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conectados ahora", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Solo se muestran equipos detectados. No son interruptores.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showUsbChip) DevicePresenceChip("Impresora USB")
                    if (showBtChip) DevicePresenceChip("Impresora Bluetooth")
                    if (showScannerChip) DevicePresenceChip("Lector")
                }
                if (!showUsbChip && !showBtChip && !showScannerChip) {
                    Text("Ningún periférico detectado ahora.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (usbStatus == PeripheralStatus.PERMISSION_REQUIRED) {
                    Text("Impresora USB visible · falta permiso USB.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (btConfiguredOffline) {
                    Text(
                        "Impresora Bluetooth configurada, fuera de línea.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bluetooth y destino de impresión", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Empareja la térmica en Ajustes de Android. USB se usa cuando hay impresora en el hub; Bluetooth es el camino con la tablet cargando.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!bluetoothGranted) {
                    PosButton("Permitir Bluetooth", { bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) })
                } else if (printers.isEmpty()) {
                    Text("No hay impresoras Bluetooth emparejadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    printers.forEach { printer ->
                        val selected = printer.mac.equals(savedMac, ignoreCase = true)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(printer.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (selected) "en uso para tickets" else printer.mac,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (!selected) {
                                PosButton(
                                    "Usar para imprimir",
                                    {
                                        PrinterPreferences(context).saveMac(printer.mac)
                                        PosPrinterRegistry.bluetoothPrinter(context, printer.mac)
                                        refreshPrinter()
                                        PosPrinterRegistry.notifyChanged()
                                        peripheralMessage = "Impresora Bluetooth: ${printer.name}."
                                    },
                                )
                            }
                        }
                    }
                }
                if (bluetoothGranted && scanners.isNotEmpty()) {
                    Text(
                        "También emparejado: ${scanners.joinToString { it.name }}. No se usa para imprimir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (bluetoothGranted && others.isNotEmpty()) {
                    Text(
                        "Otros Bluetooth: ${others.joinToString { it.name }}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (savedMac != null) {
                    PosButton("Olvidar impresora Bluetooth", {
                        PrinterPreferences(context).clearMac()
                        PosPrinterRegistry.releaseBluetooth()
                        refreshPrinter()
                        PosPrinterRegistry.notifyChanged()
                        peripheralMessage = "Ya no se usará esa impresora Bluetooth para tickets. Sigue emparejada en Android."
                    })
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pruebas", style = MaterialTheme.typography.titleMedium)
                val failed = printJobs.value.count { it.status == "FAILED" }
                Text(
                    "${printJobs.value.size} trabajos pendientes · $failed fallidos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Las pruebas no alteran ventas ni eliminan eventos pendientes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Pulsa Probar lectura y escanea. El Enter del lector no sale de esta pantalla.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PosButton("Imprimir prueba y abrir cajón", {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val printer = PrinterFactory.create(context, mode)
                            val encoder = EscPosEncoder(printer.profile)
                            val document = OperationalDocument(
                                title = "Prueba de impresión",
                                folio = "TEST",
                                occurredAt = Instant.now(),
                                lines = listOf(PrintableLine("POS-5890A", "1", 0)),
                                totalCentavos = 0,
                            )
                            printer.print(encoder.encode(document, openDrawer = true))
                        }
                        val link = PrinterFactory.availability(context, mode)
                        peripheralMessage = when {
                            result is PrintResult.Printed && link.link == PrinterLink.USB &&
                                link.status == PeripheralStatus.READY ->
                                "Prueba enviada por USB."
                            result is PrintResult.Printed && link.link == PrinterLink.BLUETOOTH &&
                                link.status == PeripheralStatus.READY ->
                                "Prueba enviada por Bluetooth."
                            result is PrintResult.Printed -> "Prueba simulada. No hay impresora USB ni Bluetooth."
                            result is PrintResult.Failed -> "Impresión fallida: ${result.reason.name}"
                            else -> "Impresión fallida."
                        }
                    }
                }, primary = true)
                PosButton("Procesar cola de impresión", {
                    PrintWorker.enqueue(context)
                    refreshPrintJobs()
                    peripheralMessage = "Cola de impresión encolada."
                })
                PosButton(
                    if (awaitingScan) "Esperando lectura…" else "Probar lectura",
                    { awaitingScan = true },
                    enabled = !awaitingScan,
                )
                peripheralMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
