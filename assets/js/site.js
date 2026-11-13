(function () {
  'use strict';
  var IP = 'play.snailmailmc.com';
  var store = {
    get: function (k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
    set: function (k, v) { try { localStorage.setItem(k, v); } catch (e) {} }
  };

  /* Toast */
  var toast = document.getElementById('toast'), toastTimer;
  function say(msg) {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toast.classList.remove('show'); }, 1800);
  }

  /* Copy buttons */
  function copy(text) {
    if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(text);
    return new Promise(function (res, rej) {
      var t = document.createElement('textarea');
      t.value = text; t.setAttribute('readonly', ''); t.style.position = 'fixed'; t.style.opacity = '0';
      document.body.appendChild(t); t.select();
      try { document.execCommand('copy') ? res() : rej(); } catch (e) { rej(e); }
      document.body.removeChild(t);
    });
  }
  document.addEventListener('click', function (e) {
    var b = e.target.closest('[data-copy]');
    if (!b) return;
    var v = b.getAttribute('data-copy');
    copy(v).then(function () {
      b.classList.remove('stamped'); void b.offsetWidth; b.classList.add('stamped');
      say('Copied ' + v);
    }, function () { say(v); });
  });

  /* Mobile menu */
  var menuBtn = document.querySelector('.menu-btn'), nav = document.getElementById('nav');
  if (menuBtn && nav) {
    menuBtn.addEventListener('click', function () {
      var open = menuBtn.getAttribute('aria-expanded') === 'true';
      menuBtn.setAttribute('aria-expanded', String(!open));
      document.body.classList.toggle('menu-open', !open);
    });
  }

  /* Edition tabs (remembered) */
  var groups = document.querySelectorAll('[data-edition-tabs]');
  function setEdition(ed, save) {
    groups.forEach(function (g) {
      g.querySelectorAll('[data-edition]').forEach(function (t) {
        t.setAttribute('aria-selected', String(t.getAttribute('data-edition') === ed));
      });
      g.querySelectorAll('[data-panel]').forEach(function (p) {
        p.hidden = p.getAttribute('data-panel') !== ed;
      });
    });
    if (save) store.set('sm-edition', ed);
  }
  if (groups.length) {
    var saved = store.get('sm-edition');
    if (saved === 'java' || saved === 'bedrock' || saved === 'console') setEdition(saved, false);
    document.addEventListener('click', function (e) {
      var t = e.target.closest('[data-edition]');
      if (t) setEdition(t.getAttribute('data-edition'), true);
    });
  }

  /* Season from the real calendar (the island follows real time) */
  var m = new Date().getMonth();
  var season = m < 2 || m === 11 ? 'winter' : m < 5 ? 'spring' : m < 8 ? 'summer' : 'autumn';
  document.querySelectorAll('[data-season]').forEach(function (el) { el.textContent = season; });

  /* Server status */
  var dots = document.querySelectorAll('[data-status-dot]');
  function setStatus(state, data) {
    dots.forEach(function (d) { d.dataset.state = state; });
    document.querySelectorAll('[data-status-text]').forEach(function (el) {
      el.textContent = state === 'online' ? 'online' : state === 'offline' ? 'offline' : 'checking';
    });
    document.querySelectorAll('[data-players]').forEach(function (el) {
      el.textContent = data && data.players ? data.players.online + ' of ' + data.players.max : '–';
    });
  }
  function checkStatus() {
    if (!dots.length && !document.querySelector('[data-status-text]')) return;
    fetch('https://api.mcstatus.io/v2/status/java/' + IP, { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(function (d) { setStatus(d.online ? 'online' : 'offline', d); })
      .catch(function () { setStatus('unknown'); });
  }
  checkStatus();
  setInterval(checkStatus, 60000);

  /* Discord counts */
  var dEls = document.querySelectorAll('[data-discord-count]');
  if (dEls.length) {
    fetch('https://discord.com/api/v9/invites/snailmail?with_counts=true')
      .then(function (r) { return r.json(); })
      .then(function (d) {
        if (!d.approximate_member_count) return;
        dEls.forEach(function (el) {
          el.innerHTML = '<strong>' + d.approximate_member_count + ' islanders</strong> are there already, and <strong>' +
            d.approximate_presence_count + '</strong> are online now.';
          el.hidden = false;
        });
      }).catch(function () {});
  }

})();
