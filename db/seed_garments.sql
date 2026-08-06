-- 의류 시드 데이터 (확정본 6종)
--
-- 운영 프로필은 spring.sql.init.mode: never 라 data.sql 이 실행되지 않습니다.
-- 스키마 적용 후 이 파일을 직접 실행해 주세요.
--
--   docker exec -i closr-db psql -U closr -d closr < db/seed_garments.sql
--
-- 여러 번 실행해도 중복되지 않습니다 (design 유니크 제약 + ON CONFLICT).
-- 내용은 src/main/resources/data.sql 과 동일합니다. 한쪽을 고치면 다른 쪽도 고쳐주세요.
--
-- garment_size_specs 는 스펙 확정 전이라 시드가 없습니다.
INSERT INTO garments (design, name, category, created_at, updated_at) VALUES
    ('tshirt_basic',  '베이직 티셔츠',  'top',    NOW(), NOW()),
    ('shirt_slim',    '슬림 셔츠',      'top',    NOW(), NOW()),
    ('shirt_over',    '오버핏 셔츠',    'top',    NOW(), NOW()),
    ('dress_basic',   '베이직 원피스',  'dress',  NOW(), NOW()),
    ('pants_slacks',  '슬랙스',         'bottom', NOW(), NOW()),
    ('skirt_pencil',  '펜슬 스커트',    'bottom', NOW(), NOW())
ON CONFLICT (design) DO NOTHING;
