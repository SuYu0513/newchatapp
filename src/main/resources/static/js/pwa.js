// PWA機能の管理
class PWAManager {
    constructor() {
        this.deferredPrompt = null;
        this.isInstalled = false;
        this.init();
    }

    init() {
        // Service Worker登録
        this.registerServiceWorker();
        
        // インストールプロンプトの管理
        this.setupInstallPrompt();
        
        // アプリインストール状態の確認
        this.checkInstallStatus();
        
        // オンライン/オフライン状態の監視
        this.setupOnlineOfflineHandling();
        
        // モバイル最適化
        this.setupMobileOptimizations();
    }

    // Service Worker登録
    async registerServiceWorker() {
        if ('serviceWorker' in navigator) {
            try {
                const registration = await navigator.serviceWorker.register('/sw.js');
                console.log('Service Worker registered successfully:', registration.scope);
                
                // 更新チェック
                registration.addEventListener('updatefound', () => {
                    const newWorker = registration.installing;
                    newWorker.addEventListener('statechange', () => {
                        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                            this.showUpdateAvailable();
                        }
                    });
                });
            } catch (error) {
                console.log('Service Worker registration failed:', error);
            }
        }
    }

    // インストールプロンプトの設定
    setupInstallPrompt() {
        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            this.deferredPrompt = e;
            this.showInstallPrompt();
        });

        window.addEventListener('appinstalled', () => {
            console.log('PWA was installed');
            this.isInstalled = true;
            this.hideInstallPrompt();
        });
    }

    // インストールプロンプト表示
    showInstallPrompt() {
        if (this.isInstalled) return;

        const prompt = document.createElement('div');
        prompt.className = 'install-prompt';
        prompt.innerHTML = `
            <div class="d-flex align-items-center justify-content-between">
                <div>
                    <h6 class="mb-1">📱 アプリをインストール</h6>
                    <small class="text-muted">ホーム画面に追加して簡単アクセス</small>
                </div>
                <div>
                    <button class="btn btn-primary btn-sm me-2" onclick="pwaManager.installApp()">
                        インストール
                    </button>
                    <button class="btn btn-outline-secondary btn-sm" onclick="pwaManager.hideInstallPrompt()">
                        ×
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(prompt);
        setTimeout(() => prompt.classList.add('show'), 100);
    }

    // アプリインストール実行
    async installApp() {
        if (!this.deferredPrompt) return;

        const result = await this.deferredPrompt.prompt();
        console.log('Install prompt result:', result.outcome);
        
        this.deferredPrompt = null;
        this.hideInstallPrompt();
    }

    // インストールプロンプト非表示
    hideInstallPrompt() {
        const prompt = document.querySelector('.install-prompt');
        if (prompt) {
            prompt.remove();
        }
    }

    // インストール状態確認
    checkInstallStatus() {
        // スタンドアロンモードかチェック
        if (window.matchMedia('(display-mode: standalone)').matches || 
            window.navigator.standalone === true) {
            this.isInstalled = true;
            document.body.classList.add('standalone-mode');
        }
    }

    // オンライン/オフライン処理
    setupOnlineOfflineHandling() {
        window.addEventListener('online', () => {
            this.showOnlineStatus('オンラインに復帰しました', 'success');
        });

        window.addEventListener('offline', () => {
            this.showOnlineStatus('オフラインモードです', 'warning');
        });
    }

    // オンライン状態表示
    showOnlineStatus(message, type) {
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} position-fixed`;
        toast.style.cssText = `
            top: 20px;
            right: 20px;
            z-index: 9999;
            border-radius: 10px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        `;
        toast.textContent = message;
        
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }

    // 更新通知
    showUpdateAvailable() {
        const updatePrompt = document.createElement('div');
        updatePrompt.className = 'alert alert-info position-fixed';
        updatePrompt.style.cssText = `
            bottom: 20px;
            left: 20px;
            right: 20px;
            z-index: 9999;
            border-radius: 15px;
        `;
        updatePrompt.innerHTML = `
            <div class="d-flex align-items-center justify-content-between">
                <span>新しいバージョンが利用可能です</span>
                <button class="btn btn-outline-primary btn-sm" onclick="location.reload()">
                    更新
                </button>
            </div>
        `;
        
        document.body.appendChild(updatePrompt);
    }

    // モバイル最適化
    setupMobileOptimizations() {
        // ビューポート高さの調整（モバイルブラウザ対応）
        const setViewportHeight = () => {
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        };

        setViewportHeight();
        window.addEventListener('resize', setViewportHeight);
        window.addEventListener('orientationchange', () => {
            setTimeout(setViewportHeight, 100);
        });

        // タッチ操作の改善
        this.setupTouchOptimizations();
        
        // キーボード表示時の調整
        this.setupKeyboardHandling();
    }

    // タッチ操作最適化
    setupTouchOptimizations() {
        // 長押しメニュー無効化
        document.addEventListener('contextmenu', (e) => {
            if (e.target.closest('.message-bubble')) {
                e.preventDefault();
            }
        });

        // ダブルタップズーム無効化
        let lastTouchEnd = 0;
        document.addEventListener('touchend', (e) => {
            const now = (new Date()).getTime();
            if (now - lastTouchEnd <= 300) {
                e.preventDefault();
            }
            lastTouchEnd = now;
        }, false);
    }

    // キーボード表示時の調整
    setupKeyboardHandling() {
        const initialViewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
        
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', () => {
                const currentHeight = window.visualViewport.height;
                const heightDiff = initialViewportHeight - currentHeight;
                
                // キーボードが表示されている場合
                if (heightDiff > 150) {
                    document.body.classList.add('keyboard-open');
                    
                    // チャットメッセージを最下部にスクロール
                    const chatMessages = document.querySelector('.chat-messages');
                    if (chatMessages) {
                        setTimeout(() => {
                            chatMessages.scrollTop = chatMessages.scrollHeight;
                        }, 100);
                    }
                } else {
                    document.body.classList.remove('keyboard-open');
                }
            });
        }
    }
}

