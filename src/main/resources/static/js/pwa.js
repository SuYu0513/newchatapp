// PWA機能の管理（軽量版）
class PWAManager {
    constructor() {
        this.deferredPrompt = null;
        this.init();
    }

    init() {
        this.registerServiceWorker();
        this.setupInstallPrompt();
    }

    // Service Worker登録
    async registerServiceWorker() {
        if ('serviceWorker' in navigator) {
            try {
                await navigator.serviceWorker.register('/sw.js');
            } catch (error) {
                console.log('SW registration failed:', error);
            }
        }
    }

    // インストールプロンプトの設定
    setupInstallPrompt() {
        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            this.deferredPrompt = e;
        });
    }

    // アプリインストール実行
    async installApp() {
        if (!this.deferredPrompt) return;
        await this.deferredPrompt.prompt();
        this.deferredPrompt = null;
    }
}

// 初期化
const pwaManager = new PWAManager();