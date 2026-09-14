package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.serialization.Serializable

@Serializable data class EnrollRequest(val enrollmentCode: String, val devicePublicId: String, val deviceName: String, val appVersion: String)
@Serializable data class SiteDto(val id: String, val name: String, val address: String = "", val currency: String = "MXN")
@Serializable data class EnrollResponse(val deviceId: String, val visibleCode: String, val status: String, val site: SiteDto, val accessToken: String, val refreshToken: String, val accessTokenExpiresInSeconds: Long, val refreshTokenExpiresInSeconds: Long, val refreshTokenMaxExpiresInSeconds: Long, val minAppVersion: String? = null, val eventSchemaVersions: Map<String, Int> = emptyMap())
@Serializable data class TokenPair(val accessToken: String, val refreshToken: String, val accessTokenExpiresInSeconds: Long, val refreshTokenExpiresInSeconds: Long, val refreshTokenMaxExpiresInSeconds: Long)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class OperatorDto(val id: String, val displayName: String, val role: String, val pinHash: String, val active: Boolean)
@Serializable data class ProductDto(
    val id: String,
    val sku: String,
    val barcode: String? = null,
    val name: String,
    val saleCategory: String = "",
    val unit: String,
    val priceCentavos: Long,
    val costCentavos: Long,
    val available: Boolean,
    // Backend sends JSON numbers; Room stores them as text.
    val stockQuantity: Int,
    val stockMinQuantity: Int,
    val stockPolicy: String = "UNLIMITED",
    val negativeStockLimit: Int? = null,
)
@Serializable data class PoliciesDto(
    val allowNegativeStock: Boolean = true,
    val allowOpenProducts: Boolean = false,
    // Backend omits this when null (@JsonInclude NON_NULL).
    val defaultNegativeStockLimit: Int? = null,
    val staleCatalogWarnHours: Int = 24,
    val staleCatalogBlockHours: Int = 72,
)
@Serializable data class CursorsDto(val changes: String)
@Serializable data class BootstrapResponse(val schemaVersion: Int, val kind: String, val snapshotId: String, val generatedAt: String, val site: SiteDto, val device: DeviceDto, val operators: List<OperatorDto>, val products: List<ProductDto>, val openAmountCategories: List<String>, val policies: PoliciesDto, val cursors: CursorsDto)
@Serializable data class DeviceDto(val id: String, val name: String, val visibleCode: String, val status: String)
@Serializable data class ChangeOp(val op: String, val entity: String, val id: String, val data: kotlinx.serialization.json.JsonElement? = null)
@Serializable data class ChangesResponse(val schemaVersion: Int, val nextCursor: String, val operations: List<ChangeOp>)
@Serializable data class EventEnvelope(val eventId: String, val eventType: String, val schemaVersion: Int, val deviceId: String, val siteId: String, val deviceSequence: Long, val aggregateId: String? = null, val shiftId: String? = null, val occurredAt: String, val payload: kotlinx.serialization.json.JsonObject)
@Serializable data class EventsRequest(val events: List<EventEnvelope>)
@Serializable data class EventResult(val eventId: String, val status: String, val serverReceivedAt: String? = null, val incidentId: String? = null, val message: String? = null)
@Serializable data class EventsResponse(val results: List<EventResult>)
@Serializable data class TelemetryLogDto(val schemaVersion: Int, val eventType: String, val level: String, val message: String, val stack: String? = null, val occurredAt: String)
@Serializable data class TelemetryHealthDto(val syncState: String, val pendingEvents: Int, val oldestPendingAgeSeconds: Long, val appVersion: String)
@Serializable data class TelemetryBatchRequest(val events: List<TelemetryLogDto>, val health: TelemetryHealthDto? = null)
@Serializable data class TelemetryAcceptedResponse(val accepted: Int)
