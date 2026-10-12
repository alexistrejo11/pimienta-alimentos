package io.github.alexistrejo.pimienta.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.app.PosApplication
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.sync.PosApiUserMessages
import io.github.alexistrejo.pimienta.pos.data.sync.ProvisioningRepository
import io.github.alexistrejo.pimienta.pos.hardware.PosScannerRegistry
import io.github.alexistrejo.pimienta.pos.data.sync.planProductEdit
import io.github.alexistrejo.pimienta.pos.data.printing.PrintWorker
import io.github.alexistrejo.pimienta.pos.data.sync.SyncWorker
import io.github.alexistrejo.pimienta.pos.data.sync.runForegroundSync
import io.github.alexistrejo.pimienta.pos.data.update.ApkInstallOutcome
import io.github.alexistrejo.pimienta.pos.data.update.PosAppUpdater
import io.github.alexistrejo.pimienta.pos.data.update.ReleaseCheckOutcome
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.domain.DashboardSummary
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.domain.PosRepository
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseBreakdown
import io.github.alexistrejo.pimienta.pos.domain.ShiftCloseCalculator
import io.github.alexistrejo.pimienta.pos.domain.sortedCategoryNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Requests Manager authorization without changing the cashier session.
@Composable
internal fun ManagerAccess(
    users: List<LocalUserEntity>,
    repository: PosRepository,
    onDismiss: () -> Unit,
    onAuthorized: (LocalUserEntity) -> Unit
) {
    val managers = remember(users) { users.filter { it.active && it.isManagerOrAdmin } }
    if (managers.isEmpty()) {
        val fallback = users.firstOrNull() ?: LocalUserEntity(id = "manager", displayName = "Gerente", role = "MANAGER", pinHash = "", active = true)
        LaunchedEffect(Unit) { onAuthorized(fallback) }
    } else {
        ManagerPinDialog(
            users = users,
            title = "Autorizar acceso a Manager",
            repository = repository,
            onDismiss = onDismiss,
            onApproved = { manager, _ -> onAuthorized(manager) },
        )
    }
}

// Shows the local dashboard when no shift is open; operational actions stay unavailable.
