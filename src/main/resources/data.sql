-- 의류 시드 데이터 (확정본 6종)
--
-- local 프로필에서만 실행됩니다 (spring.sql.init.mode).
-- 매 기동마다 실행되므로 design 유니크 제약을 이용해 중복 삽입을 막습니다.
--
-- garment_size_specs 는 스펙 확정 전이라 비워 둡니다.
INSERT INTO garments (design, name, category, created_at, updated_at) VALUES
    ('tshirt_basic',  '베이직 티셔츠',  'top',    NOW(), NOW()),
    ('shirt_slim',    '슬림 셔츠',      'top',    NOW(), NOW()),
    ('shirt_over',    '오버핏 셔츠',    'top',    NOW(), NOW()),
    ('dress_basic',   '베이직 원피스',  'dress',  NOW(), NOW()),
    ('pants_slacks',  '슬랙스',         'bottom', NOW(), NOW()),
    ('skirt_pencil',  '펜슬 스커트',    'bottom', NOW(), NOW())
ON CONFLICT (design) DO NOTHING;
