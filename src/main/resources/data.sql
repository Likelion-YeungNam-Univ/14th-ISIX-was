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

-- 의류 사이즈별 실측 스펙 (확정본, 시뮬레이션 담당 제공)
--
-- 여유량은 (의류 스펙 - 아바타 치수) 로 계산합니다. 양수면 여유입니다.
--
-- 부위는 검증이 끝난 4개만 씁니다.
--   shoulder_width, chest_circ, waist_circ, hip_circ
-- sleeve_length · back_length 는 패턴에서 측정은 되지만 아바타 계측과
-- 정의가 같은지 확인되지 않아 제외했습니다.
--
-- 의류별 부위 구성
--   상의   chest_circ, shoulder_width
--   원피스 chest_circ, shoulder_width, hip_circ
--   바지   waist_circ
--   스커트 waist_circ, hip_circ
--
-- design 으로 garments 를 찾아 연결하므로 위 INSERT 가 먼저 실행돼야 합니다.
-- (garment_id, size) 유니크 제약으로 매 기동마다 실행돼도 중복되지 않습니다.
INSERT INTO garment_size_specs (garment_id, size, measurements, created_at, updated_at)
SELECT g.id, v.size, v.measurements::jsonb, NOW(), NOW()
FROM (VALUES
    ('tshirt_basic', 'S', '{"shoulder_width": 35.0, "chest_circ": 99.0}'),
    ('tshirt_basic', 'M', '{"shoulder_width": 37.0, "chest_circ": 108.0}'),
    ('tshirt_basic', 'L', '{"shoulder_width": 38.7, "chest_circ": 114.0}'),
    ('shirt_slim', 'S', '{"shoulder_width": 35.0, "chest_circ": 93.0}'),
    ('shirt_slim', 'M', '{"shoulder_width": 37.0, "chest_circ": 102.0}'),
    ('shirt_slim', 'L', '{"shoulder_width": 38.6, "chest_circ": 108.0}'),
    ('shirt_over', 'S', '{"shoulder_width": 35.0, "chest_circ": 115.0}'),
    ('shirt_over', 'M', '{"shoulder_width": 37.0, "chest_circ": 124.0}'),
    ('shirt_over', 'L', '{"shoulder_width": 39.1, "chest_circ": 130.0}'),
    ('dress_basic', 'S', '{"shoulder_width": 35.0, "chest_circ": 99.0, "hip_circ": 97.0}'),
    ('dress_basic', 'M', '{"shoulder_width": 37.0, "chest_circ": 108.0, "hip_circ": 106.0}'),
    ('dress_basic', 'L', '{"shoulder_width": 38.7, "chest_circ": 114.0, "hip_circ": 112.0}'),
    ('pants_slacks', 'S', '{"waist_circ": 71.5}'),
    ('pants_slacks', 'M', '{"waist_circ": 80.1}'),
    ('pants_slacks', 'L', '{"waist_circ": 85.8}'),
    ('skirt_pencil', 'S', '{"waist_circ": 71.0, "hip_circ": 97.0}'),
    ('skirt_pencil', 'M', '{"waist_circ": 80.0, "hip_circ": 106.0}'),
    ('skirt_pencil', 'L', '{"waist_circ": 86.0, "hip_circ": 112.0}')
) AS v(design, size, measurements)
JOIN garments g ON g.design = v.design
ON CONFLICT (garment_id, size) DO NOTHING;
