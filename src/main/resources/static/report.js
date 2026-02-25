function toggle(id) {
    document.getElementById(id).classList.toggle('open');
    document.getElementById('chevron_' + id).classList.toggle('open');
}

document.addEventListener('DOMContentLoaded', () => {
    if (localStorage.getItem('theme') === 'dark') {
        document.body.classList.add('dark');
    }
    const first = document.querySelector('.group-body');
    const firstChevron = document.querySelector('.chevron');
    if (first) { first.classList.add('open'); firstChevron.classList.add('open'); }
});

document.getElementById('themeToggle').addEventListener('click', () => {
    document.body.classList.toggle('dark');
    localStorage.setItem('theme', document.body.classList.contains('dark') ? 'dark' : 'light');
});

const VAR_CLASSES = {
    'λ':      'math-lam',
    'lambda': 'math-lam',
    'x':      'math-x',
    'l':      'math-l',
    'd':      'math-d',
};

function classify(raw) {
    const s = (raw || '').trim().toLowerCase();
    return VAR_CLASSES[s] || VAR_CLASSES[raw.trim()] || null;
}

function colorizeVars() {
    document.querySelectorAll('mjx-mi').forEach(el => {
        const candidates = [
            el.getAttribute('data-semantic-speech'),
            el.getAttribute('title'),
            el.textContent,
        ];
        for (const c of candidates) {
            const cls = classify(c || '');
            if (cls) { el.classList.add(cls); break; }
        }
    });
}

window.addEventListener('load', () => {
    const run = () => {
        colorizeVars();
        new MutationObserver(colorizeVars)
            .observe(document.body, { childList: true, subtree: true });
    };
    if (window.MathJax?.startup?.promise) {
        MathJax.startup.promise.then(run);
    } else {
        setTimeout(run, 1500);
    }
});