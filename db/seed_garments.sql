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
-- 사이즈 스펙과 핏별 허용 범위도 함께 넣습니다.
-- 운영은 spring.sql.init.mode: never 라 data.sql 이 돌지 않으므로,
-- 이 파일이 운영 DB 에 값을 반영하는 유일한 경로입니다.
INSERT INTO garments (design, name, category, created_at, updated_at) VALUES
    ('tshirt_basic',  '베이직 티셔츠',  'top',    NOW(), NOW()),
    ('shirt_slim',    '슬림 셔츠',      'top',    NOW(), NOW()),
    ('shirt_over',    '오버핏 셔츠',    'top',    NOW(), NOW()),
    ('dress_basic',   '베이직 원피스',  'dress',  NOW(), NOW()),
    ('pants_slacks',  '슬랙스',         'bottom', NOW(), NOW()),
    ('skirt_pencil',  '펜슬 스커트',    'bottom', NOW(), NOW())
ON CONFLICT (design) DO NOTHING;

-- 사이즈 표기를 소문자로 정리합니다.
--
-- 유니크 키가 (garment_id, size) 라서 'M' 이 남아 있는 상태로 'm' 을 넣으면
-- 충돌이 나지 않고 새 행이 생깁니다. 디자인마다 3행이 6행이 되고 판정이
-- 사이즈 6개를 대상으로 돌게 됩니다. 그래서 INSERT 전에 먼저 내립니다.
--
-- 소문자로 통일하는 이유는 R2 파일명입니다. 키가
-- {design}_{size}__{bucket}.glb 이고 R2 는 대소문자를 구분하므로,
-- 'M' 을 그대로 조합하면 _M__ 이 되어 404 가 납니다.
-- 이미 소문자 행이 있는 상태에서 대문자 행을 lower() 하면 유니크 키
-- uk_garment_size (garment_id, size) 에 걸려 기동이 실패합니다.
--   ERROR: duplicate key value violates unique constraint "uk_garment_size"
-- 그러면 dataSourceScriptDatabaseInitializer 빈 생성이 깨져 서버가 아예 뜨지 않습니다.
--
-- 두 표기가 섞이는 경로는 둘입니다.
--   브랜치를 옮겨 다니며 테스트를 돌린 로컬 DB
--   운영에 db/seed_garments.sql 을 수동으로 먼저 돌린 뒤 배포
--
-- 그래서 짝이 있는 대문자 행은 먼저 지우고, 남은 것만 소문자로 내립니다.
-- 지운 자리는 아래 INSERT 가 다시 채우므로 데이터가 사라지지 않습니다.
DELETE FROM garment_size_specs u
 WHERE u.size <> lower(u.size)
   AND EXISTS (SELECT 1 FROM garment_size_specs l
                WHERE l.garment_id = u.garment_id AND l.size = lower(u.size));

UPDATE garment_size_specs SET size = lower(size) WHERE size <> lower(size);
UPDATE fitting_records   SET recommended_size = lower(recommended_size)
 WHERE recommended_size IS NOT NULL AND recommended_size <> lower(recommended_size);

