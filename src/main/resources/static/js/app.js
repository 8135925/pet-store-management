// 全站脚本：导航高亮、确认对话框、金额格式化（零外部依赖）
(function () {
  'use strict';

  // 侧栏当前菜单高亮
  // 按路径段匹配：/bills 只匹配 /bills 与 /bills/...，不会误中 /billsnew 之类
  function matchNav(path, nav) {
    if (nav === '/') {
      return path === '/' || path === '';
    }
    return path === nav || path.indexOf(nav + '/') === 0;
  }

  function highlightNav() {
    var path = location.pathname;
    var links = document.querySelectorAll('.nav a[data-nav]');
    var best = null;
    links.forEach(function (a) {
      var nav = a.getAttribute('data-nav');
      if (matchNav(path, nav)) {
        if (!best || nav.length > best.getAttribute('data-nav').length) best = a;
      }
    });
    if (best) {
      best.classList.add('active');
      // 侧栏是独立滚动容器：跳转/刷新后把当前菜单滚回可视区，
      // 避免点了靠下的菜单（如用户管理）每次都要重新往下拉
      best.scrollIntoView({ block: 'nearest' });
    }
  }

  // 「返回」按钮：优先回到来源页（保留列表的筛选条件），
  // 直接打开/刷新丢失历史时，退回 data-back 指定的列表页
  function bindBackButtons() {
    document.querySelectorAll('[data-back]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        if (history.length > 1 && document.referrer
                && document.referrer.indexOf(location.origin) === 0) {
          history.back();
        } else {
          location.href = btn.getAttribute('data-back');
        }
      });
    });
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

  document.addEventListener('DOMContentLoaded', function () {
    highlightNav();
    bindBackButtons();
  });
})();
