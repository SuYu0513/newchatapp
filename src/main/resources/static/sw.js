const CACHE_NAME = 'chat-app-v20'; // API POST修正
const urlsToCache = [
  '/css/chat-style.css',
  '/css/kawaii-theme.css',
  '/js/kawaii-theme.js',
  '/images/app-icon-192-enhanced.svg',
  '/images/app-icon.svg',
  '/manifest.json'
];

// Service Worker インストール時
self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(function(cache) {
        return cache.addAll(urlsToCache);
      })
      .then(function() {
        return self.skipWaiting();
      })
  );
});

// キャッシュからレスポンスを返す
self.addEventListener('fetch', function(event) {
  const url = new URL(event.request.url);
  
  // WebSocketリクエストは無視
  if (url.pathname.includes('/ws')) {
    return;
  }
  
  // POSTリクエストはサービスワーカーを通さずそのまま通す
  if (event.request.method !== 'GET') {
    return;
  }

  // APIリクエスト（GET）は常にネットワークから取得（キャッシュしない）
  if (url.pathname.startsWith('/api/') ||
      url.pathname.includes('/api/') ||
      url.pathname.startsWith('/rooms/')) {
    event.respondWith(
      fetch(event.request, {
        cache: 'no-store'
      })
    );
    return;
  }
  
  // リダイレクトレスポンスの場合は直接返す
  if (event.request.redirect === 'manual') {
    event.respondWith(fetch(event.request));
    return;
  }
  
  // HTMLページは常にネットワークから取得（Network First戦略）
  if (event.request.headers.get('accept')?.includes('text/html') ||
      url.pathname === '/' ||
      url.pathname === '/login' ||
      url.pathname === '/home' ||
      url.pathname === '/chat' ||
      url.pathname.endsWith('.html')) {
    event.respondWith(
      fetch(event.request, { redirect: 'follow' })
        .then(function(response) {
          // 成功した場合はレスポンスを返す（キャッシュしない）
          return response;
        })
        .catch(function(error) {
          // ネットワークエラーの場合はキャッシュから返す（オフライン対応）
          return caches.match(event.request).then(function(cachedResponse) {
            return cachedResponse || Response.error();
          });
        })
    );
    return;
  }
  
  // 静的リソース（CSS/JS/画像）はCache First戦略
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
            return caches.delete(cacheName);
          }
        })
      );
    }).then(function() {
      return self.clients.claim();
    })
  );
});