INSERT INTO garment_size_specs (garment_id, size, measurements, target_ease, created_at, updated_at)
SELECT g.id, v.size, v.measurements::jsonb, v.target_ease::jsonb, NOW(), NOW()
FROM (VALUES
    ('tshirt_basic', 's', '{"shoulder_width": 35.0, "chest_circ": 99.0}', '{"shoulder_width": -7.9, "chest_circ": 15.9}'),
    ('tshirt_basic', 'm', '{"shoulder_width": 37.0, "chest_circ": 108.0}', '{"shoulder_width": -9.0, "chest_circ": 14.0}'),
    ('tshirt_basic', 'l', '{"shoulder_width": 38.7, "chest_circ": 114.0}', '{"shoulder_width": -8.9, "chest_circ": 13.0}'),
    ('shirt_slim', 's', '{"shoulder_width": 35.0, "chest_circ": 93.0}', '{"shoulder_width": -7.9, "chest_circ": 9.9}'),
    ('shirt_slim', 'm', '{"shoulder_width": 37.0, "chest_circ": 102.0}', '{"shoulder_width": -9.0, "chest_circ": 8.0}'),
    ('shirt_slim', 'l', '{"shoulder_width": 38.6, "chest_circ": 108.0}', '{"shoulder_width": -9.0, "chest_circ": 7.0}'),
    ('shirt_over', 's', '{"shoulder_width": 35.0, "chest_circ": 115.0}', '{"shoulder_width": -7.9, "chest_circ": 31.9}'),
    ('shirt_over', 'm', '{"shoulder_width": 37.0, "chest_circ": 124.0}', '{"shoulder_width": -9.0, "chest_circ": 30.0}'),
    ('shirt_over', 'l', '{"shoulder_width": 39.1, "chest_circ": 130.0}', '{"shoulder_width": -8.5, "chest_circ": 29.0}'),
    ('dress_basic', 's', '{"shoulder_width": 35.0, "chest_circ": 99.0, "hip_circ": 97.0}', '{"shoulder_width": -7.9, "chest_circ": 15.9, "hip_circ": 7.9}'),
    ('dress_basic', 'm', '{"shoulder_width": 37.0, "chest_circ": 108.0, "hip_circ": 106.0}', '{"shoulder_width": -9.0, "chest_circ": 14.0, "hip_circ": 7.3}'),
    ('dress_basic', 'l', '{"shoulder_width": 38.7, "chest_circ": 114.0, "hip_circ": 112.0}', '{"shoulder_width": -8.9, "chest_circ": 13.0, "hip_circ": 7.3}'),
    ('pants_slacks', 's', '{"waist_circ": 71.5}', '{"waist_circ": 10.8}'),
    ('pants_slacks', 'm', '{"waist_circ": 80.1}', '{"waist_circ": 5.2}'),
    ('pants_slacks', 'l', '{"waist_circ": 85.8}', '{"waist_circ": 2.3}'),
    ('skirt_pencil', 's', '{"waist_circ": 71.0, "hip_circ": 97.0}', '{"waist_circ": 10.3, "hip_circ": 7.9}'),
    ('skirt_pencil', 'm', '{"waist_circ": 80.0, "hip_circ": 106.0}', '{"waist_circ": 5.1, "hip_circ": 7.3}'),
    ('skirt_pencil', 'l', '{"waist_circ": 86.0, "hip_circ": 112.0}', '{"waist_circ": 2.5, "hip_circ": 7.3}')
) AS v(design, size, measurements, target_ease)
JOIN garments g ON g.design = v.design
ON CONFLICT (garment_id, size) DO UPDATE
   SET measurements = EXCLUDED.measurements,
       target_ease  = EXCLUDED.target_ease
 WHERE garment_size_specs.measurements IS DISTINCT FROM EXCLUDED.measurements
    OR garment_size_specs.target_ease  IS DISTINCT FROM EXCLUDED.target_ease;

-- 핏 · 부위별 허용 편차 범위 (fit_judge.py 의 TOL_BY_FIT).
--
-- 편차가 범위 안이면 적정, dev_min 보다 작으면 꽉 낌, dev_max 보다 크면 여유 있음입니다.
-- 어깨는 폭이 가장 좁습니다. 어깨가 안 맞으면 다른 곳이 맞아도 못 입기 때문입니다.
--
-- 핏에만 종속되어 의류가 늘어도 이 표는 그대로입니다.
INSERT INTO fit_tolerances (fit, part, dev_min, dev_max, created_at, updated_at)
SELECT v.fit, v.part, v.dev_min, v.dev_max, NOW(), NOW()
FROM (VALUES
    ('슬림', 'chest_circ', -2, 3),
    ('슬림', 'waist_circ', -2, 3),
    ('슬림', 'hip_circ', -2, 3),
    ('슬림', 'shoulder_width', -2, 2),
    ('레귤러', 'chest_circ', -4, 6),
    ('레귤러', 'waist_circ', -4, 6),
    ('레귤러', 'hip_circ', -4, 6),
    ('레귤러', 'shoulder_width', -2, 3),
    ('오버핏', 'chest_circ', -6, 12),
    ('오버핏', 'waist_circ', -6, 12),
    ('오버핏', 'hip_circ', -6, 12),
    ('오버핏', 'shoulder_width', -2, 5)
) AS v(fit, part, dev_min, dev_max)
-- DO NOTHING 이면 값이 바뀌어도 기존 행이 그대로 남습니다. 어깨 허용 범위가
-- 의류 파트 CSV 와 어긋난 채로 판정이 돌던 원인이라 DO UPDATE 로 바꿉니다.
ON CONFLICT (fit, part) DO UPDATE
   SET dev_min = EXCLUDED.dev_min,
       dev_max = EXCLUDED.dev_max
 WHERE fit_tolerances.dev_min IS DISTINCT FROM EXCLUDED.dev_min
    OR fit_tolerances.dev_max IS DISTINCT FROM EXCLUDED.dev_max;
