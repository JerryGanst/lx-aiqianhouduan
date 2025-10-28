import{e as y,r as w,j as d,c as i,o as n,a as e,t,b as l,U as $,F as C,q as D,n as k,d as S,m as c,l as u}from"#entry";import{h as p}from"./exB6uOw2.js";const j={class:"space-y-4"},T={class:"mb-2 text-lg font-medium"},z={class:"text-muted-foreground text-sm"},F={key:0,class:"border-border rounded-lg border p-6 text-center"},U={class:"mb-2 font-medium"},B={class:"text-muted-foreground text-sm"},I={key:1,class:"space-y-4"},N={class:"flex border-b"},P=["onClick"],V={class:"space-y-4"},A={key:0,class:"space-y-3"},E={class:"flex items-center justify-between"},J={class:"font-medium"},K={class:"relative"},q={class:"bg-muted overflow-auto rounded-lg p-4 text-sm"},L={class:"text-muted-foreground text-xs"},W={key:1,class:"space-y-3"},H={class:"flex items-center justify-between"},M={class:"relative"},R={class:"bg-muted overflow-auto rounded-lg p-4 text-sm"},G={class:"text-muted-foreground text-xs"},O={key:2,class:"space-y-3"},Q={class:"flex items-center justify-between"},X={class:"font-medium"},Y={class:"relative"},Z={class:"bg-muted overflow-auto rounded-lg p-4 text-sm"},ee={class:"text-muted-foreground text-xs"},se={class:"space-y-3"},te={class:"font-medium"},oe={class:"border-border flex justify-center rounded-lg border p-6"},ie={class:"border-border overflow-hidden rounded-lg border shadow-lg",style:{width:"100%",height:"750px"}},ne=["src"],de=y({__name:"embed-code",props:{agent:{}},setup(_){const m=_,a=w("iframe"),o=d(()=>m.agent?.publishToken?`${window.location.origin}/public/agent/shared/${m.agent.publishToken}`:""),h=d(()=>o.value?`<!-- FastbuildAI 智能体嵌入代码 -->
<iframe
  src="${o.value}?embed=true"
  width="400"
  height="600"
  frameborder="0"
  style="border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
</iframe>`:""),b=d(()=>o.value?`<!-- 使用 JavaScript SDK -->
<div id="chatbot-container"></div>
<script>
  window.FastbuildAI = {
    init: function(options) {
      const iframe = document.createElement('iframe');
      iframe.src = '${o.value}?embed=true&sdk=true';
      iframe.width = options.width || '400px';
      iframe.height = options.height || '600px';
      iframe.style.border = 'none';
      iframe.style.borderRadius = '10px';
      iframe.style.boxShadow = '0 4px 20px rgba(0,0,0,0.1)';

      const container = document.querySelector(options.container);
      if (container) {
        container.appendChild(iframe);
      }
    }
  };

  // 初始化智能体
  FastbuildAI.init({
    container: '#chatbot-container',
    width: '400px',
    height: '600px'
  });
<\\/script>`:""),f=d(()=>o.value?`<!-- WordPress 短代码 -->
[fastbuildai_chatbot url="${o.value}" width="400" height="600"]

<!-- 或者直接使用 HTML -->
<div style="width: 400px; height: 600px;">
  <iframe
    src="${o.value}?embed=true"
    width="100%"
    height="100%"
    frameborder="0"
    style="border-radius: 10px;">
  </iframe>
</div>`:""),x=[{value:"iframe",label:"iframe 嵌入",icon:"i-lucide-code"},{value:"javascript",label:"JavaScript SDK",icon:"i-lucide-braces"},{value:"wordpress",label:"WordPress",icon:"i-lucide-wordpress"}];return(s,v)=>{const g=$;return n(),i("div",j,[e("div",null,[e("h3",T,t(s.$t("console-ai-agent.publish.embedCode")),1),e("p",z,t(s.$t("console-ai-agent.publish.embedCodeDesc")),1)]),s.agent?.isPublished?(n(),i("div",I,[e("div",N,[(n(),i(C,null,D(x,r=>e("button",{key:r.value,class:k(["flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors",a.value===r.value?"border-primary text-primary border-b-2":"text-muted-foreground hover:text-foreground"]),onClick:ae=>a.value=r.value},[l(g,{name:r.icon,class:"size-4"},null,8,["name"]),S(" "+t(r.label),1)],10,P)),64))]),e("div",V,[a.value==="iframe"?(n(),i("div",A,[e("div",E,[e("h4",J,t(s.$t("console-ai-agent.publish.iframeCode")),1),l(u(p),{content:h.value,variant:"outline",size:"sm",copiedText:s.$t("console-common.messages.copySuccess"),"default-text":s.$t("console-common.copy")},null,8,["content","copiedText","default-text"])]),e("div",K,[e("pre",q,[e("code",null,t(h.value),1)])]),e("div",L,[e("p",null,"• "+t(s.$t("console-ai-agent.publish.iframeCodeDesc1")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.iframeCodeDesc2")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.iframeCodeDesc3")),1)])])):c("",!0),a.value==="javascript"?(n(),i("div",W,[e("div",H,[v[0]||(v[0]=e("h4",{class:"font-medium"},"JavaScript SDK",-1)),l(u(p),{content:b.value,variant:"outline",size:"sm",copiedText:s.$t("console-common.messages.copySuccess"),"default-text":s.$t("console-common.copy")},null,8,["content","copiedText","default-text"])]),e("div",M,[e("pre",R,[e("code",null,t(b.value),1)])]),e("div",G,[e("p",null,"• "+t(s.$t("console-ai-agent.publish.javascriptCodeDesc1")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.javascriptCodeDesc2")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.javascriptCodeDesc3")),1)])])):c("",!0),a.value==="wordpress"?(n(),i("div",O,[e("div",Q,[e("h4",X,t(s.$t("console-ai-agent.publish.wordpressCode")),1),l(u(p),{content:f.value,variant:"outline",size:"sm",copiedText:s.$t("console-common.messages.copySuccess"),"default-text":s.$t("console-common.copy")},null,8,["content","copiedText","default-text"])]),e("div",Y,[e("pre",Z,[e("code",null,t(f.value),1)])]),e("div",ee,[e("p",null,"• "+t(s.$t("console-ai-agent.publish.wordpressCodeDesc1")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.wordpressCodeDesc2")),1),e("p",null,"• "+t(s.$t("console-ai-agent.publish.wordpressCodeDesc3")),1)])])):c("",!0)]),e("div",se,[e("h4",te,t(s.$t("console-ai-agent.publish.previewEffect")),1),e("div",oe,[e("div",ie,[e("iframe",{src:`${o.value}?embed=true&preview=true`,width:"100%",height:"100%",frameborder:"0"},null,8,ne)])])])])):(n(),i("div",F,[l(g,{name:"i-lucide-lock",class:"text-muted-foreground mx-auto mb-3 size-12"}),e("h4",U,t(s.$t("console-ai-agent.publish.unpublished")),1),e("p",B,t(s.$t("console-ai-agent.publish.unpublishedDesc2")),1)]))])}}});export{de as _};
