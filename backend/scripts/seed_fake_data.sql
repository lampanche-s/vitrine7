\set ON_ERROR_STOP on
\pset pager off

BEGIN;

-- Corrige somente os placeholders encontrados na base de desenvolvimento.
UPDATE users
SET name = 'Rafael Menezes',
    username = 'rafael.menezes',
    updated_at = CURRENT_TIMESTAMP
WHERE role = 'SUPER_ADMIN'
  AND username LIKE 'teste\_%' ESCAPE '\';

UPDATE suppliers
SET name = 'Açougue Monte Gordo',
    normalized_name = 'acougue monte gordo',
    cnpj_digits = '99000000000001',
    phone_digits = '71970000001',
    cep_digits = '42840000',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND name = 'Rom´rio Carne';

UPDATE catalog_entries
SET name = 'Carvão vegetal 3 kg',
    normalized_name = 'carvao vegetal 3 kg',
    price_cents = 1890,
    stock_enabled = TRUE,
    stock_quantity = 48,
    minimum_stock_quantity = 12,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1
  AND name = 'Item';

UPDATE catalog_entries
SET name = 'Lavagem externa expressa',
    normalized_name = 'lavagem externa expressa',
    price_cents = 3500,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2
  AND entry_type = 'SERVICE'
  AND name = 'cu';

