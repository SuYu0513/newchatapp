-- friendshipsテーブルの内容を確認
SELECT 
    f.id,
    u1.username as requester,
    u2.username as addressee,
    f.status,
    f.created_at
FROM friendships f
LEFT JOIN users u1 ON f.requester_id = u1.id
LEFT JOIN users u2 ON f.addressee_id = u2.id
ORDER BY f.created_at DESC;

-- 特定のユーザー（最新のユーザー）が関係するフレンド申請を確認
SELECT 
    f.id,
    u1.username as requester,
    u2.username as addressee,
    f.status,
    f.created_at
FROM friendships f
LEFT JOIN users u1 ON f.requester_id = u1.id
LEFT JOIN users u2 ON f.addressee_id = u2.id
WHERE u1.id = (SELECT MAX(id) FROM users) 
   OR u2.id = (SELECT MAX(id) FROM users)
ORDER BY f.created_at DESC;

-- 全ユーザー一覧
SELECT id, username, email, created_at FROM users ORDER BY created_at DESC;
