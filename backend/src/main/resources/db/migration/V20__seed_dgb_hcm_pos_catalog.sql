-- Seed: DGB Heroico Colegio Militar cafeteria (headquarter, POS settings, sale categories, catalog,
-- POS operators). Operator PINs are stored as the Argon2 hashes issued by the backend.
-- Each row is a sale product. Postgres assigns the SKU. CONTROLLED rows also get an inventory item.
-- Package EAN/UPC goes in barcode; products without a package barcode keep it NULL.
-- Sale price lives only on headquarter_items. Cost is not captured yet (default 0).
INSERT INTO headquarters (name, address, description)
VALUES (
    'DGB Heroico Colegio Militar',
    'TODO: reemplazar con la direccion real de la sede',
    'servicio de cafeteria escolar'
);

INSERT INTO headquarter_pos_settings (
    headquarter_id,
    open_amount_categories,
    allow_open_products,
    default_negative_stock_limit,
    stockless
)
SELECT
    h.id,
    '["Deli", "Mexicano", "Saludable", "Japonesa", "Paquete", "Postres", "Bebidas preparadas", "Agua", "Jugos", "Refrescos", "Lacteos", "Panaderia", "Galletas", "Dulces", "Botanas"]'::jsonb,
    TRUE,
    10000,
    TRUE
FROM headquarters h
WHERE h.name = 'DGB Heroico Colegio Militar'
  AND h.deleted_at IS NULL;

INSERT INTO pos_operators (display_name, pos_role, pin_hash, active)
VALUES
    ('Daniela', 'MANAGER', '$argon2id$v=19$m=16384,t=2,p=1$cA7ODiJ4GFF44H6VWO49Rw$CCegg93TJ3wPUYnuY9cRiQL9DHTWCFLoM/Xn+8kWdOQ', TRUE),
    ('Marcos', 'MANAGER', '$argon2id$v=19$m=16384,t=2,p=1$95WiaX5kwiDxp9dpaPTZGA$xknFe62CI9jsBpxRvpd+7PwrsmbTA8RptLG3bntZjUs', TRUE),
    ('Ruth', 'MANAGER', '$argon2id$v=19$m=16384,t=2,p=1$aREChQctcOnpsd9WOVZ5EQ$i7ruuAI9WO8SBvSQDGBogLqxxZpcS9wsib+0LP3N0kk', TRUE);

INSERT INTO pos_operator_headquarter (operator_id, headquarter_id)
SELECT o.id, h.id
FROM pos_operators o
JOIN headquarters h
  ON h.name = 'DGB Heroico Colegio Militar'
 AND h.deleted_at IS NULL
WHERE o.deleted_at IS NULL
ORDER BY o.id;

INSERT INTO pos_sale_categories (headquarter_id, name, active)
SELECT h.id, v.name, TRUE
FROM headquarters h
CROSS JOIN (VALUES
    ('Deli'),
    ('Mexicano'),
    ('Saludable'),
    ('Japonesa'),
    ('Paquete'),
    ('Postres'),
    ('Bebidas preparadas'),
    ('Agua'),
    ('Jugos'),
    ('Refrescos'),
    ('Lacteos'),
    ('Panaderia'),
    ('Galletas'),
    ('Dulces'),
    ('Botanas')
) AS v(name)
WHERE h.name = 'DGB Heroico Colegio Militar'
  AND h.deleted_at IS NULL;

