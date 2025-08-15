-- メインルームの初期化（created_byは後で更新される想定）
-- JPA Hibernateがテーブルを作成した後に実行される
INSERT OR IGNORE INTO chat_rooms (id, name, type, created_by, created_at) 
SELECT 1, 'メインルーム', 'GROUP', 
       COALESCE((SELECT id FROM users LIMIT 1), 1), 
       datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM chat_rooms WHERE id = 1);
