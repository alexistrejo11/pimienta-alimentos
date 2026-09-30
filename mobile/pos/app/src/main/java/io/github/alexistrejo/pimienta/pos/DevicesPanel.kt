package io.github.alexistrejo.pimienta.pos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.hardware.BondedBluetoothDevice
import io.github.alexistrejo.pimienta.pos.hardware.BondedBluetoothKind
import io.github.alexistrejo.pimienta.pos.hardware.DeviceFix
import io.github.alexistrejo.pimienta.pos.hardware.DeviceStatusLine
import io.github.alexistrejo.pimienta.pos.hardware.DeviceTone
import io.github.alexistrejo.pimienta.pos.hardware.EscPosEncoder
import io.github.alexistrejo.pimienta.pos.hardware.OperationalDocument
import io.github.alexistrejo.pimienta.pos.hardware.PeripheralStatus
import io.github.alexistrejo.pimienta.pos.hardware.PosPrinterRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.hardware.PrintableLine
import io.github.alexistrejo.pimienta.pos.hardware.PrinterFactory
import io.github.alexistrejo.pimienta.pos.hardware.PrinterLink
import io.github.alexistrejo.pimienta.pos.hardware.PrinterPanelInput
import io.github.alexistrejo.pimienta.pos.hardware.PrinterPreferences
import io.github.alexistrejo.pimienta.pos.hardware.UsbPrintTransport
import io.github.alexistrejo.pimienta.pos.hardware.UsbTicketPrinter
import io.github.alexistrejo.pimienta.pos.hardware.bluetoothRadioReady
import io.github.alexistrejo.pimienta.pos.hardware.bondedBluetoothDevices
import io.github.alexistrejo.pimienta.pos.hardware.printerStatusLine
import io.github.alexistrejo.pimienta.pos.hardware.scannerLooksPresent
import io.github.alexistrejo.pimienta.pos.hardware.scannerStatusLine
import io.github.alexistrejo.pimienta.pos.hardware.testPrintMessage
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
                    "Revisa la impresora y el lector de esta caja",
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

