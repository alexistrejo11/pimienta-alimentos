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
   server.enqueue(MockResponse().setResponseCode(200).setBody("""{"schemaVersion":1,"kind":"FULL","snapshotId":"s","generatedAt":"2026-01-01T00:00:00Z","site":{"id":"1","name":"Centro","address":"","currency":"MXN"},"device":{"id":"d","name":"POS","visibleCode":"T1","status":"ACTIVE"},"operators":[],"products":[{"id":"p","sku":"SKU","barcode":"1","name":"Agua","saleCategory":"Bebidas","unit":"PZA","priceCentavos":1250,"costCentavos":500,"available":true,"stockQuantity":10,"stockMinQuantity":0,"stockPolicy":"UNLIMITED"}],"openAmountCategories":[],"policies":{"allowNegativeStock":false,"allowOpenProducts":true,"defaultNegativeStockLimit":0,"openAmountCategories":["Bebidas"],"staleCatalogWarnHours":24,"staleCatalogBlockHours":48},"cursors":{"changes":"cursor-hq-1-s42"}}""").addHeader("Content-Type","application/json"))
   val result=api.bootstrap(); Assert.assertEquals("cursor-hq-1-s42",result.cursors.changes); Assert.assertEquals(1250L,result.products.single().priceCentavos); Assert.assertEquals(10, result.products.single().stockQuantity); Assert.assertTrue(result.policies.allowOpenProducts); Assert.assertEquals(listOf("Bebidas"), result.policies.openAmountCategories)
 }
 @Test fun bootstrapPoliciesWithoutDefaultNegativeStockLimit()=runBlocking{
  // Mirrors production when HQ POS settings leave defaultNegativeStockLimit null (field omitted).
   server.enqueue(MockResponse().setResponseCode(200).setBody("""{"schemaVersion":1,"kind":"FULL","snapshotId":"s","generatedAt":"2026-01-01T00:00:00Z","site":{"id":"1","name":"Centro","address":"","currency":"MXN"},"device":{"id":"d","name":"POS","visibleCode":"T1","status":"ACTIVE"},"operators":[],"products":[],"openAmountCategories":[],"policies":{"allowNegativeStock":true,"allowOpenProducts":false,"staleCatalogWarnHours":24,"staleCatalogBlockHours":72},"cursors":{"changes":"cursor-hq-1-s1"}}""").addHeader("Content-Type","application/json"))
  val result=api.bootstrap()
  Assert.assertNull(result.policies.defaultNegativeStockLimit)
  Assert.assertEquals(24, result.policies.staleCatalogWarnHours)
  Assert.assertEquals(72, result.policies.staleCatalogBlockHours)
  Assert.assertFalse(result.policies.stockless)
 }

 @Test fun changesUsesSequenceCursorInQuery()=runBlocking{
   server.enqueue(MockResponse().setResponseCode(200).setBody("""{"schemaVersion":1,"nextCursor":"cursor-hq-1-s43","operations":[]}""").addHeader("Content-Type","application/json"))
   api.changes("cursor-hq-1-s42")
   Assert.assertEquals("cursor-hq-1-s42",server.takeRequest().requestUrl?.queryParameter("cursor"))
 }
 @Test fun mixedResults()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(200).setBody("""{"results":[{"eventId":"a","status":"ACCEPTED"},{"eventId":"b","status":"REQUIRES_REVIEW","incidentId":"inc-1"},{"eventId":"c","status":"REJECTED"}]}""").addHeader("Content-Type","application/json"))
  val result=api.events(EventsRequest(emptyList())); Assert.assertEquals(listOf("ACCEPTED","REQUIRES_REVIEW","REJECTED"),result.results.map{it.status})
 }
 @Test fun createProductPostsDeviceCatalogBody()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"9","sku":"INT-1","name":"Agua","saleCategory":"Bebidas","unit":"PIECE","priceCentavos":1500,"costCentavos":0,"available":true,"stockQuantity":0,"stockMinQuantity":0,"stockPolicy":"NOT_CONTROLLED"}""").addHeader("Content-Type","application/json"))
  val created=api.createProduct(CreatePosProductRequest("Agua",1500,"Bebidas",null,42,"NOT_CONTROLLED"))
  Assert.assertEquals("9",created.id)
  Assert.assertEquals(1500L,created.priceCentavos)
  val recorded=server.takeRequest()
  Assert.assertEquals("/api/v1/pos/sync/products",recorded.path)
  Assert.assertEquals("POST",recorded.method)
  val body=recorded.body.readUtf8()
  Assert.assertTrue(body.contains("\"saleCategory\":\"Bebidas\""))
  Assert.assertTrue(body.contains("\"createdByOperatorId\":42"))
  Assert.assertTrue(body.contains("\"stockPolicy\":\"NOT_CONTROLLED\""))
 }
 @Test fun renameProductPutsNameOnly()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"9","sku":"INT-1","name":"Horchata","saleCategory":"Bebidas","unit":"PIECE","priceCentavos":1500,"costCentavos":0,"available":true,"stockQuantity":0,"stockMinQuantity":0,"stockPolicy":"NOT_CONTROLLED"}""").addHeader("Content-Type","application/json"))
  val updated=api.renameProduct("9", RenamePosProductRequest("Horchata", "7501"))
  Assert.assertEquals("Horchata", updated.name)
  val recorded=server.takeRequest()
  Assert.assertEquals("PUT", recorded.method)
  Assert.assertEquals("/api/v1/pos/sync/products/9", recorded.path)
  val body=recorded.body.readUtf8()
  Assert.assertTrue(body.contains("\"name\":\"Horchata\""))
  Assert.assertTrue(body.contains("\"barcode\":\"7501\""))
  Assert.assertEquals(1, server.requestCount)
 }
 @Test fun updateOfferPutsPriceAndStockPolicy()=runBlocking{
  server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"9","sku":"INT-1","name":"Agua","saleCategory":"Bebidas","unit":"PIECE","priceCentavos":1800,"costCentavos":0,"available":true,"stockQuantity":0,"stockMinQuantity":0,"stockPolicy":"CONTROLLED"}""").addHeader("Content-Type","application/json"))
  applyPlannedProductEdit(
   plan = ProductEditPlan(rename = false, offer = true),
   rename = { api.renameProduct("9", RenamePosProductRequest("No")) },
   offer = { api.updateProductOffer("9", UpdatePosProductOfferRequest(1800, "CONTROLLED")) },
   persist = {},
  )
  Assert.assertEquals(1, server.requestCount)
  val recorded=server.takeRequest()
  Assert.assertEquals("PUT", recorded.method)
  Assert.assertEquals("/api/v1/pos/sync/products/9/offer", recorded.path)
  val body=recorded.body.readUtf8()
  Assert.assertTrue(body.contains("\"salePriceCentavos\":1800"))
  Assert.assertTrue(body.contains("\"stockPolicy\":\"CONTROLLED\""))
 }
}
