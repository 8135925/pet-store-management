// 全站脚本：导航高亮、确认对话框、金额格式化（零外部依赖）
(function () {
  'use strict';

  // 侧栏当前菜单高亮
  function highlightNav() {
    var path = location.pathname;
    var links = document.querySelectorAll('.nav a[data-nav]');
    var best = null;
    links.forEach(function (a) {
      var nav = a.getAttribute('data-nav');
      if (path.indexOf(nav) === 0) {
        if (!best || nav.length > best.getAttribute('data-nav').length) best = a;
      }
    });
    if (!best && (path === '/' || path === '')) {
      best = document.querySelector('.nav a[data-nav="/"]');
    }
    if (best) best.classList.add('active');
  }

  // 确认对话框（替代 confirm 依赖，同样语义）
  window.confirmAct = function (msg) {
    return window.confirm(msg);
  };

  // 分 -> 元 显示
  window.fmtYuan = function (cent) {
    return (cent / 100).toFixed(2);
  };
  // 元字符串 -> 分
  window.toCent = function (yuan) {
    var v = Math.round(parseFloat(yuan || '0') * 100);
    return isNaN(v) ? 0 : v;
  };

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  window.escHtml = esc;

  document.addEventListener('DOMContentLoaded', highlightNav);
})();
