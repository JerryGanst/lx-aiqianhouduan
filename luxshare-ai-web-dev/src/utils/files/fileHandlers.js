import mammoth from 'mammoth'
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import {nextTick} from "vue";

// 定义所有文件类型的处理器
export const FILE_HANDLERS = new Map([
  ['txt', async (file) => {
    const reader = new FileReader();
    return new Promise((resolve) => {
      reader.onload = e => {
        resolve({
          content: e.target.result,
          type: 'text'
        });
      };
      reader.readAsText(file.raw);
    });
  }],

  ['docx', async (file) => {
    const arrayBuffer = await file.raw.arrayBuffer();
    const result = await mammoth.convertToHtml({ arrayBuffer });
    return {
      content: `
        <div class="word-preview">
          <style>
            .word-preview {
              font-family: "Times New Roman", serif;
              line-height: 1.2;
              padding: 20px;
              font-size:14px
            }
            table {
              border-collapse: collapse;
              margin: 10px 0;
            }
            td {
              border: 1px solid #ddd;
              padding: 8px;
            }
          </style>
          ${result.value}
        </div>
      `,
      type: 'html'
    };
  }],

  ['doc', async (file) => {
    // 复用 docx 的处理逻辑
    return FILE_HANDLERS.get('docx')(file);
  }],

  ['pdf', async (file) => {
    return {
      content: URL.createObjectURL(file.raw),
      type: 'pdf'
    };
  }],

  ['pptx', async (file) => {
    return {
      content: await file.raw.arrayBuffer(),
      type: 'pptx'
    };
  }],

  ['ppt', async (file) => {
    // 复用 pptx 的处理逻辑
    return FILE_HANDLERS.get('pptx')(file);
  }],

  ['xlsx', async (file) => {
    return {
      content: await file.raw.arrayBuffer(),
      type: 'excel'
    };
  }],

  ['xls', async (file) => {
    return {
      content: await file.raw.arrayBuffer(),
      type: 'excel'
    };
  }]
]);

const downloads = url => {
  try {
    // 1. 调用后端接口获取预签名URL
    // 2. 创建隐藏的<a>标签触发下载
    const link = document.createElement('a')
    link.href = url
    link.style.display = 'none'
    // 3. 从URL中提取文件名（可选）
    const originalFileName = url.split('/').pop().split('?')[0] // 根据实际情况调整
    // 4. 设置下载属性（需配合CORS配置）
    link.setAttribute('download', originalFileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error('下载失败:', error)
    // 使用ElementUI的提示组件
    ElMessage.error('文件下载失败')
  }
}
export const downloadFile = async file => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  let downloadUrl = "/Files/getDownloadUrl?"
  if (file.isLocal) {
    downloadUrl = "/Files/getDownloadUrlFromTemp?"
  } else if (file.libraryType === 'department') {
    downloadUrl = "/Files/getDepartmentDownloadUrl?"
  }
  request
      .post(downloadUrl + `fileId=${file.fileId}&userId=${userInfo.id}`)
      .then(res => {
        if (res.status) {
          downloads(res.data)
        }
      })
      .catch(err => {
        console.error(err)
      })
}