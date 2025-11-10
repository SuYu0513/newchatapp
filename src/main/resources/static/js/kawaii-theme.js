/* 🌟 Kawaii Theme JavaScript - 可愛い動的効果 ✨ */

// ページローダー
function initPageLoader() {
    // ローダー要素を作成
    const loader = document.createElement('div');
    loader.id = 'pageLoader';
    loader.className = 'page-loader';
    loader.innerHTML = '<div class="loader"></div>';
    document.body.prepend(loader);
    
    window.addEventListener('load', function() {
        setTimeout(function() {
            loader.style.opacity = '0';
            setTimeout(function() {
                loader.style.display = 'none';
            }, 500);
        }, 1000);
    });
}

// 背景の浮遊図形を作成
function createFloatingShapes() {
    const shapesContainer = document.createElement('div');
    shapesContainer.className = 'floating-shapes';
    
    for (let i = 0; i < 6; i++) {
        const shape = document.createElement('div');
        shape.className = 'shape';
        shapesContainer.appendChild(shape);
    }
    
    document.body.appendChild(shapesContainer);
}

// パーティクル効果を作成
function createParticles() {
    const particlesContainer = document.createElement('div');
    particlesContainer.className = 'particles';
    particlesContainer.id = 'particles';
    
    const particleCount = 50;
    const cardMarks = ['❤', '★', '♦', '♠', '♣'];
    
    for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        
        // ランダムにカードマークを選択
        const randomMark = cardMarks[Math.floor(Math.random() * cardMarks.length)];
        particle.innerHTML = randomMark;
        particle.style.fontSize = '1.2rem';
        particle.style.color = getRandomColor();
        
        particle.style.left = Math.random() * 100 + '%';
        particle.style.animationDelay = Math.random() * 8 + 's';
        particle.style.animationDuration = (Math.random() * 3 + 5) + 's';
        particlesContainer.appendChild(particle);
    }
    
    document.body.appendChild(particlesContainer);
}

// ランダムな色を生成する関数
function getRandomColor() {
    const colors = [
        '#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', 
        '#6c5ce7', '#fd79a8', '#fdcb6e', '#e17055',
        '#74b9ff', '#fd79a8', '#fdcb6e', '#55a3ff'
    ];
    return colors[Math.floor(Math.random() * colors.length)];
}

// カードのホバー効果
function addCardEffects() {
    const cards = document.querySelectorAll('.card');
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-5px) scale(1.02)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });
}

// スクロールアニメーション
function addScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                entry.target.classList.add('fade-in');
            }
        });
    }, observerOptions);
    
    // 要素を観察対象に追加
    document.querySelectorAll('.card, .alert, .btn, .table').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'all 0.6s ease';
        observer.observe(el);
    });
}

// ボタンのリップル効果
function addRippleEffect() {
    document.querySelectorAll('.btn').forEach(button => {
        button.addEventListener('click', function(e) {
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;
            
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = x + 'px';
            ripple.style.top = y + 'px';
            ripple.classList.add('ripple-effect');
            
            this.appendChild(ripple);
            
            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
}

// キラキラ効果
function addSparkleEffect(element) {
    element.addEventListener('mouseenter', function() {
        // カードマークの配列
        const cardMarks = ['❤', '★', '♦', '♠', '♣'];
        
        for (let i = 0; i < 3; i++) {
            setTimeout(() => {
                const sparkle = document.createElement('div');
                // ランダムにカードマークを選択
                const randomMark = cardMarks[Math.floor(Math.random() * cardMarks.length)];
                sparkle.innerHTML = randomMark;
                sparkle.style.position = 'absolute';
                sparkle.style.pointerEvents = 'none';
                sparkle.style.animation = 'sparkle 1s ease-out forwards';
                sparkle.style.left = Math.random() * 100 + '%';
                sparkle.style.top = Math.random() * 100 + '%';
                sparkle.style.zIndex = '1000';
                sparkle.style.fontSize = '1.2rem';
                this.style.position = 'relative';
                this.appendChild(sparkle);
                
                setTimeout(() => sparkle.remove(), 1000);
            }, i * 200);
        }
    });
}

// 数字カウントアップアニメーション
function animateNumbers() {
    const numbers = document.querySelectorAll('.animate-number');
    numbers.forEach(numberEl => {
        const target = parseInt(numberEl.textContent);
        let current = 0;
        const increment = target / 30;
        const timer = setInterval(function() {
            current += increment;
            if (current >= target) {
                current = target;
                clearInterval(timer);
            }
            numberEl.textContent = Math.floor(current);
        }, 50);
    });
}

// ナビゲーションのアクティブ状態
function updateActiveNav() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href && currentPath.includes(href) && href !== '/') {
            link.classList.add('active');
        }
    });
}

