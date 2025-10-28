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

async function getSsoToken() {
  try {
    const token = await OfficeRuntime.auth.getAccessToken({ allowSignInPrompt: true, forMSGraphAccess: true });
    return { token, source: 'SSO' };
  } catch (error) {
    console.warn('SSO token acquisition failed', error);
    return { token: '', source: 'Fallback' };
  }
}

async function summarize(mode) {
  set('result', '分析中…');
  const { subject, body } = await getCurrentItemBody();
  const started = performance.now();
  const { token, source } = await getSsoToken();
  try {
    const res = await fetch('/summarize/text', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ subject, text: body, mode, accessToken: token || undefined })
    });
    const data = await res.json();
    const cost = Math.round(performance.now() - started);
    const model = esc(data.model || 'mail-assistant');
    set('meta', `subject=${esc(subject.slice(0,80))} | latency=${cost}ms (model=${model}) | auth=${source}`);
    set('result', esc(data.result));
  } catch (e) {
    set('result', '调用失败：' + esc(e.message));
  }
}
