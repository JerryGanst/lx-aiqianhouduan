// taskpane.js
function set(el, html) { document.getElementById(el).innerHTML = html; }
function esc(s){ return (s||'').toString().replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m])); }

Office.onReady(async () => {
  const btnSummary = document.getElementById('btn-summary');
  const btnActions = document.getElementById('btn-actions');

  btnSummary.onclick = () => summarize('thread-summary');
  btnActions.onclick = () => summarize('actions');

  set('meta', '准备就绪：点击按钮开始分析当前打开的邮件。');
});

async function getCurrentItemBody() {
  return new Promise((resolve) => {
    const item = Office.context.mailbox.item;
    if (!item) return resolve({ subject: '', body: '' });
    const subject = item.subject || '';
    item.body.getAsync(Office.CoercionType.Text, (asyncResult) => {
      if (asyncResult.status === Office.AsyncResultStatus.Succeeded) {
        resolve({ subject, body: asyncResult.value || '' });
      } else {
        resolve({ subject, body: '' });
      }
    });
  });
}

async function summarize(mode) {
  set('result', '分析中…');
  const { subject, body } = await getCurrentItemBody();
  const started = performance.now();
  try {
    const res = await fetch('/summarize/text', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ subject, text: body, mode })
    });
    const data = await res.json();
    const cost = Math.round(performance.now() - started);
    set('meta', `subject=${esc(subject.slice(0,80))} | latency=${cost}ms (model=${esc(data.model)})`);
    set('result', esc(data.result));
  } catch (e) {
    set('result', '调用失败：' + esc(e.message));
  }
}