-- Usuários: poucos em relação ao restante da operação.
WITH source(name, username, email, role, status) AS (
    VALUES
        ('Camila Andrade', 'camila.andrade', 'camila.andrade@vitrine7.local', 'ADMINISTRADOR', 'ATIVO'),
        ('Bruno Ferreira', 'bruno.ferreira', 'bruno.ferreira@vitrine7.local', 'ADMINISTRADOR', 'ATIVO'),
        ('Aline Santos', 'aline.santos', 'aline.santos@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Diego Oliveira', 'diego.oliveira', 'diego.oliveira@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Juliana Costa', 'juliana.costa', 'juliana.costa@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Marcos Vinícius', 'marcos.vinicius', 'marcos.vinicius@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Renata Almeida', 'renata.almeida', 'renata.almeida@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Thiago Ribeiro', 'thiago.ribeiro', 'thiago.ribeiro@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Fernanda Lima', 'fernanda.lima', 'fernanda.lima@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Lucas Nascimento', 'lucas.nascimento', 'lucas.nascimento@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('Patrícia Gomes', 'patricia.gomes', 'patricia.gomes@vitrine7.local', 'OPERADOR', 'ATIVO'),
        ('João Pedro Souza', 'joao.souza', 'joao.souza@vitrine7.local', 'OPERADOR', 'BLOQUEADO')
), password AS (
    SELECT password_hash
    FROM users
    WHERE role = 'ADMINISTRADOR'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
)
INSERT INTO users(name, username, email, password_hash, role, status)
SELECT source.name, source.username, source.email, password.password_hash, source.role, source.status
FROM source
CROSS JOIN password
WHERE NOT EXISTS (
    SELECT 1 FROM users existing_user
    WHERE LOWER(existing_user.username) = LOWER(source.username)
      AND existing_user.deleted_at IS NULL
);

-- Fornecedores regionais e distribuidores compatíveis com bar e estética automotiva.
WITH source(name, cnpj_digits, phone_digits, cep_digits) AS (
    VALUES
        ('Distribuidora Costa do Sol', '99000000000002', '71970000002', '42840010'),
        ('Bahia Bebidas Atacado', '99000000000003', '71970000003', '42840020'),
        ('Frigorífico Boa Carne', '99000000000004', '71970000004', '42840030'),
        ('Hortifruti Verde Vida', '99000000000005', '71970000005', '42840040'),
        ('Laticínios Serra Azul', '99000000000006', '71970000006', '42840050'),
        ('Pescados do Litoral', '99000000000007', '71970000007', '42840060'),
        ('Casa dos Temperos Bahia', '99000000000008', '71970000008', '42840070'),
        ('Panificadora Tradição', '99000000000009', '71970000009', '42840080'),
        ('Embalagens Atlântico', '99000000000010', '71970000010', '42840090'),
        ('Gelo Cristal Distribuidora', '99000000000011', '71970000011', '42840100'),
        ('Cervejaria Baía Norte', '99000000000012', '71970000012', '42840110'),
        ('Doces Recôncavo', '99000000000013', '71970000013', '42840120'),
        ('Café Forte da Bahia', '99000000000014', '71970000014', '42840130'),
        ('Alimentos Porto Seguro', '99000000000015', '71970000015', '42840140'),
        ('Produtos de Limpeza Mar Azul', '99000000000016', '71970000016', '42840150'),
        ('Auto Química Nordeste', '99000000000017', '71970000017', '42840160'),
        ('Estética Car Profissional', '99000000000018', '71970000018', '42840170'),
        ('Cerauto Ceras e Polidores', '99000000000019', '71970000019', '42840180'),
        ('Panos e Fibras Salvador', '99000000000020', '71970000020', '42840190'),
        ('Mundo das Mangueiras', '99000000000021', '71970000021', '42840200'),
        ('Equipamentos Litoral', '99000000000022', '71970000022', '42840210'),
        ('Descartáveis Camaçari', '99000000000023', '71970000023', '42840220'),
        ('Água Mineral Fonte Clara', '99000000000024', '71970000024', '42840230'),
        ('Refrigerantes Tropical', '99000000000025', '71970000025', '42840240'),
        ('Congelados Sabor da Terra', '99000000000026', '71970000026', '42840250'),
        ('Fazenda Ovos Caipiras', '99000000000027', '71970000027', '42840260'),
        ('Queijos do Sertão', '99000000000028', '71970000028', '42840270'),
        ('Molhos e Sabores Nordeste', '99000000000029', '71970000029', '42840280'),
        ('Depósito de Bebidas Itacimirim', '99000000000030', '71970000030', '42840290'),
        ('Utilidades Praia Forte', '99000000000031', '71970000031', '42840300'),
        ('Distribuidora Linha Verde', '99000000000032', '71970000032', '42840310')
), normalized AS (
    SELECT *, LOWER(TRANSLATE(name,
        'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
        'aaaaeeiooouucAAAAEEIOOOUUC')) AS normalized_name
    FROM source
)
INSERT INTO suppliers(name, normalized_name, cnpj_digits, phone_digits, cep_digits)
SELECT name, normalized_name, cnpj_digits, phone_digits, cep_digits
FROM normalized
WHERE NOT EXISTS (
    SELECT 1 FROM suppliers supplier
    WHERE supplier.normalized_name = normalized.normalized_name
      AND supplier.deleted_at IS NULL
);

-- 60 espetos, 50 bebidas e 49 porções/complementos; somados ao item existente = 160.
WITH proteins AS (
    SELECT * FROM UNNEST(ARRAY[
        'Carne bovina', 'Frango', 'Linguiça toscana', 'Queijo coalho', 'Coração de frango',
        'Costela suína', 'Pernil suíno', 'Kafta bovina', 'Camarão', 'Tilápia',
        'Pão de alho', 'Legumes grelhados', 'Carne de sol', 'Bacon com queijo', 'Frango com bacon'
    ]) WITH ORDINALITY AS value(name, position)
), styles AS (
    SELECT * FROM UNNEST(ARRAY[
        'Tradicional', 'com chimichurri', 'com vinagrete', 'especial da casa'
    ]) WITH ORDINALITY AS value(name, position)
), skewers AS (
    SELECT 'Espeto de ' || proteins.name || ' ' || styles.name AS name,
           900 + proteins.position * 85 + styles.position * 75 AS price_cents
    FROM proteins CROSS JOIN styles
), beverages AS (
    SELECT beverage || ' ' || size AS name,
           450 + beverage_position * 55 + size_position * 120 AS price_cents
    FROM UNNEST(ARRAY[
        'Coca-Cola', 'Coca-Cola Zero', 'Guaraná Antarctica', 'Guaraná Zero', 'Fanta Laranja',
        'Sprite', 'Água mineral', 'Água com gás', 'Suco de laranja', 'Suco de maracujá'
    ]) WITH ORDINALITY AS drink(beverage, beverage_position)
    CROSS JOIN UNNEST(ARRAY['350 ml', '500 ml', '600 ml', '1 litro', '2 litros'])
        WITH ORDINALITY AS measure(size, size_position)
), portions AS (
    SELECT base || ' ' || style AS name,
           1200 + base_position * 135 + style_position * 190 AS price_cents
    FROM UNNEST(ARRAY[
        'Batata frita', 'Mandioca frita', 'Calabresa acebolada', 'Carne de sol', 'Frango a passarinho',
        'Queijo coalho', 'Isca de peixe', 'Camarão alho e óleo', 'Farofa da casa', 'Vinagrete'
    ]) WITH ORDINALITY AS portion(base, base_position)
    CROSS JOIN UNNEST(ARRAY['pequena', 'média', 'grande', 'com queijo', 'especial'])
        WITH ORDINALITY AS variation(style, style_position)
    LIMIT 49
), all_items AS (
    SELECT * FROM skewers
    UNION ALL SELECT * FROM beverages
    UNION ALL SELECT * FROM portions
), numbered AS (
    SELECT *, ROW_NUMBER() OVER (ORDER BY name) AS row_number
    FROM all_items
), supplier_list AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number,
           COUNT(*) OVER () AS total
    FROM suppliers
    WHERE deleted_at IS NULL
), prepared AS (
    SELECT numbered.name,
           LOWER(TRANSLATE(numbered.name,
               'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
               'aaaaeeiooouucAAAAEEIOOOUUC')) AS normalized_name,
           numbered.price_cents,
           20 + (numbered.row_number * 17 % 180) AS stock_quantity,
           5 + (numbered.row_number * 3 % 25) AS minimum_stock_quantity,
           supplier_list.id AS supplier_id
    FROM numbered
    JOIN supplier_list
      ON supplier_list.row_number = ((numbered.row_number - 1) % supplier_list.total) + 1
)
INSERT INTO catalog_entries(
    entry_type, name, normalized_name, price_cents,
    stock_quantity, minimum_stock_quantity, stock_enabled, supplier_id
)
SELECT 'ITEM', name, normalized_name, price_cents,
       stock_quantity, minimum_stock_quantity, TRUE, supplier_id