// モバイル用チャット機能拡張
class MobileChatEnhancements {
    constructor() {
        this.init();
    }

    init() {
        // スワイプ操作の追加
        this.setupSwipeGestures();
        
        // プルツーリフレッシュ
        this.setupPullToRefresh();
        
        // メッセージの長押し操作
        this.setupMessageLongPress();
    }

    // スワイプ操作
    setupSwipeGestures() {
        let startX, startY, endX, endY;

        document.addEventListener('touchstart', (e) => {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
        });

        document.addEventListener('touchend', (e) => {
            if (!startX || !startY) return;

            endX = e.changedTouches[0].clientX;
            endY = e.changedTouches[0].clientY;

            const diffX = startX - endX;
            const diffY = startY - endY;

            // 横スワイプでサイドバー切り替え（チャット画面のみ）
            if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 50) {
                if (window.location.pathname === '/chat') {
                    this.toggleSidebar(diffX > 0 ? 'left' : 'right');
                }
            }

            startX = startY = endX = endY = null;
        });
    }

    // サイドバー切り替え
    toggleSidebar(direction) {
        const sidebar = document.querySelector('.chat-sidebar');
        const main = document.querySelector('.chat-main');
        
        if (direction === 'left') {
            // 左スワイプ：サイドバーを隠す
            sidebar.style.transform = 'translateX(-100%)';
            main.style.width = '100%';
        } else {
            // 右スワイプ：サイドバーを表示
            sidebar.style.transform = 'translateX(0)';
            main.style.width = 'calc(100% - 250px)';
        }
    }

    // プルツーリフレッシュ
    setupPullToRefresh() {
        let startY = 0;
        let isPulling = false;

        const chatMessages = document.querySelector('.chat-messages');
        if (!chatMessages) return;

        chatMessages.addEventListener('touchstart', (e) => {
            if (chatMessages.scrollTop === 0) {
                startY = e.touches[0].clientY;
                isPulling = true;
            }
        });

        chatMessages.addEventListener('touchmove', (e) => {
            if (isPulling && chatMessages.scrollTop === 0) {
                const currentY = e.touches[0].clientY;
                const diff = currentY - startY;

                if (diff > 50) {
                    e.preventDefault();
                    // リフレッシュインジケーター表示
                    this.showRefreshIndicator();
                }
            }
        });

        chatMessages.addEventListener('touchend', () => {
            if (isPulling) {
                isPulling = false;
                // メッセージリフレッシュ実行
                this.refreshMessages();
            }
        });
    }

    // リフレッシュインジケーター
    showRefreshIndicator() {
        let indicator = document.querySelector('.refresh-indicator');
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.className = 'refresh-indicator';
            indicator.innerHTML = '<i class="fas fa-sync-alt fa-spin"></i>';
            indicator.style.cssText = `
                position: absolute;
                top: 10px;
                left: 50%;
                transform: translateX(-50%);
                color: #667eea;
                font-size: 1.2rem;
                z-index: 1000;
            `;
            document.querySelector('.chat-messages').appendChild(indicator);
        }
    }

    // メッセージリフレッシュ
    refreshMessages() {
        const indicator = document.querySelector('.refresh-indicator');
        if (indicator) {
            setTimeout(() => {
                indicator.remove();
                // ここで実際のメッセージ更新処理を実行
                location.reload();
            }, 1000);
        }
    }

    // メッセージ長押し操作
    setupMessageLongPress() {
        document.addEventListener('touchstart', (e) => {
            if (e.target.closest('.message-bubble')) {
                this.longPressTimer = setTimeout(() => {
                    this.showMessageActions(e.target.closest('.message-bubble'));
                }, 500);
            }
        });

        document.addEventListener('touchend', () => {
            if (this.longPressTimer) {
                clearTimeout(this.longPressTimer);
            }
        });

        document.addEventListener('touchmove', () => {
            if (this.longPressTimer) {
                clearTimeout(this.longPressTimer);
            }
        });
    }

    // メッセージアクション表示
    showMessageActions(messageElement) {
        const actions = document.createElement('div');
        actions.className = 'message-actions';
        actions.style.cssText = `
            position: fixed;
            bottom: 20px;
            left: 20px;
            right: 20px;
            background: rgba(255,255,255,0.95);
            backdrop-filter: blur(10px);
            border-radius: 15px;
            padding: 1rem;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            z-index: 9999;
            display: flex;
            gap: 1rem;
            justify-content: center;
        `;

        actions.innerHTML = `
            <button class="btn btn-outline-primary btn-sm" onclick="this.parentElement.remove()">
                <i class="fas fa-reply"></i> 返信
            </button>
            <button class="btn btn-outline-secondary btn-sm" onclick="this.parentElement.remove()">
                <i class="fas fa-copy"></i> コピー
            </button>
            <button class="btn btn-outline-danger btn-sm" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i> 閉じる
            </button>
        `;

        document.body.appendChild(actions);

        // 3秒後に自動で閉じる
        setTimeout(() => {
            if (actions.parentElement) {
                actions.remove();
            }
        }, 3000);
    }
}

// 初期化
let pwaManager, mobileChatEnhancements;

document.addEventListener('DOMContentLoaded', () => {
    pwaManager = new PWAManager();
    mobileChatEnhancements = new MobileChatEnhancements();
    
    // ローディング画面の非表示
    setTimeout(() => {
        const loading = document.querySelector('.app-loading');
        if (loading) {
            loading.classList.add('hidden');
            setTimeout(() => loading.remove(), 500);
        }
    }, 1000);
});

// デバイス向き変更時の処理
window.addEventListener('orientationchange', () => {
    setTimeout(() => {
        // レイアウトの再調整
        window.dispatchEvent(new Event('resize'));
    }, 100);
});