// Printer route, reader, Bluetooth backup, and hardware tests shared by caja and Manager Estado.
@Composable
internal fun DevicesPanel(
    repository: PosRepository,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
) {
    val printJobs = remember { mutableStateOf(emptyList<PrintJobEntity>()) }
    var peripheralMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingScan by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode = repository.mode()
    var usbStatus by remember { mutableStateOf(UsbTicketPrinter.status(context)) }
    var savedMac by remember { mutableStateOf(PrinterPreferences(context).mac()) }
    var bonded by remember { mutableStateOf(emptyList<BondedBluetoothDevice>()) }
    var radioReady by remember { mutableStateOf(bluetoothRadioReady(context)) }
    var bluetoothStatus by remember { mutableStateOf(PosPrinterRegistry.bluetoothStatus()) }
    var bluetoothGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    fun refreshPrinter() {
        usbStatus = UsbTicketPrinter.status(context)
        savedMac = PrinterPreferences(context).mac()
        radioReady = bluetoothRadioReady(context)
        bluetoothStatus = PosPrinterRegistry.bluetoothStatus()
        if (bluetoothGranted) bonded = bondedBluetoothDevices(context)
    }

    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bluetoothGranted = granted
        refreshPrinter()
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
        peripheralMessage = "Escanea cualquier código con el lector…"
        val value = withTimeoutOrNull(20_000) {
            while (true) {
                val at = PosScannerRegistry.lastReadAtMillis
                val code = PosScannerRegistry.lastReadValue
                if (code != null && at >= armedAt) return@withTimeoutOrNull code
                delay(40)
            }
        }
        peripheralMessage = if (value != null) {
            "El lector funciona. Leyó: $value"
        } else {
            "No llegó ninguna lectura. Revisa que el lector esté encendido y vuelve a probar."
        }
        awaitingScan = false
    }

    val printers = remember(bonded) { bonded.filter { it.kind == BondedBluetoothKind.PRINTER } }
    val scanners = remember(bonded) { bonded.filter { it.kind == BondedBluetoothKind.SCANNER } }
    val savedName = printers.firstOrNull { it.mac.equals(savedMac, ignoreCase = true) }?.name
    val printerLine = printerStatusLine(
        PrinterPanelInput(
            usb = usbStatus,
            savedMac = savedMac,
            savedPrinterName = savedName,
            bluetoothRadioReady = radioReady,
            bluetoothStatus = bluetoothStatus,
            training = mode == RuntimeMode.SANDBOX,
        ),
    )
    val scannerLine = scannerStatusLine(scannerLooksPresent(scanners))
    val usbActive = usbStatus == PeripheralStatus.READY

    // Maps a row's suggested fix to the matching Android prompt or settings screen.
    fun applyFix(fix: DeviceFix) {
        when (fix) {
            DeviceFix.GRANT_USB -> {
                UsbPrintTransport.requestPermissionIfNeeded(context)
                peripheralMessage = "Acepta el aviso de Android para usar la impresora por cable."
            }
            DeviceFix.OPEN_BLUETOOTH_SETTINGS -> openBluetoothSettings(context)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showHeading) {
            Text("Impresión y dispositivos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        // Current state: one row per device with what to do when it is not ready.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DeviceStatusRow(
                    device = "Impresora",
                    line = printerLine,
                    fixLabel = printerLine.fix?.let(::fixLabel),
                    onFix = { printerLine.fix?.let(::applyFix) },
                )
                DeviceStatusRow(device = "Lector", line = scannerLine)
            }
        }

        // Bluetooth printer used whenever the cable is not plugged in.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Impresora Bluetooth", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (usbActive) {
                        "Ahora se imprime por cable. La impresora elegida aquí se usa cuando desconectas el cable."
                    } else {
                        "Se usa cuando la impresora no está conectada por cable."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!bluetoothGranted) {
                    PosButton("Permitir Bluetooth", { bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) })
                } else if (printers.isEmpty()) {
                    Text(
                        "No hay impresoras emparejadas. Enciende la impresora y emparéjala en Ajustes de Android.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PosButton("Abrir ajustes de Bluetooth", { openBluetoothSettings(context) })
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
                                if (selected) {
                                    Text(
                                        if (usbActive) "Elegida · en espera mientras haya cable" else "Elegida",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (!selected) {
                                PosButton(
                                    "Usar esta",
                                    {
                                        PrinterPreferences(context).saveMac(printer.mac)
                                        PosPrinterRegistry.bluetoothPrinter(context, printer.mac)
                                        refreshPrinter()
                                        PosPrinterRegistry.notifyChanged()
                                        peripheralMessage = "Listo: se imprimirá por Bluetooth en ${printer.name} cuando no haya cable."
                                    },
                                )
                            }
                        }
                    }
                }
                if (bluetoothGranted && scanners.isNotEmpty()) {
                    Text(
                        "Lector emparejado: ${scanners.joinToString { it.name }}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (savedMac != null) {
                    PosButton("Quitar impresora Bluetooth", {
                        PrinterPreferences(context).clearMac()
                        PosPrinterRegistry.releaseBluetooth()
                        refreshPrinter()
                        PosPrinterRegistry.notifyChanged()
                        peripheralMessage = "Ya no se imprimirá por Bluetooth. La impresora sigue emparejada en Android."
                    })
                }
            }
        }

        // Hardware checks. They never touch sales or pending events.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Probar", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Las pruebas no registran ventas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            val availability = PrinterFactory.availability(context, mode)
                            val link = if (availability.status == PeripheralStatus.READY) availability.link else PrinterLink.NONE
                            peripheralMessage = testPrintMessage(result, link)
                            refreshPrinter()
                        }
                    }, primary = true)
                    PosButton(
                        if (awaitingScan) "Esperando lectura…" else "Probar lectura",
                        { awaitingScan = true },
                        enabled = !awaitingScan,
                    )
                }
                peripheralMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurface) }
            }
        }

        // Print queue internals for support; collapsed so cashiers are not asked about jobs.
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraSmall) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PosButton(
                    if (showAdvanced) "Ocultar avanzado" else "Avanzado",
                    {
                        showAdvanced = !showAdvanced
                        if (showAdvanced) refreshPrintJobs()
                    },
                )
                if (showAdvanced) {
                    val failed = printJobs.value.count { it.status == "FAILED" }
                    Text(
                        "Tickets por imprimir: ${printJobs.value.size} · con error: $failed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PosButton("Reintentar tickets pendientes", {
                        PrintWorker.enqueue(context)
                        refreshPrintJobs()
                        peripheralMessage = "Reintentando los tickets pendientes."
                    })
                }
            }
        }
    }
}

// One device line: coloured dot, device name, plain-language state, and an optional fix button.
@Composable
internal fun DeviceStatusRow(
    device: String,
    line: DeviceStatusLine,
    fixLabel: String? = null,
    onFix: () -> Unit = {},
) {
    val dot = when (line.tone) {
        DeviceTone.OK -> MaterialTheme.colorScheme.secondary
        DeviceTone.WARNING -> MaterialTheme.colorScheme.tertiary
        DeviceTone.BLOCKED -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(dot, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                device,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(line.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            line.detail?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (fixLabel != null) PosButton(fixLabel, onFix, primary = line.tone == DeviceTone.BLOCKED)
    }
}

private fun fixLabel(fix: DeviceFix): String = when (fix) {
    DeviceFix.GRANT_USB -> "Dar permiso USB"
    DeviceFix.OPEN_BLUETOOTH_SETTINGS -> "Abrir ajustes de Bluetooth"
}

// Opens Android Bluetooth settings so the cashier can pair or turn the radio on.
private fun openBluetoothSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