// フォームのアニメーション効果
function addFormEffects() {
    const inputs = document.querySelectorAll('.form-control, .form-select');
    inputs.forEach(input => {
        input.addEventListener('focus', function() {
            this.style.transform = 'scale(1.02)';
        });
        
        input.addEventListener('blur', function() {
            this.style.transform = 'scale(1)';
        });
    });
}

// テーブル行のホバー効果
function addTableEffects() {
    const tableRows = document.querySelectorAll('tbody tr');
    tableRows.forEach(row => {
        row.addEventListener('mouseenter', function() {
            this.style.background = 'rgba(102, 126, 234, 0.1)';
            this.style.transform = 'scale(1.01)';
        });
        
        row.addEventListener('mouseleave', function() {
            this.style.background = '';
            this.style.transform = 'scale(1)';
        });
    });
}

// チャットメッセージのアニメーション
function animateNewMessage(messageElement) {
    messageElement.style.opacity = '0';
    messageElement.style.transform = 'translateX(-20px)';
    
    setTimeout(() => {
        messageElement.style.transition = 'all 0.3s ease';
        messageElement.style.opacity = '1';
        messageElement.style.transform = 'translateX(0)';
    }, 100);
}

// ツールチップの初期化
function initTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    if (typeof bootstrap !== 'undefined') {
        tooltipTriggerList.map(function(tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
}

// モーダルの効果
function addModalEffects() {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('show.bs.modal', function() {
            this.querySelector('.modal-content').style.transform = 'scale(0.8)';
            this.querySelector('.modal-content').style.opacity = '0';
            
            setTimeout(() => {
                this.querySelector('.modal-content').style.transition = 'all 0.3s ease';
                this.querySelector('.modal-content').style.transform = 'scale(1)';
                this.querySelector('.modal-content').style.opacity = '1';
            }, 100);
        });
    });
}

// 成功・エラーメッセージのアニメーション
function animateAlerts() {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        alert.style.transform = 'translateY(-20px)';
        alert.style.opacity = '0';
        
        setTimeout(() => {
            alert.style.transition = 'all 0.5s ease';
            alert.style.transform = 'translateY(0)';
            alert.style.opacity = '1';
        }, 100);
        
        // 自動で閉じる
        setTimeout(() => {
            if (alert.querySelector('.btn-close')) {
                alert.style.transform = 'translateY(-20px)';
                alert.style.opacity = '0';
                setTimeout(() => alert.remove(), 500);
            }
        }, 5000);
    });
}

// ハートビート効果
function addHeartBeat(element) {
    element.classList.add('heart-beat');
}

// 初期化関数
function initKawaiiTheme() {
    // 基本要素を作成
    createFloatingShapes();
    createParticles();
    initPageLoader();
    
    // 効果を追加
    addCardEffects();
    addScrollAnimations();
    addRippleEffect();
    addFormEffects();
    addTableEffects();
    animateAlerts();
    updateActiveNav();
    initTooltips();
    addModalEffects();
    
    // 特別な要素にキラキラ効果
    document.querySelectorAll('.sparkle-on-hover').forEach(el => {
        addSparkleEffect(el);
    });
    
    // 数字アニメーション
    setTimeout(animateNumbers, 1500);
    
    // ページ固有の初期化
    if (typeof initPageSpecific === 'function') {
        initPageSpecific();
    }
}

// DOMが読み込まれた時に初期化
document.addEventListener('DOMContentLoaded', function() {
    initKawaiiTheme();
});

// 動的に追加された要素に効果を適用
function applyEffectsToNewElements(container) {
    // 新しいカードに効果を追加
    container.querySelectorAll('.card').forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'all 0.6s ease';
        
        setTimeout(() => {
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, 100);
    });
    
    // 新しいボタンにリップル効果
    container.querySelectorAll('.btn').forEach(button => {
        button.addEventListener('click', function(e) {
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;
            
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = x + 'px';
            ripple.style.top = y + 'px';
            ripple.classList.add('ripple-effect');
            
            this.appendChild(ripple);
            
            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
}

// エクスポート（必要に応じて）
window.KawaiiTheme = {
    init: initKawaiiTheme,
    addSparkle: addSparkleEffect,
    animateMessage: animateNewMessage,
    applyEffects: applyEffectsToNewElements,
    addHeartBeat: addHeartBeat
};