WITH seed AS (
    SELECT *
    FROM (VALUES
        (0, 'Hot Cakes con Avena y Plátano', NULL, 0, 'POS_SELLABLE', 'Deli', 40::numeric, 'NOT_CONTROLLED'),
        (1, 'Chapata de Jamón de Pavo/Pollo', NULL, 5, 'POS_SELLABLE', 'Deli', 40::numeric, 'NOT_CONTROLLED'),
        (2, 'Chapata de Bistec', NULL, 0, 'POS_SELLABLE', 'Deli', 45::numeric, 'NOT_CONTROLLED'),
        (3, 'Chapa Pizza', NULL, 0, 'POS_SELLABLE', 'Deli', 45::numeric, 'NOT_CONTROLLED'),
        (4, 'Lasaña de Carne', NULL, 0, 'POS_SELLABLE', 'Deli', 45::numeric, 'NOT_CONTROLLED'),
        (5, 'Sándwich De Jamón', NULL, 5, 'POS_SELLABLE', 'Deli', 25::numeric, 'NOT_CONTROLLED'),
        (6, 'Sándwich de Pollo', NULL, 0, 'POS_SELLABLE', 'Deli', 35::numeric, 'NOT_CONTROLLED'),
        (7, 'Gringa', NULL, 0, 'POS_SELLABLE', 'Deli', 45::numeric, 'NOT_CONTROLLED'),
        (8, 'Sincronizadas', NULL, 0, 'POS_SELLABLE', 'Deli', 30::numeric, 'NOT_CONTROLLED'),
        (9, 'Burrito', NULL, 5, 'POS_SELLABLE', 'Deli', 50::numeric, 'NOT_CONTROLLED'),
        (10, 'Pizza', NULL, 0, 'POS_SELLABLE', 'Deli', 50::numeric, 'NOT_CONTROLLED'),
        (11, 'Chicken Bake', NULL, 0, 'POS_SELLABLE', 'Deli', 50::numeric, 'NOT_CONTROLLED'),
        (12, 'Molletes Sencillos', NULL, 0, 'POS_SELLABLE', 'Deli', 35::numeric, 'NOT_CONTROLLED'),
        (13, 'Molletes con Jamón', NULL, 0, 'POS_SELLABLE', 'Deli', 40::numeric, 'NOT_CONTROLLED'),
        (14, 'Sopa Pasta', NULL, 0, 'POS_SELLABLE', 'Deli', 40::numeric, 'NOT_CONTROLLED'),
        (15, 'Chilaquiles Sencillos', NULL, 0, 'POS_SELLABLE', 'Mexicano', 35::numeric, 'NOT_CONTROLLED'),
        (16, 'Chilaquiles Pollo', NULL, 0, 'POS_SELLABLE', 'Mexicano', 45::numeric, 'NOT_CONTROLLED'),
        (17, 'Enchiladas', NULL, 0, 'POS_SELLABLE', 'Mexicano', 45::numeric, 'NOT_CONTROLLED'),
        (18, 'Tacos De Bistec 3pz', NULL, 0, 'POS_SELLABLE', 'Mexicano', 50::numeric, 'NOT_CONTROLLED'),
        (19, 'Quesadilla 3pz', NULL, 0, 'POS_SELLABLE', 'Mexicano', 35::numeric, 'NOT_CONTROLLED'),
        (20, 'Huarache Sencillo', NULL, 0, 'POS_SELLABLE', 'Mexicano', 35::numeric, 'NOT_CONTROLLED'),
        (21, 'Huarache Con Huevo', NULL, 0, 'POS_SELLABLE', 'Mexicano', 45::numeric, 'NOT_CONTROLLED'),
        (22, 'Huarache con Res', NULL, 0, 'POS_SELLABLE', 'Mexicano', 50::numeric, 'NOT_CONTROLLED'),
        (23, 'Sope Sencillo', NULL, 0, 'POS_SELLABLE', 'Mexicano', 30::numeric, 'NOT_CONTROLLED'),
        (24, 'Sope con Carne', NULL, 0, 'POS_SELLABLE', 'Mexicano', 40::numeric, 'NOT_CONTROLLED'),
        (25, 'Torta de Jamón/Salchicha/Huevo', NULL, 0, 'POS_SELLABLE', 'Mexicano', 35::numeric, 'NOT_CONTROLLED'),
        (26, 'Torta de Pollo/Bistec', NULL, 0, 'POS_SELLABLE', 'Mexicano', 40::numeric, 'NOT_CONTROLLED'),
        (27, 'Ingrediente Extra', NULL, 0, 'POS_SELLABLE', 'Mexicano', 10::numeric, 'NOT_CONTROLLED'),
        (28, 'Ensalada', NULL, 0, 'POS_SELLABLE', 'Saludable', 50::numeric, 'NOT_CONTROLLED'),
        (29, 'Cóctel de Frutas', NULL, 0, 'POS_SELLABLE', 'Saludable', 45::numeric, 'NOT_CONTROLLED'),
        (30, 'Vaso de Fruta', NULL, 0, 'POS_SELLABLE', 'Saludable', 30::numeric, 'NOT_CONTROLLED'),
        (31, 'Crudites', NULL, 0, 'POS_SELLABLE', 'Saludable', 30::numeric, 'NOT_CONTROLLED'),
        (32, 'Yakimeshi', NULL, 0, 'POS_SELLABLE', 'Japonesa', 50::numeric, 'NOT_CONTROLLED'),
        (33, 'Sushi', NULL, 0, 'POS_SELLABLE', 'Japonesa', 50::numeric, 'NOT_CONTROLLED'),
        (34, 'Ramen', NULL, 0, 'POS_SELLABLE', 'Japonesa', 35::numeric, 'NOT_CONTROLLED'),
        (35, 'Gelatina', NULL, 5, 'POS_SELLABLE', 'Postres', 25::numeric, 'NOT_CONTROLLED'),
        (36, 'Pay de Limón', NULL, 0, 'POS_SELLABLE', 'Postres', 30::numeric, 'NOT_CONTROLLED'),
        (37, 'Arroz con Leche', NULL, 0, 'POS_SELLABLE', 'Postres', 30::numeric, 'NOT_CONTROLLED'),
        (38, 'Fresas Con Crema', NULL, 0, 'POS_SELLABLE', 'Postres', 35::numeric, 'NOT_CONTROLLED'),
        (39, 'Jugo de Naranja', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 30::numeric, 'NOT_CONTROLLED'),
        (40, 'Licuado De Platano', NULL, 5, 'POS_SELLABLE', 'Bebidas preparadas', 35::numeric, 'NOT_CONTROLLED'),
        (41, 'Licuado de Fresa', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 40::numeric, 'NOT_CONTROLLED'),
        (42, 'Licuado de Chocolate', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 30::numeric, 'NOT_CONTROLLED'),
        (43, 'Cafe', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 15::numeric, 'NOT_CONTROLLED'),
        (44, 'Cafe con Leche', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 20::numeric, 'NOT_CONTROLLED'),
        (45, 'Chocolate Caliente', NULL, 0, 'POS_SELLABLE', 'Bebidas preparadas', 30::numeric, 'NOT_CONTROLLED'),
        (46, 'Agua de Fruta 1lt', NULL, 5, 'POS_SELLABLE', 'Bebidas preparadas', 25::numeric, 'CONTROLLED'),
        (47, 'Paquete Desayuno', NULL, 0, 'POS_SELLABLE', 'Paquete', 70::numeric, 'NOT_CONTROLLED'),
        (48, 'Paquete Comida', NULL, 0, 'POS_SELLABLE', 'Paquete', 75::numeric, 'NOT_CONTROLLED'),
        (49, 'Boing de Guayaba 500ml', '75003135', 5, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (50, 'Arizona 570ml', '613008772901', 5, 'POS_SELLABLE', 'Jugos', 30::numeric, 'CONTROLLED'),
        (51, 'Bonafont 1lt', '758104100422', 0, 'POS_SELLABLE', 'Agua', 15::numeric, 'CONTROLLED'),
        (52, 'Bonafont 600ml', '758104001712', 10, 'POS_SELLABLE', 'Agua', 10::numeric, 'CONTROLLED'),
        (53, 'Peñafiel 600ml', '7501073839854', 0, 'POS_SELLABLE', 'Refrescos', 25::numeric, 'CONTROLLED'),
        (54, 'Chaparritta Mandarina Sin Gas 250ml', '7500326103483', 0, 'POS_SELLABLE', 'Jugos', 15::numeric, 'CONTROLLED'),
        (55, 'Coca Zero 600ml', '7501055320639', 0, 'POS_SELLABLE', 'Refrescos', 25::numeric, 'CONTROLLED'),
        (56, 'Coca 600ml', '75007614', 0, 'POS_SELLABLE', 'Refrescos', 25::numeric, 'CONTROLLED'),
        (57, 'Te Verde Kirklannd', '096619757572', 0, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (58, 'Sangría Señorial 600ml', '7500326103360', 0, 'POS_SELLABLE', 'Refrescos', 25::numeric, 'CONTROLLED'),
        (59, 'Electrolit', '7501125118562', 0, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (60, 'Mantecadas de Nuez Bimbo', '7501030419389', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (61, 'Mantecadas de Vainilla Bimbo', '7501000112401', 5, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (62, 'Donitas Espolvoreadas Bimbo', '7501030418399', 0, 'POS_SELLABLE', 'Panaderia', 30::numeric, 'CONTROLLED'),
        (63, 'Panquesitos con Chispas Bimbo', '7501000112388', 0, 'POS_SELLABLE', 'Panaderia', 30::numeric, 'CONTROLLED'),
        (64, 'Madalenas Bimbo', '7501030477488', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (65, 'Tiras Doraditas', '7501030464242', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (66, 'Barra BranFrut Fresa', '7501000116447', 5, 'POS_SELLABLE', 'Panaderia', 15::numeric, 'CONTROLLED'),
        (67, 'Nito Bimbo', '7501000112784', 0, 'POS_SELLABLE', 'Panaderia', 20::numeric, 'CONTROLLED'),
        (68, 'Tartinas con Fresa Tía Rosa', '7500810015261', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (69, 'Tartinas con Piña Tía Rosa', '7500810016190', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (70, 'Donas de Azúcar Bimbo', '7501030474227', 0, 'POS_SELLABLE', 'Panaderia', 30::numeric, 'CONTROLLED'),
        (71, 'Barra BranFrut de Piña', '7501000116430', 0, 'POS_SELLABLE', 'Panaderia', 15::numeric, 'CONTROLLED'),
        (72, 'Sponch Marinela', '7501000138944', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (73, 'Príncipe de Chocolate', '7500810011126', 0, 'POS_SELLABLE', 'Galletas', 30::numeric, 'CONTROLLED'),
        (74, 'Deliciosas de Vainilla Lara', '7501030463740', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (75, 'Canelitas Marinela', '7500810022801', 0, 'POS_SELLABLE', 'Galletas', 30::numeric, 'CONTROLLED'),
        (76, 'Baritas De Fresa Marinela', '7500810014721', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (77, 'Baritas De Pina Marinela', '7500810014738', 5, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (78, 'Pingüinos Marinela', '7501000153800', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (79, 'Chocoroles Mariela', '75002275', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (80, 'Gansito Marinela', '7501000153107', 0, 'POS_SELLABLE', 'Panaderia', 20::numeric, 'CONTROLLED'),
        (81, 'Bigotes Tia Rosa', '7500810029077', 5, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (82, 'Principe Rees Marinela', '7500810044056', 5, 'POS_SELLABLE', 'Galletas', 30::numeric, 'CONTROLLED'),
        (83, 'Bimbunuelos Bimbo', '7501030472698', 5, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (84, 'Principe Chocolate Blanco', '7500810011140', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (85, 'Deliciosas con sabor de chispas de chocolate Lara', '7501030463726', 0, 'POS_SELLABLE', 'Galletas', 25::numeric, 'CONTROLLED'),
        (86, 'Principe de Limon Marinela', '7500810011157', 0, 'POS_SELLABLE', 'Galletas', 30::numeric, 'CONTROLLED'),
        (87, 'Triki Trakes Marinela', '7501000140855', 0, 'POS_SELLABLE', 'Galletas', 30::numeric, 'CONTROLLED'),
        (88, 'Pastisetas Suandy', '7500810031278', 0, 'POS_SELLABLE', 'Galletas', 35::numeric, 'CONTROLLED'),
        (89, 'Pechuga o BIstec Asado', NULL, 5, 'POS_SELLABLE', 'Mexicano', 55::numeric, 'NOT_CONTROLLED'),
        (90, 'Rebanadas Bimbo', '7501000112845', 0, 'POS_SELLABLE', 'Panaderia', 15::numeric, 'CONTROLLED'),
        (91, 'Lucas Panzon', '7502226815145', 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (92, 'Halls Caramelo', '7622210267863', 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (93, 'Halls Yerbabuena', '7622210267832', 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (94, 'Halls Mora Azul', '7622210267856', 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (95, 'Tutsi Pop', '7501063500177', 5, 'POS_SELLABLE', 'Dulces', 10::numeric, 'CONTROLLED'),
        (96, 'Lucas Muecas Cereza', '7502226815053', 5, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (97, 'Lucas Muecas Sandia', '7502226815084', 5, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (98, 'Lucas Muecas Chamoy', '7502226815022', 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (99, 'Paleta de Dulce Generica', NULL, 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'NOT_CONTROLLED'),
        (100, 'Paleta Bubbalo Xtreme', '7501056900168', 0, 'POS_SELLABLE', 'Dulces', 8::numeric, 'CONTROLLED'),
        (101, 'Cacahuate Japones Nishikawa 60g', '7501308300074', 0, 'POS_SELLABLE', 'Botanas', 15::numeric, 'CONTROLLED'),
        (102, 'Yogurt Danone Fresa 220g', '7501032398576', 0, 'POS_SELLABLE', 'Lacteos', 20::numeric, 'CONTROLLED'),
        (103, 'Agua Purificada Kirkland 500ml', '096619536740', 0, 'POS_SELLABLE', 'Agua', 10::numeric, 'CONTROLLED'),
        (104, 'Brownie ChocoBro', NULL, 5, 'POS_SELLABLE', 'Postres', 20::numeric, 'NOT_CONTROLLED'),
        (105, 'Flan', NULL, 5, 'POS_SELLABLE', 'Postres', 30::numeric, 'NOT_CONTROLLED'),
        (106, 'Suerox Naranja Mandarina', '650240032301', 5, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (107, 'Suerox Fresa Kiwi', '650240032264', 5, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (108, 'Suerox', '650240035395', 5, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (109, 'Suerox Uva', '650240032271', 5, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (110, 'Boing de Mango 500ml', '75003104', 5, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (111, 'Boing de Fresa', '75003166', 5, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (112, 'Boing de Manzana', '75003159', 5, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (113, 'Arizona de Mango 570ml', '613008772963', 5, 'POS_SELLABLE', 'Jugos', 30::numeric, 'CONTROLLED'),
        (114, 'Arizona de Ponche de Frutas', '613008772871', 5, 'POS_SELLABLE', 'Jugos', 30::numeric, 'CONTROLLED'),
        (115, 'Arizona de Sandia 570ml', '613008772840', 5, 'POS_SELLABLE', 'Jugos', 30::numeric, 'CONTROLLED'),
        (116, 'Muffin de Chocolate Kirkland', '4696932000001', 5, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (117, 'Galleta de Macadamia Kirkland', '4665610000008', 5, 'POS_SELLABLE', 'Galletas', 20::numeric, 'CONTROLLED'),
        (118, 'Galleta de Chocolate Kirkland', '4542655000005', 5, 'POS_SELLABLE', 'Galletas', 20::numeric, 'CONTROLLED'),
        (119, 'Muffin de Vainilla Kirkland', '4696933000000', 5, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (120, 'Chicle Menta  Orbit', '75039394', 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'CONTROLLED'),
        (121, 'Chicle de Fresa Bubbalo', '75073244', 5, 'POS_SELLABLE', 'Dulces', 4::numeric, 'CONTROLLED'),
        (122, 'Pollo a la Naranja', NULL, 5, 'POS_SELLABLE', 'Japonesa', 50::numeric, 'NOT_CONTROLLED'),
        (123, 'Chicle Orbit Polar Mint', '75040437', 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'CONTROLLED'),
        (124, 'Halls Cere', '7622210267825', 5, 'POS_SELLABLE', 'Dulces', 15::numeric, 'CONTROLLED'),
        (125, 'Palenta Pintazul Vero', '759686272682', 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'CONTROLLED'),
        (126, 'Paleta Tarrito Vero', '7503030374552', 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'CONTROLLED'),
        (127, 'Paleta Manita Vero', '7503030374583', 5, 'POS_SELLABLE', 'Dulces', 5::numeric, 'CONTROLLED'),
        (128, 'Dulce Mexicano', NULL, 0, 'POS_SELLABLE', 'Dulces', 10::numeric, 'NOT_CONTROLLED'),
        (129, 'Leche Yomi LaLa Chocolate 400ml', '7501020570854', 0, 'POS_SELLABLE', 'Lacteos', 20::numeric, 'CONTROLLED'),
        (130, 'Yogurth Danone Mango 220g', '7506443108592', 0, 'POS_SELLABLE', 'Lacteos', 20::numeric, 'CONTROLLED'),
        (131, 'Leche Nesquik Nestle 185ml', '7506475117876', 0, 'POS_SELLABLE', 'Lacteos', 20::numeric, 'CONTROLLED'),
        (132, 'Chaparritta Piña Sin Gas 250ml', '7500326103490', 0, 'POS_SELLABLE', 'Jugos', 15::numeric, 'CONTROLLED'),
        (133, 'Chaparritta Guayaba Sin Gas 250ml', '7503029481803', 0, 'POS_SELLABLE', 'Jugos', 15::numeric, 'CONTROLLED'),
        (134, 'Cacahuete Japonés Nishikawa 120g', NULL, 0, 'POS_SELLABLE', 'Botanas', 30::numeric, 'NOT_CONTROLLED'),
        (135, 'Mineralita 600ml', '737186930875', 0, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (136, 'Hamburguesa', NULL, 0, 'POS_SELLABLE', 'Deli', 50::numeric, 'NOT_CONTROLLED'),
        (137, 'Alitas 3pz', NULL, 0, 'POS_SELLABLE', 'Deli', 55::numeric, 'NOT_CONTROLLED'),
        (138, 'Red Cola 600ml', '7503006897016', 0, 'POS_SELLABLE', 'Refrescos', 22::numeric, 'CONTROLLED'),
        (139, 'Jarrito de Uva  600 ml', '7503006897603', 0, 'POS_SELLABLE', 'Refrescos', 22::numeric, 'CONTROLLED'),
        (140, 'Jarrito de Tamarindo 600ml', '744886328826', 0, 'POS_SELLABLE', 'Refrescos', 22::numeric, 'CONTROLLED'),
        (141, 'Wasa', NULL, 0, 'POS_SELLABLE', 'Botanas', 70::numeric, 'NOT_CONTROLLED'),
        (142, 'Jarrito de Mandarina 600ml', '744886298822', 0, 'POS_SELLABLE', 'Refrescos', 22::numeric, 'CONTROLLED'),
        (143, 'Jarrito de Toronja 600ml', '744886348824', 0, 'POS_SELLABLE', 'Refrescos', 22::numeric, 'CONTROLLED'),
        (144, 'Agua Natural Skarch 600ml', '793573264091', 0, 'POS_SELLABLE', 'Agua', 10::numeric, 'CONTROLLED'),
        (145, 'Agua Natural Skarch 1lt', '040232838699', 0, 'POS_SELLABLE', 'Agua', 15::numeric, 'CONTROLLED'),
        (146, 'Leche Alpura Vaquitas 200ml', '7501055915668', 0, 'POS_SELLABLE', 'Lacteos', 25::numeric, 'CONTROLLED'),
        (147, 'Palomitas', NULL, 0, 'POS_SELLABLE', 'Dulces', 15::numeric, 'NOT_CONTROLLED'),
        (148, 'Twinkies Vainilla 3pz', '7501030429319', 0, 'POS_SELLABLE', 'Panaderia', 25::numeric, 'CONTROLLED'),
        (149, 'Frutsi Uva 250ml', '75001957', 0, 'POS_SELLABLE', 'Jugos', 15::numeric, 'CONTROLLED'),
        (150, 'Boing de Uva 500ml', '75003180', 0, 'POS_SELLABLE', 'Jugos', 25::numeric, 'CONTROLLED'),
        (151, 'Agua Natural Kirkiland 1lt', '096619334346', 0, 'POS_SELLABLE', 'Agua', 15::numeric, 'CONTROLLED'),
        (152, 'Electrolit Mora Azul 625ml', '7501125174797', 0, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED'),
        (153, 'Electrolit Fresa 625ml', '7501125104268', 0, 'POS_SELLABLE', 'Agua', 25::numeric, 'CONTROLLED')
    ) AS v(
        ord,
        name,
        barcode,
        reorder_point,
        catalog_role,
        sale_category,
        sale_price,
        stock_policy
    )
),
with_rows AS (
    SELECT *
    FROM (
        SELECT *
        FROM seed
        ORDER BY ord
    ) s
),
inserted_items AS (
    INSERT INTO inventory_items (
        name,
        category,
        unit,
        reorder_point,
        reorder_quantity,
        status
    )
    SELECT
        w.name,
        'FINISHED_GOOD',
        'PIECE',
        w.reorder_point,
        0,
        'ACTIVE'
    FROM with_rows w
    WHERE w.stock_policy = 'CONTROLLED'
    ORDER BY w.ord
    RETURNING id, name
),
inserted_products AS (
    INSERT INTO products (
        name,
        unit,
        barcode,
        status,
        inventory_item_id
    )
    SELECT
        w.name,
        'PIECE',
        w.barcode,
        'ACTIVE',
        i.id
    FROM with_rows w
    LEFT JOIN inserted_items i
      ON i.name = w.name
    ORDER BY w.ord
    RETURNING id, name
)
INSERT INTO headquarter_items (
    headquarter_id,
    product_id,
    pos_sale_category_id,
    sale_category,
    sale_price,
    available,
    stock_policy
)
SELECT
    h.id,
    p.id,
    c.id,
    c.name,
    w.sale_price,
    TRUE,
    w.stock_policy
FROM with_rows w
JOIN inserted_products p
  ON p.name = w.name
JOIN headquarters h
  ON h.name = 'DGB Heroico Colegio Militar'
 AND h.deleted_at IS NULL
JOIN pos_sale_categories c
  ON c.headquarter_id = h.id
 AND c.name = w.sale_category
 AND c.deleted_at IS NULL
ORDER BY w.ord;
