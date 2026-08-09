-- 의류 시드 데이터
--
-- local · prod 양쪽에서 기동할 때마다 실행됩니다 (spring.sql.init.mode).
-- 여러 번 실행돼도 안전하도록 모두 멱등하게 작성합니다.
--
-- 원본은 시뮬레이션 담당이 제공한 CSV 3종이며, 손으로 옮기지 않고 생성했습니다.
--   garment_size_specs.csv   부위별 실측 치수
--   garment_target_ease.csv  부위별 목표 여유
--   garment_tolerance.csv    핏별 허용 편차 범위

-- 의류 6종.
-- fit 은 나중에 추가된 컬럼이라 기존 행에도 채워지도록 DO UPDATE 를 씁니다.
-- 값이 같으면 쓰지 않아 매 기동마다 updated_at 이 바뀌지는 않습니다.
INSERT INTO garments (design, name, category, fit, created_at, updated_at)
SELECT v.design, v.name, v.category, v.fit, NOW(), NOW()
FROM (VALUES
    ('tshirt_basic', '베이직 티셔츠', 'top', '레귤러'),
    ('shirt_slim', '슬림 셔츠', 'top', '슬림'),
    ('shirt_over', '오버핏 셔츠', 'top', '오버핏'),
    ('dress_basic', '베이직 원피스', 'dress', '레귤러'),
    ('pants_slacks', '슬랙스', 'bottom', '레귤러'),
    ('skirt_pencil', '펜슬 스커트', 'bottom', '레귤러')
) AS v(design, name, category, fit)
ON CONFLICT (design) DO UPDATE
   SET fit = EXCLUDED.fit
 WHERE garments.fit IS DISTINCT FROM EXCLUDED.fit;

-- 사이즈별 실측 치수와 목표 여유.
--
-- 판정은 실제 여유가 아니라 목표 대비 편차로 합니다.
--   실제 여유 = 의류 치수 - 아바타 치수
--   편차      = 실제 여유 - 목표 여유
--
-- 부위는 검증이 끝난 4개만 씁니다.
--   shoulder_width, chest_circ, waist_circ, hip_circ
-- sleeve_length · back_length 는 패턴에서 측정은 되지만 아바타 계측과
-- 정의가 같은지 확인되지 않아 제외했습니다.
--
-- design 으로 garments 를 찾으므로 위 INSERT 가 먼저 실행돼야 합니다.
INSERT INTO garment_size_specs (garment_id, size, measurements, target_ease, created_at, updated_at)
SELECT g.id, v.size, v.measurements::jsonb, v.target_ease::jsonb, NOW(), NOW()
FROM (VALUES
    ('tshirt_basic', 'S', '{"shoulder_width": 35.0, "chest_circ": 99.0}', '{"shoulder_width": -2.0, "chest_circ": 14.0}'),
    ('tshirt_basic', 'M', '{"shoulder_width": 37.0, "chest_circ": 108.0}', '{"shoulder_width": -2.0, "chest_circ": 14.0}'),
    ('tshirt_basic', 'L', '{"shoulder_width": 38.7, "chest_circ": 114.0}', '{"shoulder_width": -1.8, "chest_circ": 14.0}'),
    ('shirt_slim', 'S', '{"shoulder_width": 35.0, "chest_circ": 93.0}', '{"shoulder_width": -2.0, "chest_circ": 8.0}'),
    ('shirt_slim', 'M', '{"shoulder_width": 37.0, "chest_circ": 102.0}', '{"shoulder_width": -2.0, "chest_circ": 8.0}'),
    ('shirt_slim', 'L', '{"shoulder_width": 38.6, "chest_circ": 108.0}', '{"shoulder_width": -1.9, "chest_circ": 8.0}'),
    ('shirt_over', 'S', '{"shoulder_width": 35.0, "chest_circ": 115.0}', '{"shoulder_width": -2.0, "chest_circ": 30.0}'),
    ('shirt_over', 'M', '{"shoulder_width": 37.0, "chest_circ": 124.0}', '{"shoulder_width": -2.0, "chest_circ": 30.0}'),
    ('shirt_over', 'L', '{"shoulder_width": 39.1, "chest_circ": 130.0}', '{"shoulder_width": -1.4, "chest_circ": 30.0}'),
    ('dress_basic', 'S', '{"shoulder_width": 35.0, "chest_circ": 99.0, "hip_circ": 97.0}', '{"shoulder_width": -2.0, "chest_circ": 14.0, "hip_circ": 6.0}'),
    ('dress_basic', 'M', '{"shoulder_width": 37.0, "chest_circ": 108.0, "hip_circ": 106.0}', '{"shoulder_width": -2.0, "chest_circ": 14.0, "hip_circ": 6.0}'),
    ('dress_basic', 'L', '{"shoulder_width": 38.7, "chest_circ": 114.0, "hip_circ": 112.0}', '{"shoulder_width": -1.8, "chest_circ": 14.0, "hip_circ": 6.0}'),
    ('pants_slacks', 'S', '{"waist_circ": 71.5}', '{"waist_circ": 2.5}'),
    ('pants_slacks', 'M', '{"waist_circ": 80.1}', '{"waist_circ": 2.1}'),
    ('pants_slacks', 'L', '{"waist_circ": 85.8}', '{"waist_circ": 1.8}'),
    ('skirt_pencil', 'S', '{"waist_circ": 71.0, "hip_circ": 97.0}', '{"waist_circ": 2.0, "hip_circ": 6.0}'),
    ('skirt_pencil', 'M', '{"waist_circ": 80.0, "hip_circ": 106.0}', '{"waist_circ": 2.0, "hip_circ": 6.0}'),
    ('skirt_pencil', 'L', '{"waist_circ": 86.0, "hip_circ": 112.0}', '{"waist_circ": 2.0, "hip_circ": 6.0}')
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
    ('슬림', 'shoulder_width', -1, 1),
    ('레귤러', 'chest_circ', -4, 6),
    ('레귤러', 'waist_circ', -4, 6),
    ('레귤러', 'hip_circ', -4, 6),
    ('레귤러', 'shoulder_width', -1, 2),
    ('오버핏', 'chest_circ', -6, 12),
    ('오버핏', 'waist_circ', -6, 12),
    ('오버핏', 'hip_circ', -6, 12),
    ('오버핏', 'shoulder_width', -1, 4)
) AS v(fit, part, dev_min, dev_max)
ON CONFLICT (fit, part) DO NOTHING;