FROM prepared
ON CONFLICT (entry_type, normalized_name) WHERE deleted_at IS NULL DO NOTHING;

-- 41 serviços adicionais, totalizando 42 com o serviço já existente.
WITH service_names(name, price_cents) AS (
    VALUES
        ('Lavagem externa para compacto', 4000), ('Lavagem externa para sedã', 4500), ('Lavagem externa para SUV ou picape', 5500),
        ('Lavagem completa para compacto', 6500), ('Lavagem completa para sedã', 7500), ('Lavagem completa para SUV ou picape', 9000),
        ('Lavagem técnica para compacto', 9500), ('Lavagem técnica para sedã', 11000), ('Lavagem técnica para SUV ou picape', 13500),
        ('Lavagem detalhada para compacto', 12000), ('Lavagem detalhada para sedã', 14000), ('Lavagem detalhada para SUV ou picape', 16500),
        ('Lavagem ecológica para compacto', 5500), ('Lavagem ecológica para sedã', 6500), ('Lavagem ecológica para SUV ou picape', 8000),
        ('Lavagem de motocicleta urbana', 3000), ('Lavagem de motocicleta esportiva', 4500), ('Lavagem de motocicleta de grande porte', 6000),
        ('Aspiração interna simples', 2500), ('Aspiração interna detalhada', 4500), ('Higienização interna completa', 22000),
        ('Higienização de bancos em tecido', 16000), ('Higienização de bancos em couro', 18000), ('Higienização do teto', 9000),
        ('Higienização de carpete', 11000), ('Higienização do porta-malas', 6500), ('Limpeza de ar-condicionado', 8500),
        ('Enceramento manual', 9000), ('Enceramento técnico', 15000), ('Polimento comercial', 28000),
        ('Polimento técnico em uma etapa', 42000), ('Polimento técnico em duas etapas', 65000), ('Cristalização de pintura', 35000),
        ('Vitrificação de pintura', 95000), ('Revitalização de faróis', 18000), ('Revitalização de plásticos externos', 14000),
        ('Limpeza técnica do motor', 12000), ('Descontaminação de pintura', 16000), ('Remoção de chuva ácida dos vidros', 13000),
        ('Impermeabilização de bancos', 24000), ('Aplicação de repelente de água nos vidros', 7500)
), prepared AS (
    SELECT name,
           LOWER(TRANSLATE(name,
               'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
               'aaaaeeiooouucAAAAEEIOOOUUC')) AS normalized_name,
           price_cents
    FROM service_names
)
INSERT INTO catalog_entries(
    entry_type, name, normalized_name, price_cents,
    stock_quantity, minimum_stock_quantity, stock_enabled, supplier_id
)
SELECT 'SERVICE', name, normalized_name, price_cents,
       NULL, NULL, FALSE, NULL
FROM prepared
ON CONFLICT (entry_type, normalized_name) WHERE deleted_at IS NULL DO NOTHING;

