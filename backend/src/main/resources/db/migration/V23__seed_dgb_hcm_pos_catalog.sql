-- Seed: DGB Heroico Colegio Militar cafeteria catalog from POS training bootstrap.
-- All inventory SKUs come from next_internal_item_sku().
-- Package EAN/UPC goes in barcode; internal CAF-* training codes are NULL.
-- Sale price lives only on headquarter_items (not inventory_items).

INSERT INTO headquarters (name, address, description)
VALUES (
    'DGB Heroico Colegio Militar',
    'TODO: reemplazar con la direccion real de la sede',
    'servicio de cafeteria escolar'
);

INSERT INTO pos_sale_categories (headquarter_id, name, display_order, active)
SELECT h.id, v.name, v.display_order, TRUE
FROM headquarters h
CROSS JOIN (VALUES
    ('Deli', 0),
    ('Mexicano', 1),
    ('Saludable', 2),
    ('Japonesa', 3),
    ('Postres', 4),
    ('Bebidas', 5),
    ('Paquete', 6),
    ('Panaderia', 7),
    ('Dulces', 8)
) AS v(name, display_order)
WHERE h.name = 'DGB Heroico Colegio Militar'
  AND h.deleted_at IS NULL;

WITH seed AS (
    SELECT *
    FROM (VALUES
        (1, 'Hot Cakes con Avena y Plátano', NULL, 40::numeric, 40::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (2, 'Chapata de Jamón de Pavo/Pollo', NULL, 40::numeric, 40::numeric, 5, 'Deli', 'NOT_CONTROLLED', TRUE),
        (3, 'Chapata de Bistec', NULL, 45::numeric, 45::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (4, 'Chapa Pizza', NULL, 45::numeric, 45::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (5, 'Lasaña de Carne', NULL, 45::numeric, 45::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (6, 'Sándwich De Jamón', NULL, 25::numeric, 25::numeric, 5, 'Deli', 'NOT_CONTROLLED', TRUE),
        (7, 'Sándwich de Pollo', NULL, 35::numeric, 35::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (8, 'Gringa', NULL, 45::numeric, 45::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (9, 'Sincronizadas', NULL, 30::numeric, 30::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (10, 'Burrito', NULL, 50::numeric, 50::numeric, 5, 'Deli', 'NOT_CONTROLLED', TRUE),
        (11, 'Pizza', NULL, 50::numeric, 50::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (12, 'Chicken Bake', NULL, 50::numeric, 50::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (13, 'Molletes Sencillos', NULL, 35::numeric, 35::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (14, 'Molletes con Jamón', NULL, 40::numeric, 40::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (15, 'Sopa Pasta', NULL, 40::numeric, 40::numeric, 0, 'Deli', 'NOT_CONTROLLED', TRUE),
        (16, 'Chilaquiles Sencillos', NULL, 35::numeric, 35::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (17, 'Chilaquiles Pollo', NULL, 45::numeric, 45::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (18, 'Enchiladas', NULL, 45::numeric, 45::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (19, 'Tacos De Bistec 3pz', NULL, 50::numeric, 50::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (20, 'Quesadilla 3pz', NULL, 35::numeric, 35::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (21, 'Huarache Sencillo', NULL, 35::numeric, 35::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (22, 'Huarache Con Huevo', NULL, 45::numeric, 45::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (23, 'Huarache con Res', NULL, 50::numeric, 50::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (24, 'Sope Sencillo', NULL, 30::numeric, 30::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (25, 'Sope con Carne', NULL, 40::numeric, 40::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (26, 'Torta de Jamón/Salchicha/Huevo', NULL, 35::numeric, 35::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (27, 'Torta de Pollo/Bistec', NULL, 40::numeric, 40::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (28, 'Ingrediente Extra', NULL, 10::numeric, 10::numeric, 0, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (29, 'Ensalada', NULL, 50::numeric, 50::numeric, 0, 'Saludable', 'NOT_CONTROLLED', TRUE),
        (30, 'Cóctel de Frutas', NULL, 45::numeric, 45::numeric, 0, 'Saludable', 'NOT_CONTROLLED', TRUE),
        (31, 'Vaso de Fruta', NULL, 30::numeric, 30::numeric, 0, 'Saludable', 'NOT_CONTROLLED', TRUE),
        (32, 'Crudites', NULL, 30::numeric, 30::numeric, 0, 'Saludable', 'NOT_CONTROLLED', TRUE),
        (33, 'Yakimeshi', NULL, 50::numeric, 50::numeric, 0, 'Japonesa', 'NOT_CONTROLLED', TRUE),
        (34, 'Sushi', NULL, 50::numeric, 50::numeric, 0, 'Japonesa', 'NOT_CONTROLLED', TRUE),
        (35, 'Ramen', NULL, 35::numeric, 35::numeric, 0, 'Japonesa', 'NOT_CONTROLLED', TRUE),
        (36, 'Gelatina', NULL, 325::numeric, 25::numeric, 5, 'Postres', 'NOT_CONTROLLED', TRUE),
        (37, 'Pay de Limón', NULL, 30::numeric, 30::numeric, 0, 'Postres', 'NOT_CONTROLLED', TRUE),
        (38, 'Arroz con Leche', NULL, 30::numeric, 30::numeric, 0, 'Postres', 'NOT_CONTROLLED', TRUE),
        (39, 'Fresas Con Crema', NULL, 35::numeric, 35::numeric, 0, 'Postres', 'NOT_CONTROLLED', TRUE),
        (40, 'Jugo de Naranja', NULL, 30::numeric, 30::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (41, 'Licuado De Platano', NULL, 35::numeric, 35::numeric, 5, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (42, 'Licuado de Fresa', NULL, 40::numeric, 40::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (43, 'Licuado de Chocolate', NULL, 30::numeric, 30::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (44, 'Cafe', NULL, 15::numeric, 15::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (45, 'Cafe con Leche', NULL, 20::numeric, 20::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (46, 'Chocolate Caliente', NULL, 30::numeric, 30::numeric, 0, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (47, 'Agua de Fruta 1lt', NULL, 25::numeric, 25::numeric, 5, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (48, 'Paquete Desayuno', NULL, 70::numeric, 70::numeric, 0, 'Paquete', 'NOT_CONTROLLED', TRUE),
        (49, 'Paquete Comida', NULL, 75::numeric, 75::numeric, 0, 'Paquete', 'NOT_CONTROLLED', TRUE),
        (50, 'Boing de Guayaba 500ml', '75003135', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (51, 'Arizona 570ml', '613008772901', 30::numeric, 30::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (52, 'Bonafont 1lt', '758104100422', 15::numeric, 15::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (53, 'Bonafont 600ml', '758104001712', 10::numeric, 10::numeric, 10, 'Bebidas', 'CONTROLLED', TRUE),
        (54, 'Peñafiel 600ml', '7501073839854', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (55, 'Chaparritta Mandarina Sin Gas 250ml', '7500326103483', 15::numeric, 15::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (56, 'Coca Zero 600ml', '7501055320639', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (57, 'Coca 600ml', '75007614', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (58, 'Te Verde Kirklannd', '096619757572', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (59, 'Sangría Señorial', '7500326103360', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (60, 'Electrolit', '7501125118562', 25::numeric, 25::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (61, 'Mantecadas de Nuez Bimbo', '7501030419389', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (62, 'Mantecadas de Vainilla Bimbo', '7501000112401', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (63, 'Donitas Espolvoreadas Bimbo', '7501030418399', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (64, 'Panquesitos con Chispas Bimbo', '7501000112388', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (65, 'Madalenas Bimbo', '7501030477488', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (66, 'Tiras Doraditas', '7501030464242', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (67, 'Barra BranFrut Fresa', '7501000116447', 15::numeric, 15::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (68, 'Nito Bimbo', '7501000112784', 20::numeric, 20::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (69, 'Tartinas con Fresa Tía Rosa', '7500810015261', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (70, 'Tartinas con Piña Tía Rosa', '7500810016190', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (71, 'Donas de Azúcar Bimbo', '7501030474227', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (72, 'Barra BranFrut de Piña', '7501000116430', 15::numeric, 15::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (73, 'Sponch Marinela', '7501000138944', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (74, 'Príncipe de Chocolate', '7500810011126', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (75, 'Deliciosas de Vainilla Lara', '7501030463740', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (76, 'Canelitas Marinela', '7500810022801', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (77, 'Baritas De Fresa Marinela', '7500810014721', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (78, 'Baritas De Pina Marinela', '7500810014738', 25::numeric, 25::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (79, 'Pingüinos Marinela', '7501000153800', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (80, 'Chocoroles Mariela', '75002275', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (81, 'Gansito Marinela', '7501000153107', 20::numeric, 20::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (82, 'Bigotes Tia Rosa', '7500810029077', 25::numeric, 25::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (83, 'Principe Rees Marinela', '7500810044056', 30::numeric, 30::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (84, 'Bimbunuelos Bimbo', '7501030472698', 25::numeric, 25::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (85, 'Principe Chocolate Blanco', '7500810011140', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (86, 'Deliciosas con sabor de chispas de chocolate Lara', '7501030463726', 25::numeric, 25::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (87, 'Principe de Limon Marinela', '7500810011157', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (88, 'Triki Trakes Marinela', '7501000140855', 30::numeric, 30::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (89, 'Pastisetas Suandy', '7500810031278', 35::numeric, 35::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (90, 'Pechuga o BIstec Asado', NULL, 55::numeric, 55::numeric, 5, 'Mexicano', 'NOT_CONTROLLED', TRUE),
        (91, 'Rebanadas Bimbo', '7501000112845', 15::numeric, 15::numeric, 0, 'Panaderia', 'CONTROLLED', TRUE),
        (92, 'Lucas Panzon', '7502226815145', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (93, 'Halls Caramelo', '7622210267863', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (94, 'Halls Yerbabuena', '7622210267832', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (95, 'Halls Mora Azul', '7622210267856', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (96, 'Tutsi Pop', '7501063500177', 10::numeric, 10::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (97, 'Lucas Muecas Cereza', '7502226815053', 15::numeric, 15::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (98, 'Lucas Muecas Sandia', '7502226815084', 15::numeric, 15::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (99, 'Lucas Muecas Chamoy', '7502226815022', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (100, 'Paleta de Dulce Generica', NULL, 5::numeric, 5::numeric, 5, 'Dulces', 'NOT_CONTROLLED', TRUE),
        (101, 'Paleta Bubbalo Xtreme', '7501056900168', 8::numeric, 8::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (102, 'Cacahuate Japones Nishikawa 60g', '7501308300074', 15::numeric, 15::numeric, 0, 'Dulces', 'CONTROLLED', TRUE),
        (103, 'Yogurt Danone Fresa 220g', '7501032398576', 20::numeric, 20::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (104, 'Agua Purificada Kirkland 500ml', '096619536740', 10::numeric, 10::numeric, 0, 'Bebidas', 'CONTROLLED', TRUE),
        (105, 'Brownie ChocoBro', NULL, 20::numeric, 20::numeric, 5, 'Postres', 'NOT_CONTROLLED', TRUE),
        (106, 'Flan', NULL, 30::numeric, 30::numeric, 5, 'Postres', 'NOT_CONTROLLED', TRUE),
        (107, 'Suerox Naranja Mandarina', '650240032301', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (108, 'Suerox Fresa Kiwi', '650240032264', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (109, 'Suerox', '650240035395', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (110, 'Suerox Uva', '650240032271', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (111, 'Boing de Mango 500ml', '75003104', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (112, 'Boing de Fresa', '75003166', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (113, 'Boing de Manzana', '75003159', 25::numeric, 25::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (114, 'Arizona de Mango 570ml', '613008772963', 30::numeric, 30::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (115, 'Arizona de Ponche de Frutas', '613008772871', 30::numeric, 30::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (116, 'Arizona de Sandia 570ml', '613008772840', 30::numeric, 30::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (117, 'Muffin de Chocolate Kirkland', '4696932000001', 25::numeric, 25::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (118, 'Galleta de Macadamia Kirkland', '4665610000008', 20::numeric, 20::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (119, 'Galleta de Chocolate Kirkland', '4542655000005', 20::numeric, 20::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (120, 'Muffin de Vainilla Kirkland', '4696933000000', 25::numeric, 25::numeric, 5, 'Panaderia', 'CONTROLLED', TRUE),
        (121, 'Chicle Menta  Orbit', '75039394', 5::numeric, 5::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (122, 'Chicle de Fresa Bubbalo', '75073244', 4::numeric, 4::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (123, 'Pollo a la Naranja', NULL, 50::numeric, 50::numeric, 5, 'Japonesa', 'NOT_CONTROLLED', TRUE),
        (124, 'Agua de Fruta 50ml', NULL, 15::numeric, 15::numeric, 5, 'Bebidas', 'NOT_CONTROLLED', TRUE),
        (125, 'Chicle Orbit Polar Mint', '75040437', 5::numeric, 5::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (126, 'Halls Cere', '7622210267825', 15::numeric, 15::numeric, 5, 'Bebidas', 'CONTROLLED', TRUE),
        (127, 'Palenta Pintazul Vero', '759686272682', 5::numeric, 5::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (128, 'Paleta Tarrito Vero', '7503030374552', 5::numeric, 5::numeric, 5, 'Dulces', 'CONTROLLED', TRUE),
        (129, 'Paleta Manita Vero', '7503030374583', 5::numeric, 5::numeric, 5, 'Dulces', 'CONTROLLED', TRUE)
    ) AS v(
        ord,
        name,
        barcode,
        cost_price,
        sale_price,
        reorder_point,
        sale_category,
        stock_policy,
        available
    )
),
with_sku AS MATERIALIZED (
    -- MATERIALIZED: next_internal_item_sku() must run once per row (PG 12+ may otherwise inline).
    SELECT
        s.ord,
        s.name,
        s.barcode,
        s.cost_price,
        s.sale_price,
        s.reorder_point,
        s.sale_category,
        s.stock_policy,
        s.available,
        next_internal_item_sku() AS sku
    FROM (
        SELECT *
        FROM seed
        ORDER BY ord
    ) s
),
inserted_items AS (
    INSERT INTO inventory_items (
        sku,
        name,
        category,
        unit,
        barcode,
        cost_price,
        reorder_point,
        reorder_quantity,
        status,
        catalog_role
    )
    SELECT
        w.sku,
        w.name,
        'FINISHED_GOOD',
        'PIECE',
        w.barcode,
        w.cost_price,
        w.reorder_point,
        0,
        'ACTIVE',
        'POS_SELLABLE'
    FROM with_sku w
    ORDER BY w.ord
    RETURNING id, sku
)
INSERT INTO headquarter_items (
    headquarter_id,
    item_id,
    pos_sale_category_id,
    sale_category,
    sale_price,
    available,
    stock_policy
)
SELECT
    h.id,
    i.id,
    c.id,
    c.name,
    w.sale_price,
    w.available,
    w.stock_policy
FROM with_sku w
JOIN inserted_items i
  ON i.sku = w.sku
JOIN headquarters h
  ON h.name = 'DGB Heroico Colegio Militar'
 AND h.deleted_at IS NULL
JOIN pos_sale_categories c
  ON c.headquarter_id = h.id
 AND c.name = w.sale_category
 AND c.deleted_at IS NULL
ORDER BY w.ord;

