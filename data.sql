create table users (
                       id bigserial primary key,
                       first_name varchar(100) not null,
                       last_name varchar(100) not null,
                       email varchar(255) not null unique,
                       password_hash varchar(255) not null,
                       role varchar(20) not null default 'USER',
                       created_at timestamp default CURRENT_TIMESTAMP
);

create table categories (
                            id bigserial primary key,
                            name varchar(255) not null,
                            parent_id bigint references categories(id) on delete cascade,
                            slug varchar(255) unique,
                            sort_order int default 0
);

create table products (
                          id bigserial primary key,
                          title varchar(255) not null,
                          description text,
                          image_url varchar(500),
                          base_price decimal(10, 2) not null,
                          discount_price decimal(10, 2),
                          stock_quantity int not null default 0,
                          created_at timestamp default current_timestamp,
                          updated_at timestamp default current_timestamp
);

create table product_categories (
                                    product_id bigint references products(id) on delete cascade,
                                    category_id bigint references categories(id) on delete cascade,
                                    primary key(product_id, category_id)
);

create table attributes (
                            id bigserial primary key,
                            name varchar(100) not null unique,
                            value_type varchar(20) default 'STRING'
);

CREATE TABLE product_attributes (
                                    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
                                    attribute_id BIGINT REFERENCES attributes(id) ON DELETE CASCADE,
                                    value VARCHAR(255) NOT NULL,
                                    PRIMARY KEY (product_id, attribute_id)
);

CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
                         user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                         rating INT CHECK (rating >= 1 AND rating <= 5),
                         comment_text TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE review_images (
                               id BIGSERIAL PRIMARY KEY,
                               review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
                               image_url VARCHAR(500) NOT NULL
);

CREATE TABLE carts (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT REFERENCES users(id) ON DELETE CASCADE UNIQUE
);

CREATE TABLE cart_items (
                            id BIGSERIAL PRIMARY KEY,
                            cart_id BIGINT REFERENCES carts(id) ON DELETE CASCADE,
                            product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
                            quantity INT NOT NULL DEFAULT 1,
                            UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT REFERENCES users(id),
                        total_amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) DEFAULT 'NEW', -- NEW, COMPLETED, CANCELLED
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
                             product_id BIGINT REFERENCES products(id),
                             quantity INT NOT NULL,
                             price_at_purchase DECIMAL(10, 2) NOT NULL -- Цена на момент покупки
);

INSERT INTO categories (name, parent_id, slug) VALUES
                                                   ('Электроника', NULL, 'electronics'),
                                                   ('Смартфоны', 1, 'smartphones'),
                                                   ('Apple', 2, 'apple'),
                                                   ('Samsung', 2, 'samsung'),
                                                   ('Huawei', 2, 'huawei'),
                                                   ('Сопутствующие товары', 2, 'accessories'),
                                                   ('Наушники', 6, 'accessories-headphones'),
                                                   ('Чехлы', 6, 'accessories-cases'),
                                                   ('Аудиотехника', 1, 'audio'),
                                                   ('Портативные колонки', 9, 'speakers'),
                                                   ('Наушники', 9, 'audio-headphones');

INSERT INTO products (title, description, image_url, base_price, discount_price, stock_quantity) VALUES
                                                                                                     ('IPhone 17', 'Флагманский смартфон Apple', 'img/iphone17.jpg', 120000.00, 115000.00, 10),
                                                                                                     ('IPhone Air', 'Легкий и тонкий iPhone', 'img/iphone_air.jpg', 90000.00, NULL, 3),
                                                                                                     ('Samsung Galaxy S25 Ultra', 'Мощный Android флагман', 'img/s25_ultra.jpg', 130000.00, 125000.00, 7),
                                                                                                     ('Samsung Galaxy Z Flip7', 'Раскладушка Samsung', 'img/z_flip7.jpg', 110000.00, NULL, 2),
                                                                                                     ('Huawei P50', 'Стильный смартфон Huawei', 'img/p50.jpg', 70000.00, 65000.00, 0),
                                                                                                     ('Чехол для Huawei P50', 'Защитный чехол', 'img/case_p50.jpg', 2000.00, NULL, 50),
                                                                                                     ('Умная колонка Яндекс Станция Дуо Макс', 'Колонка с экраном', 'img/station_duo.jpg', 25000.00, 23000.00, 15);

INSERT INTO products (title, description, image_url, base_price, discount_price, stock_quantity) VALUES
    ('AirPods Pro 3', 'Наушники с шумоподавлением', 'img/airpods_pro3.jpg', 25000.00, 24000.00, 20);

-- IPhone 17 (ID 1) -> Apple (ID 3)
INSERT INTO product_categories (product_id, category_id) VALUES (1, 3);
-- IPhone Air (ID 2) -> Apple (ID 3)
INSERT INTO product_categories (product_id, category_id) VALUES (2, 3);
-- Samsung S25 (ID 3) -> Samsung (ID 4)
INSERT INTO product_categories (product_id, category_id) VALUES (3, 4);
-- Samsung Flip7 (ID 4) -> Samsung (ID 4)
INSERT INTO product_categories (product_id, category_id) VALUES (8, 11);

INSERT INTO attributes (name, value_type) VALUES
                                              ('Память', 'STRING'),
                                              ('Цвет', 'STRING'),
                                              ('Диагональ', 'STRING');

INSERT INTO product_attributes (product_id, attribute_id, value) VALUES
                                                                     (1, 1, '256 ГБ'),
                                                                     (1, 2, 'Черный');

INSERT INTO users (first_name, last_name, email, password_hash, role) VALUES
                                                                          ('Иван', 'Иванов', 'ivan@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER'),
                                                                          ('Админ', 'Админов', 'admin@test.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'USER');

UPDATE users SET role = 'ADMIN' WHERE email = 'admin@test.com';