-- Clientes com nomes, veículos, telefones e placas plausíveis e sem sufixos artificiais.
WITH first_names AS (
    SELECT * FROM UNNEST(ARRAY[
        'Adriana', 'Alexandre', 'Amanda', 'André', 'Beatriz', 'Carlos', 'Carolina', 'Daniel', 'Débora', 'Eduardo',
        'Elaine', 'Felipe', 'Gabriela', 'Gustavo', 'Helena', 'Igor', 'Isabela', 'José', 'Larissa', 'Leandro'
    ]) WITH ORDINALITY AS value(name, position)
), last_names AS (
    SELECT * FROM UNNEST(ARRAY[
        'Almeida', 'Barbosa', 'Cardoso', 'Dias', 'Freitas', 'Gonçalves', 'Lima', 'Menezes', 'Oliveira', 'Pereira', 'Rocha'
    ]) WITH ORDINALITY AS value(name, position)
), people AS (
    SELECT first_names.name || ' ' || last_names.name AS name,
           ((first_names.position - 1) * 11 + last_names.position)::integer AS row_number
    FROM first_names CROSS JOIN last_names
), vehicles AS (
    SELECT * FROM UNNEST(ARRAY[
        'Chevrolet Onix prata', 'Hyundai HB20 branco', 'Volkswagen Polo cinza', 'Fiat Argo vermelho',
        'Renault Kwid branco', 'Jeep Renegade preto', 'Toyota Corolla prata', 'Honda Civic cinza',
        'Nissan Kicks azul', 'Chevrolet Tracker branco', 'Volkswagen T-Cross preto', 'Fiat Strada prata',
        'Toyota Hilux branca', 'Ford Ranger cinza', 'Honda HR-V azul', 'Hyundai Creta preto',
        'Volkswagen Saveiro branca', 'Chevrolet S10 prata', 'Fiat Mobi vermelho', 'Renault Duster cinza'
    ]) WITH ORDINALITY AS value(name, position)
), plate_prefixes AS (
    SELECT * FROM UNNEST(ARRAY[
        'JQZ', 'NTD', 'OZD', 'PKH', 'PLR', 'QTS', 'RDA', 'RDM', 'RDR', 'RPA',
        'RPC', 'RPE', 'RPF', 'RPG', 'RPH', 'RPI', 'RPJ', 'RPK', 'RPL', 'RPM'
    ]) WITH ORDINALITY AS value(prefix, position)
), prepared AS (
    SELECT people.name,
           LOWER(TRANSLATE(people.name,
               'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
               'aaaaeeiooouucAAAAEEIOOOUUC')) AS normalized_name,
           '719' || LPAD(people.row_number::text, 8, '0') AS phone_digits,
           vehicles.name AS vehicle_name,
           LOWER(TRANSLATE(vehicles.name,
               'áàãâéêíóôõúüçÁÀÃÂÉÊÍÓÔÕÚÜÇ',
               'aaaaeeiooouucAAAAEEIOOOUUC')) AS normalized_vehicle_name,
           plate_prefixes.prefix || LPAD(people.row_number::text, 4, '0') AS plate
    FROM people
    JOIN vehicles ON vehicles.position = ((people.row_number - 1) % 20) + 1
    JOIN plate_prefixes ON plate_prefixes.position = ((people.row_number - 1) % 20) + 1
)
INSERT INTO clients(
    name, normalized_name, phone_digits,
    vehicle_name, normalized_vehicle_name, plate, active
)
SELECT name, normalized_name, phone_digits,
       vehicle_name, normalized_vehicle_name, plate,
       (RIGHT(plate, 1)::integer % 17) <> 0
FROM prepared
ON CONFLICT (plate) WHERE deleted_at IS NULL DO NOTHING;

COMMIT;

SELECT 'Usuários' AS categoria, COUNT(*) AS total
FROM users WHERE deleted_at IS NULL
UNION ALL
SELECT 'Fornecedores', COUNT(*) FROM suppliers WHERE deleted_at IS NULL
UNION ALL
SELECT 'Itens', COUNT(*) FROM catalog_entries WHERE deleted_at IS NULL AND entry_type = 'ITEM'
UNION ALL
SELECT 'Serviços', COUNT(*) FROM catalog_entries WHERE deleted_at IS NULL AND entry_type = 'SERVICE'
UNION ALL
SELECT 'Clientes', COUNT(*) FROM clients WHERE deleted_at IS NULL
ORDER BY categoria;
