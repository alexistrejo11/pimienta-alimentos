package io.github.alexistrejo.pimienta.pos.data.sync
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.*
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DeviceApiMockWebServerTest {
 private lateinit var server: MockWebServer
 private lateinit var api: DeviceApi
 @Before fun setUp(){ server=MockWebServer(); server.start(); api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(Json{ignoreUnknownKeys=true}.asConverterFactory("application/json".toMediaType())).build().create(DeviceApi::class.java) }
 @After fun tearDown(){server.shutdown()}
 @Test fun bootstrapCursorAndCentavos()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(200).setBody("""{"schemaVersion":1,"kind":"FULL","snapshotId":"s","generatedAt":"2026-01-01T00:00:00Z","site":{"id":"1","name":"Centro","address":"","currency":"MXN"},"device":{"id":"d","name":"POS","visibleCode":"T1","status":"ACTIVE"},"operators":[],"products":[{"id":"p","sku":"SKU","barcode":"1","name":"Agua","saleCategory":"Bebidas","unit":"PZA","priceCentavos":1250,"costCentavos":500,"available":true,"stockQuantity":"10","stockMinQuantity":"0","stockPolicy":"UNLIMITED"}],"openAmountCategories":[],"policies":{"allowNegativeStock":false,"defaultNegativeStockLimit":0,"staleCatalogWarnHours":24,"staleCatalogBlockHours":48},"cursors":{"changes":"c1"}}""").addHeader("Content-Type","application/json"))
  val result=api.bootstrap(); Assert.assertEquals("c1",result.cursors.changes); Assert.assertEquals(1250L,result.products.single().priceCentavos)
 }
 @Test fun mixedResults()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(200).setBody("""{"results":[{"eventId":"a","status":"ACCEPTED"},{"eventId":"b","status":"REQUIRES_REVIEW","incidentId":"inc-1"},{"eventId":"c","status":"REJECTED"}]}""").addHeader("Content-Type","application/json"))
  val result=api.events(EventsRequest(emptyList())); Assert.assertEquals(listOf("ACCEPTED","REQUIRES_REVIEW","REJECTED"),result.results.map{it.status})
 }
}
