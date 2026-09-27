# Convert the real deprecated catalog into the local POS bootstrap contract.
param(
    [string]$Source = "docs\technical\data\deprecated_product_data.json",
    [string]$Destination = "app\src\main\assets\pos-training-bootstrap.json"
)

$ErrorActionPreference = "Stop"
$legacy = Get-Content $Source -Raw | ConvertFrom-Json
$seenBarcodes = @{}
$products = @()
$nextId = 1

foreach ($item in $legacy.productos) {
    $baseBarcode = [string]$item.codigo
    if ($seenBarcodes.ContainsKey($baseBarcode)) {
        $seenBarcodes[$baseBarcode]++
        $barcode = "$baseBarcode-$($seenBarcodes[$baseBarcode])"
    } else {
        $seenBarcodes[$baseBarcode] = 1
        $barcode = $baseBarcode
    }

    # Store-prepared CAF products are not inventory-controlled yet.
    $stockPolicy = if ($baseBarcode.StartsWith("CAF-")) { "NOT_CONTROLLED" } else { "CONTROLLED" }
    $stock = [decimal]$item.stock
    $stockMin = [decimal]$item.stockMin

    $products += [ordered]@{
        id = "product-$('{0:D4}' -f $nextId)"
        sku = $barcode
        barcode = $barcode
        name = [string]$item.nombre
        saleCategory = [string]$item.categoria
        unit = "PIECE"
        price = ([decimal]$item.precio).ToString("0.####", [Globalization.CultureInfo]::InvariantCulture)
        cost = ([decimal]$item.costo).ToString("0.####", [Globalization.CultureInfo]::InvariantCulture)
        available = $true
        stock = $stock.ToString("0.####", [Globalization.CultureInfo]::InvariantCulture)
        stockMin = $stockMin.ToString("0.####", [Globalization.CultureInfo]::InvariantCulture)
        stockPolicy = $stockPolicy
    }
    $nextId++
}

$bootstrap = [ordered]@{
    schemaVersion = 2
    kind = "pos-bootstrap"
    environment = "debug"
    snapshotId = "real-catalog-debug-001"
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    site = [ordered]@{ id = "site-debug-001"; name = "Sede demostración"; address = "Datos locales de desarrollo"; currency = "MXN" }
    device = [ordered]@{ id = "tablet-debug-001"; name = "Tablet de desarrollo"; status = "AUTHORIZED" }
    users = @()
    products = $products
    openAmountCategories = @("Apoyo escolar", "Otro")
    notes = @(
        "Catalog converted from docs/technical/data/deprecated_product_data.json.",
        "CAF- products are NOT_CONTROLLED until prepared-food inventory rules are defined.",
        "Duplicate barcodes receive a unique POS suffix."
    )
}

$parent = Split-Path $Destination -Parent
New-Item -ItemType Directory -Force $parent | Out-Null
$bootstrap | ConvertTo-Json -Depth 8 | Set-Content $Destination -Encoding UTF8
Write-Host "Generated $($products.Count) products at $Destination"
