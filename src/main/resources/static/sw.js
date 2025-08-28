const CACHE_NAME = 'chat-app-v11';
const urlsToCache = [
  '/',
  '/login',
  '/chat',
  '/css/chat-style.css',
  '/css/kawaii-theme.css',
  '/css/mobile.css',
  '/js/kawaii-theme.js',
  '/js/pwa.js',
  '/images/default-avatar.svg',
  '/images/app-icon.svg',
  '/images/app-icon-48.svg',
  '/images/app-icon-72.svg',
  '/images/app-icon-96.svg',
  '/images/app-icon-144.svg',
  '/images/app-icon-180.svg',
  '/images/app-icon-180.png',
  '/images/app-icon-192.svg',
  '/images/app-icon-192-enhanced.svg',
  '/manifest.json',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css',
  'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js'
];

// Service Worker インストール時
self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(function(cache) {
        console.log('Service Worker: キャッシュを開いています');
        return cache.addAll(urlsToCache);
      })
  );
});

// キャッシュからレスポンスを返す
self.addEventListener('fetch', function(event) {
  // リダイレクトレスポンスの場合は直接返す
  if (event.request.redirect === 'manual') {
    event.respondWith(fetch(event.request));
    return;
  }
  
  event.respondWith(
    caches.match(event.request)
      .then(function(response) {
        // キャッシュにある場合はそれを返す
        if (response) {
          return response;
        }
        
        // キャッシュにない場合はネットワークから取得
        return fetch(event.request, { redirect: 'follow' }).then(function(response) {
          // レスポンスが有効でない場合やリダイレクトの場合はそのまま返す
          if (!response || response.status !== 200 || response.type !== 'basic' || response.redirected) {
            return response;
          }
          
          // レスポンスをクローンしてキャッシュに保存
          var responseToCache = response.clone();
          caches.open(CACHE_NAME)
            .then(function(cache) {
              cache.put(event.request, responseToCache);
            });
          
          return response;
        }).catch(function(error) {
          console.log('Service Worker: フェッチエラー', error);
          throw error;
        });
      })
  );
});

// 古いキャッシュの削除
self.addEventListener('activate', function(event) {
  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(
        cacheNames.map(function(cacheName) {
          if (cacheName !== CACHE_NAME) {
            console.log('Service Worker: 古いキャッシュを削除しています', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
